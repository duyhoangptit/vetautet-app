package com.vetautet.app.application.payment.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.application.payment.dto.ConfirmPaymentCommand;
import com.vetautet.app.application.payment.dto.ConfirmPaymentResultDto;
import com.vetautet.app.application.payment.dto.PaymentTransactionDto;
import com.vetautet.app.application.payment.port.input.ConfirmPaymentUseCase;
import com.vetautet.app.domain.booking.model.BookingOrder;
import com.vetautet.app.domain.booking.model.BookingOrderItem;
import com.vetautet.app.domain.booking.model.BookingOrderItemStatus;
import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import com.vetautet.app.domain.booking.repository.BookingOrderRepository;
import com.vetautet.app.domain.inventory.model.DepartureInventoryBucket;
import com.vetautet.app.domain.inventory.model.InventoryReservation;
import com.vetautet.app.domain.inventory.model.InventoryReservationStatus;
import com.vetautet.app.domain.inventory.repository.DepartureInventoryBucketRepository;
import com.vetautet.app.domain.inventory.repository.InventoryReservationRepository;
import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import com.vetautet.app.domain.messaging.repository.OutboxEventRepository;
import com.vetautet.app.domain.payment.model.PaymentTransaction;
import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;
import com.vetautet.app.domain.payment.repository.PaymentTransactionRepository;
import com.vetautet.app.shared.common.context.RequestIdContext;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import com.vetautet.app.shared.common.util.JsonUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfirmPaymentUseCaseImpl implements ConfirmPaymentUseCase {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final DepartureInventoryBucketRepository departureInventoryBucketRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ConfirmPaymentResultDto execute(ConfirmPaymentCommand command) {
        Instant confirmedAt = command.getConfirmedAt() != null ? command.getConfirmedAt() : Instant.now();

        try {
            PaymentTransaction paymentTransaction = paymentTransactionRepository.findById(command.getPaymentTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, command.getPaymentTransactionId()));
            if (paymentTransaction.isTerminal()) {
                throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "payment transaction is already in terminal state");
            }

            BookingOrder bookingOrder = bookingOrderRepository.findById(paymentTransaction.getBookingOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, paymentTransaction.getBookingOrderId()));
            if (bookingOrder.isTerminal()) {
                throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "booking order is already in terminal state");
            }

            List<InventoryReservation> reservations = inventoryReservationRepository
                    .findByBookingOrderIdAndStatus(bookingOrder.getBookingOrderId(), InventoryReservationStatus.HLD);
            if (reservations.isEmpty()) {
                throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "no active inventory reservations found for booking");
            }

            for (InventoryReservation reservation : reservations) {
                DepartureInventoryBucket bucket = departureInventoryBucketRepository.findById(reservation.getInventoryBucketId())
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, reservation.getInventoryBucketId()));

                departureInventoryBucketRepository.save(bucket.toBuilder()
                        .reservedQuantity(bucket.getReservedQuantity() - reservation.getReservedQuantity())
                        .soldQuantity(bucket.getSoldQuantity() + reservation.getReservedQuantity())
                        .build());

                inventoryReservationRepository.save(reservation.toBuilder()
                        .reservationStatus(InventoryReservationStatus.CNF)
                        .confirmedAt(confirmedAt)
                        .build());
            }

            BookingOrder confirmedBooking = bookingOrderRepository.save(bookingOrder.toBuilder()
                    .status(BookingOrderStatus.CNF)
                    .confirmedAt(confirmedAt)
                    .items(confirmItems(bookingOrder.getItems()))
                    .build());

            PaymentTransaction settledPayment = paymentTransactionRepository.save(paymentTransaction.toBuilder()
                    .status(PaymentTransactionStatus.SET)
                    .providerTransactionId(command.getProviderTransactionId())
                    .providerPaymentUrl(command.getProviderPaymentUrl())
                    .settledAt(confirmedAt)
                    .build());

            outboxEventRepository.save(buildOutboxEvent(
                    "PAYMENT_TRANSACTION",
                    settledPayment.getPaymentTransactionId(),
                    "payment-confirmed",
                    confirmedBooking.getOrderCode(),
                    confirmedAt,
                    Map.of(
                            "bookingOrderId", confirmedBooking.getBookingOrderId(),
                            "paymentTransactionId", settledPayment.getPaymentTransactionId(),
                            "providerTransactionId", command.getProviderTransactionId(),
                            "settledAt", confirmedAt,
                            "amount", settledPayment.getAmount(),
                            "currencyCode", settledPayment.getCurrencyCode())));

            return ConfirmPaymentResultDto.builder()
                    .bookingOrderId(confirmedBooking.getBookingOrderId())
                    .orderCode(confirmedBooking.getOrderCode())
                    .bookingStatus(confirmedBooking.getStatus())
                    .bookingConfirmedAt(confirmedBooking.getConfirmedAt())
                    .paymentTransaction(toPaymentTransactionDto(settledPayment))
                    .build();
        } catch (OptimisticLockingFailureException ex) {
            throw new AppLogicException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "inventory changed while confirming payment, please retry");
        }
    }

    private List<BookingOrderItem> confirmItems(List<BookingOrderItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> item.toBuilder().itemStatus(BookingOrderItemStatus.CNF).build())
                .toList();
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

}