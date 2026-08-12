package com.vetautet.app.application.ticketing.usecase;

import com.vetautet.app.application.ticketing.dto.DepartureAvailabilityDto;
import com.vetautet.app.application.ticketing.dto.DepartureBucketAvailabilityDto;
import com.vetautet.app.application.ticketing.dto.SearchDepartureQuery;
import com.vetautet.app.application.ticketing.port.input.SearchDepartureUseCase;
import com.vetautet.app.domain.inventory.model.InventorySaleStatus;
import com.vetautet.app.domain.inventory.repository.DepartureInventoryBucketRepository;
import com.vetautet.app.domain.ticketing.model.TrainDepartureStatus;
import com.vetautet.app.domain.ticketing.repository.TrainDepartureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchDepartureUseCaseImpl implements SearchDepartureUseCase {

    private final TrainDepartureRepository trainDepartureRepository;
    private final DepartureInventoryBucketRepository departureInventoryBucketRepository;

    @Override
    public List<DepartureAvailabilityDto> execute(SearchDepartureQuery query) {
        Instant currentTime = query.getCurrentTime() != null ? query.getCurrentTime() : Instant.now();

        return trainDepartureRepository.findByBusinessDateAndStatus(query.getBusinessDate(), TrainDepartureStatus.OPN)
                .stream()
                .filter(departure -> departure.getOriginStationId().equals(query.getOriginStationId()))
                .filter(departure -> departure.getDestinationStationId().equals(query.getDestinationStationId()))
                .filter(departure -> !departure.getSaleOpensAt().isAfter(currentTime))
                .filter(departure -> !departure.getSaleClosesAt().isBefore(currentTime))
                .map(departure -> DepartureAvailabilityDto.builder()
                        .departureId(departure.getDepartureId())
                        .departureCode(departure.getDepartureCode())
                        .businessDate(departure.getBusinessDate())
                        .originStationId(departure.getOriginStationId())
                        .destinationStationId(departure.getDestinationStationId())
                        .plannedDepartureAt(departure.getPlannedDepartureAt())
                        .plannedArrivalAt(departure.getPlannedArrivalAt())
                        .buckets(departureInventoryBucketRepository.findByDepartureIdAndSaleStatus(
                                        departure.getDepartureId(),
                                        InventorySaleStatus.OPN)
                                .stream()
                                .filter(bucket -> bucket.getAvailableQuantity() != null && bucket.getAvailableQuantity() > 0)
                                .map(bucket -> DepartureBucketAvailabilityDto.builder()
                                        .inventoryBucketId(bucket.getInventoryBucketId())
                                        .seatClassCode(bucket.getSeatClassCode())
                                        .quotaCode(bucket.getQuotaCode())
                                        .bucketNo(bucket.getBucketNo())
                                        .availableQuantity(bucket.getAvailableQuantity())
                                        .fareAmount(bucket.getFareAmount())
                                        .currencyCode(bucket.getCurrencyCode())
                                        .build())
                                .toList())
                        .build())
                .filter(dto -> !dto.getBuckets().isEmpty())
                .toList();
    }
}