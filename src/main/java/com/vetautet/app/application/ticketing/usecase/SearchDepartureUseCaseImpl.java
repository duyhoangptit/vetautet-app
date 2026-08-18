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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchDepartureUseCaseImpl implements SearchDepartureUseCase {

    private final TrainDepartureRepository trainDepartureRepository;
    private final DepartureInventoryBucketRepository departureInventoryBucketRepository;

    // Public, unauthenticated endpoint (see SecurityConfig) - short TTL
    // ("spring.cache.redis.caches.departure-search", currently 30s) because
    // seat availability changes on every booking. This only smooths out
    // repeated searches for the same route/date; it does not protect against
    // an attacker varying params to defeat the cache - that's what
    // @RateLimit(scope = "departureSearch") on DepartureController is for.
    @Override
    @Cacheable(value = "departure-search",
            key = "#query.originStationId + ':' + #query.destinationStationId + ':' + #query.businessDate")
    public List<DepartureAvailabilityDto> execute(SearchDepartureQuery query) {
        Instant currentTime = query.getCurrentTime() != null ? query.getCurrentTime() : Instant.now();

        return trainDepartureRepository.findByBusinessDateAndStatusAndOriginStationIdAndDestinationStationId(
                        query.getBusinessDate(), TrainDepartureStatus.OPN,
                        query.getOriginStationId(), query.getDestinationStationId())
                .stream()
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