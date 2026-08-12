package com.vetautet.app.presentation.rest.mapper;

import com.vetautet.app.application.booking.dto.HoldBookingCommand;
import com.vetautet.app.application.booking.dto.HoldBookingItemCommand;
import com.vetautet.app.application.booking.dto.HoldBookingResultDto;
import com.vetautet.app.application.payment.dto.ConfirmPaymentCommand;
import com.vetautet.app.application.payment.dto.ConfirmPaymentResultDto;
import com.vetautet.app.application.payment.dto.PaymentTransactionDto;
import com.vetautet.app.application.ticketing.dto.DepartureAvailabilityDto;
import com.vetautet.app.presentation.rest.dto.request.ConfirmPaymentRequest;
import com.vetautet.app.presentation.rest.dto.request.HoldBookingRequest;
import com.vetautet.app.presentation.rest.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BookingFlowPresentationMapper {

    public HoldBookingCommand toCommand(UUID userId, HoldBookingRequest request) {
        if (request == null) {
            return null;
        }

        return HoldBookingCommand.builder()
                .userId(userId)
                .departureId(request.getDepartureId())
                .bookingChannel(request.getBookingChannel())
                .customerFullName(request.getCustomerFullName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .paymentMethodCode(request.getPaymentMethodCode())
                .providerCode(request.getProviderCode())
                .holdDurationMinutes(request.getHoldDurationMinutes())
                .idempotencyKey(request.getIdempotencyKey())
                .items(request.getItems() == null ? List.of() : request.getItems().stream()
                        .map(item -> HoldBookingItemCommand.builder()
                                .inventoryBucketId(item.getInventoryBucketId())
                                .quantity(item.getQuantity())
                                .travelFromStopSequence(item.getTravelFromStopSequence())
                                .travelToStopSequence(item.getTravelToStopSequence())
                                .build())
                        .toList())
                .build();
    }

    public ConfirmPaymentCommand toCommand(ConfirmPaymentRequest request) {
        if (request == null) {
            return null;
        }

        return ConfirmPaymentCommand.builder()
                .paymentTransactionId(request.getPaymentTransactionId())
                .providerTransactionId(request.getProviderTransactionId())
                .providerPaymentUrl(request.getProviderPaymentUrl())
                .build();
    }

    public List<DepartureAvailabilityResponse> toDepartureResponses(List<DepartureAvailabilityDto> dtos) {
        if (dtos == null) {
            return List.of();
        }

        return dtos.stream().map(this::toDepartureResponse).toList();
    }

    public DepartureAvailabilityResponse toDepartureResponse(DepartureAvailabilityDto dto) {
        if (dto == null) {
            return null;
        }

        return DepartureAvailabilityResponse.builder()
                .departureId(dto.getDepartureId())
                .departureCode(dto.getDepartureCode())
                .businessDate(dto.getBusinessDate())
                .originStationId(dto.getOriginStationId())
                .destinationStationId(dto.getDestinationStationId())
                .plannedDepartureAt(dto.getPlannedDepartureAt())
                .plannedArrivalAt(dto.getPlannedArrivalAt())
                .buckets(dto.getBuckets() == null ? List.of() : dto.getBuckets().stream()
                        .map(bucket -> DepartureBucketAvailabilityResponse.builder()
                                .inventoryBucketId(bucket.getInventoryBucketId())
                                .seatClassCode(bucket.getSeatClassCode())
                                .quotaCode(bucket.getQuotaCode())
                                .bucketNo(bucket.getBucketNo())
                                .availableQuantity(bucket.getAvailableQuantity())
                                .fareAmount(bucket.getFareAmount())
                                .currencyCode(bucket.getCurrencyCode())
                                .build())
                        .toList())
                .build();
    }

    public HoldBookingResponse toResponse(HoldBookingResultDto dto) {
        if (dto == null) {
            return null;
        }

        return HoldBookingResponse.builder()
                .bookingOrderId(dto.getBookingOrderId())
                .orderCode(dto.getOrderCode())
                .status(dto.getStatus().name())
                .totalAmount(dto.getTotalAmount())
                .currencyCode(dto.getCurrencyCode())
                .holdExpiresAt(dto.getHoldExpiresAt())
                .items(dto.getItems() == null ? List.of() : dto.getItems().stream()
                        .map(item -> HoldBookingItemResponse.builder()
                                .bookingOrderItemId(item.getBookingOrderItemId())
                                .inventoryBucketId(item.getInventoryBucketId())
                                .lineNo(item.getLineNo())
                                .seatClassCode(item.getSeatClassCode())
                                .quotaCode(item.getQuotaCode())
                                .quantity(item.getQuantity())
                                .unitPriceAmount(item.getUnitPriceAmount())
                                .lineTotalAmount(item.getLineTotalAmount())
                                .build())
                        .toList())
                .paymentTransaction(toResponse(dto.getPaymentTransaction()))
                .build();
    }

    public ConfirmPaymentResponse toResponse(ConfirmPaymentResultDto dto) {
        if (dto == null) {
            return null;
        }

        return ConfirmPaymentResponse.builder()
                .bookingOrderId(dto.getBookingOrderId())
                .orderCode(dto.getOrderCode())
                .bookingStatus(dto.getBookingStatus().name())
                .bookingConfirmedAt(dto.getBookingConfirmedAt())
                .paymentTransaction(toResponse(dto.getPaymentTransaction()))
                .build();
    }

    public PaymentTransactionResponse toResponse(PaymentTransactionDto dto) {
        if (dto == null) {
            return null;
        }

        return PaymentTransactionResponse.builder()
                .paymentTransactionId(dto.getPaymentTransactionId())
                .paymentMethodCode(dto.getPaymentMethodCode() != null ? dto.getPaymentMethodCode().name() : null)
                .providerCode(dto.getProviderCode())
                .providerTransactionId(dto.getProviderTransactionId())
                .providerPaymentUrl(dto.getProviderPaymentUrl())
                .amount(dto.getAmount())
                .currencyCode(dto.getCurrencyCode())
                .status(dto.getStatus() != null ? dto.getStatus().name() : null)
                .requestedAt(dto.getRequestedAt())
                .settledAt(dto.getSettledAt())
                .expiresAt(dto.getExpiresAt())
                .build();
    }
}