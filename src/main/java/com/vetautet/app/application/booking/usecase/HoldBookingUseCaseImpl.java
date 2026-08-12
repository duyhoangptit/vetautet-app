package com.vetautet.app.application.booking.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.application.booking.dto.HoldBookingCommand;
import com.vetautet.app.application.booking.dto.HoldBookingItemCommand;
import com.vetautet.app.application.booking.dto.HoldBookingItemResultDto;
import com.vetautet.app.application.booking.dto.HoldBookingResultDto;
import com.vetautet.app.application.booking.port.input.HoldBookingUseCase;
import com.vetautet.app.application.notification.dto.SendNotificationCommand;
import com.vetautet.app.application.notification.port.input.SendNotificationUseCase;
import com.vetautet.app.application.payment.dto.PaymentTransactionDto;
import com.vetautet.app.domain.booking.model.BookingOrder;
import com.vetautet.app.domain.booking.model.BookingOrderItem;
import com.vetautet.app.domain.booking.model.BookingOrderItemStatus;
import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import com.vetautet.app.domain.booking.repository.BookingOrderRepository;
import com.vetautet.app.domain.inventory.model.DepartureInventoryBucket;
import com.vetautet.app.domain.inventory.model.InventoryReservation;
import com.vetautet.app.domain.inventory.model.InventoryReservationStatus;
import com.vetautet.app.domain.inventory.model.InventorySaleStatus;
import com.vetautet.app.domain.inventory.repository.DepartureInventoryBucketRepository;
import com.vetautet.app.domain.inventory.repository.InventoryReservationRepository;
import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import com.vetautet.app.domain.messaging.repository.OutboxEventRepository;
import com.vetautet.app.domain.notification.model.NotificationChannelCode;
import com.vetautet.app.domain.payment.model.PaymentTransaction;
import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;
import com.vetautet.app.domain.payment.repository.PaymentTransactionRepository;
import com.vetautet.app.domain.ticketing.model.TrainDeparture;
import com.vetautet.app.domain.ticketing.model.TrainDepartureStatus;
import com.vetautet.app.domain.ticketing.repository.TrainDepartureRepository;
import com.vetautet.app.shared.common.context.RequestIdContext;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import com.vetautet.app.shared.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HoldBookingUseCaseImpl implements HoldBookingUseCase {

    private static final int DEFAULT_HOLD_MINUTES = 15;

    private final TrainDepartureRepository trainDepartureRepository;
    private final DepartureInventoryBucketRepository departureInventoryBucketRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final SendNotificationUseCase sendNotificationUseCase;

    @Override
    @Transactional
    public HoldBookingResultDto execute(HoldBookingCommand command) {
        Instant now = Instant.now();
        TrainDeparture departure = trainDepartureRepository.findById(command.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, command.getDepartureId()));

        validateDepartureAvailable(departure, now);
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one booking item is required");
        }

        try {
            return createHold(command, departure, now);
        } catch (OptimisticLockingFailureException ex) {
            throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "inventory changed while processing hold, please retry");
        }
    }

    private HoldBookingResultDto createHold(HoldBookingCommand command, TrainDeparture departure, Instant now) {
        Instant holdExpiresAt = now.plusSeconds(resolveHoldMinutes(command) * 60L);
        List<BookingOrderItem> bookingItems = new ArrayList<>();
        List<HoldBookingItemResultDto> itemResults = new ArrayList<>();
        Map<UUID, DepartureInventoryBucket> touchedBuckets = new LinkedHashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        String currencyCode = null;

        int lineNo = 1;
        for (HoldBookingItemCommand itemCommand : command.getItems()) {
            DepartureInventoryBucket bucket = departureInventoryBucketRepository.findById(itemCommand.getInventoryBucketId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, itemCommand.getInventoryBucketId()));

            validateBucket(departure, bucket, itemCommand);

            int updatedAvailable = bucket.getAvailableQuantity() - itemCommand.getQuantity();
            int updatedReserved = bucket.getReservedQuantity() + itemCommand.getQuantity();
            DepartureInventoryBucket updatedBucket = bucket.toBuilder()
                    .availableQuantity(updatedAvailable)
                    .reservedQuantity(updatedReserved)
                    .build();
            departureInventoryBucketRepository.save(updatedBucket);
            touchedBuckets.put(bucket.getInventoryBucketId(), updatedBucket);

            BigDecimal lineTotal = bucket.getFareAmount().multiply(BigDecimal.valueOf(itemCommand.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            currencyCode = currencyCode == null ? bucket.getCurrencyCode() : currencyCode;

            BookingOrderItem bookingItem = BookingOrderItem.builder()
                    .bookingOrderItemId(UUID.randomUUID())
                    .lineNo(lineNo)
                    .inventoryBucketId(bucket.getInventoryBucketId())
                    .travelFromStopSequence(itemCommand.getTravelFromStopSequence())
                    .travelToStopSequence(itemCommand.getTravelToStopSequence())
                    .seatClassCode(bucket.getSeatClassCode())
                    .quotaCode(bucket.getQuotaCode())
                    .quantity(itemCommand.getQuantity())
                    .unitPriceAmount(bucket.getFareAmount())
                    .lineTotalAmount(lineTotal)
                    .itemStatus(BookingOrderItemStatus.HLD)
                    .build();
            bookingItems.add(bookingItem);

            itemResults.add(HoldBookingItemResultDto.builder()
                    .bookingOrderItemId(bookingItem.getBookingOrderItemId())
                    .inventoryBucketId(bucket.getInventoryBucketId())
                    .lineNo(lineNo)
                    .seatClassCode(bucket.getSeatClassCode())
                    .quotaCode(bucket.getQuotaCode())
                    .quantity(itemCommand.getQuantity())
                    .unitPriceAmount(bucket.getFareAmount())
                    .lineTotalAmount(lineTotal)
                    .build());
            lineNo++;
        }

        BookingOrder bookingOrder = bookingOrderRepository.save(BookingOrder.builder()
                .bookingOrderId(UUID.randomUUID())
                .orderCode(generateOrderCode())
                .userId(command.getUserId())
                .departureId(command.getDepartureId())
                .bookingChannel(defaultIfBlank(command.getBookingChannel(), "API"))
                .customerFullName(command.getCustomerFullName())
                .customerEmail(command.getCustomerEmail())
                .customerPhone(command.getCustomerPhone())
                .status(BookingOrderStatus.HLD)
                .totalAmount(totalAmount)
                .currencyCode(currencyCode)
                .holdExpiresAt(holdExpiresAt)
                .idempotencyKey(command.getIdempotencyKey())
                .items(bookingItems)
                .build());

        for (BookingOrderItem bookingItem : bookingItems) {
            inventoryReservationRepository.save(InventoryReservation.builder()
                    .inventoryReservationId(UUID.randomUUID())
                    .bookingOrderId(bookingOrder.getBookingOrderId())
                    .inventoryBucketId(bookingItem.getInventoryBucketId())
                    .reservedQuantity(bookingItem.getQuantity())
                    .holdExpiresAt(holdExpiresAt)
                    .reservationStatus(InventoryReservationStatus.HLD)
                    .build());
        }

        PaymentTransaction paymentTransaction = paymentTransactionRepository.save(PaymentTransaction.builder()
                .paymentTransactionId(UUID.randomUUID())
                .bookingOrderId(bookingOrder.getBookingOrderId())
                .paymentMethodCode(command.getPaymentMethodCode())
                .providerCode(defaultIfBlank(command.getProviderCode(), command.getPaymentMethodCode().name()))
                .amount(totalAmount)
                .currencyCode(currencyCode)
                .status(PaymentTransactionStatus.PEN)
                .requestedAt(now)
                .expiresAt(holdExpiresAt)
                .build());

        outboxEventRepository.save(buildOutboxEvent(
                "BOOKING_ORDER",
                bookingOrder.getBookingOrderId(),
                "booking-held",
                bookingOrder.getOrderCode(),
                now,
                Map.of(
                        "bookingOrderId", bookingOrder.getBookingOrderId(),
                        "orderCode", bookingOrder.getOrderCode(),
                        "paymentTransactionId", paymentTransaction.getPaymentTransactionId(),
                        "holdExpiresAt", holdExpiresAt,
                        "totalAmount", totalAmount,
                        "currencyCode", currencyCode,
                        "itemCount", bookingItems.size())));

        sendBookingHoldNotification(command, departure, bookingOrder, totalAmount, currencyCode,
                holdExpiresAt, bookingItems.size());

        return HoldBookingResultDto.builder()
                .bookingOrderId(bookingOrder.getBookingOrderId())
                .orderCode(bookingOrder.getOrderCode())
                .status(bookingOrder.getStatus())
                .totalAmount(bookingOrder.getTotalAmount())
                .currencyCode(bookingOrder.getCurrencyCode())
                .holdExpiresAt(bookingOrder.getHoldExpiresAt())
                .items(itemResults)
                .paymentTransaction(toPaymentTransactionDto(paymentTransaction))
                .build();
    }

    private static final DateTimeFormatter VN_DATETIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final DateTimeFormatter VN_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private void sendBookingHoldNotification(HoldBookingCommand command,
                                              TrainDeparture departure,
                                              BookingOrder bookingOrder,
                                              BigDecimal totalAmount,
                                              String currencyCode,
                                              Instant holdExpiresAt,
                                              int itemCount) {
        String fullName = defaultIfBlank(command.getCustomerFullName(), command.getCustomerEmail());
        Map<String, String> vars = Map.of(
                "full_name", fullName,
                "order_code", bookingOrder.getOrderCode(),
                "departure_code", departure.getDepartureCode(),
                "departure_date", VN_DATE_FMT.format(departure.getBusinessDate()),
                "planned_departure_at", VN_DATETIME_FMT.format(departure.getPlannedDepartureAt()),
                "item_count", String.valueOf(itemCount),
                "total_amount", totalAmount.toPlainString(),
                "currency_code", currencyCode,
                "hold_expires_at", VN_DATETIME_FMT.format(holdExpiresAt),
                "payment_method", command.getPaymentMethodCode().name()
        );

        sendNotificationUseCase.execute(SendNotificationCommand.builder()
                .channelCode(NotificationChannelCode.EMAIL)
                .templateCode("BOOKING_HOLD")
                .recipient(command.getCustomerEmail())
                .referenceId(bookingOrder.getBookingOrderId())
                .referenceType("BOOKING_ORDER")
                .variables(vars)
                .build());

        if (!defaultIfBlank(command.getCustomerPhone(), "").isBlank()) {
            Map<String, String> smsVars = Map.of(
                    "order_code", bookingOrder.getOrderCode(),
                    "total_amount", totalAmount.toPlainString(),
                    "currency_code", currencyCode,
                    "hold_expires_at", VN_DATETIME_FMT.format(holdExpiresAt)
            );
            sendNotificationUseCase.execute(SendNotificationCommand.builder()
                    .channelCode(NotificationChannelCode.SMS)
                    .templateCode("BOOKING_HOLD")
                    .recipient(command.getCustomerPhone())
                    .referenceId(bookingOrder.getBookingOrderId())
                    .referenceType("BOOKING_ORDER")
                    .variables(smsVars)
                    .build());
        }
    }

    private void validateDepartureAvailable(TrainDeparture departure, Instant now) {
        if (departure.getStatus() != TrainDepartureStatus.OPN
                || departure.getSaleOpensAt().isAfter(now)
                || departure.getSaleClosesAt().isBefore(now)) {
            throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "departure is not open for booking");
        }
    }

    private void validateBucket(TrainDeparture departure, DepartureInventoryBucket bucket, HoldBookingItemCommand itemCommand) {
        if (!bucket.getDepartureId().equals(departure.getDepartureId())) {
            throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "inventory bucket does not belong to departure");
        }
        if (bucket.getSaleStatus() != InventorySaleStatus.OPN) {
            throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "inventory bucket is not open for sale");
        }
        if (itemCommand.getQuantity() == null || itemCommand.getQuantity() <= 0) {
            throw new IllegalArgumentException("Booking item quantity must be greater than zero");
        }
        if (bucket.getAvailableQuantity() == null || bucket.getAvailableQuantity() < itemCommand.getQuantity()) {
            throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "insufficient inventory for bucket " + bucket.getInventoryBucketId());
        }
    }

    private int resolveHoldMinutes(HoldBookingCommand command) {
        if (command.getHoldDurationMinutes() == null || command.getHoldDurationMinutes() <= 0) {
            return DEFAULT_HOLD_MINUTES;
        }
        return command.getHoldDurationMinutes();
    }

    private String generateOrderCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String orderCode = "BKG" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            if (!bookingOrderRepository.existsByOrderCode(orderCode)) {
                return orderCode;
            }
        }

        throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED, "unable to allocate booking order code");
    }

    private OutboxEvent buildOutboxEvent(String aggregateType,
                                         UUID aggregateId,
                                         String eventType,
                                         String partitionKey,
                                         Instant now,
                                         Map<String, Object> payload) {
        return OutboxEvent.builder()
                .outboxEventId(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .partitionKey(partitionKey)
                .payload(JsonUtil.writeJson(objectMapper, payload, "Unable to serialize outbox payload"))
                .headers(JsonUtil.writeJson(objectMapper, RequestIdContext.createMessageHeaders(eventType),
                    "Unable to serialize outbox payload"))
                .publishStatus(OutboxPublishStatus.NEW)
                .retryCount(0)
                .nextAttemptAt(now)
                .build();
    }

    private PaymentTransactionDto toPaymentTransactionDto(PaymentTransaction paymentTransaction) {
        return PaymentTransactionDto.builder()
                .paymentTransactionId(paymentTransaction.getPaymentTransactionId())
                .paymentMethodCode(paymentTransaction.getPaymentMethodCode())
                .providerCode(paymentTransaction.getProviderCode())
                .providerTransactionId(paymentTransaction.getProviderTransactionId())
                .providerPaymentUrl(paymentTransaction.getProviderPaymentUrl())
                .amount(paymentTransaction.getAmount())
                .currencyCode(paymentTransaction.getCurrencyCode())
                .status(paymentTransaction.getStatus())
                .requestedAt(paymentTransaction.getRequestedAt())
                .settledAt(paymentTransaction.getSettledAt())
                .expiresAt(paymentTransaction.getExpiresAt())
                .build();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}