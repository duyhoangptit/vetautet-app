
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ticketing.model.TrainRouteStop;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainRouteStopJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TrainRouteStopEntityMapper {

    public TrainRouteStop toDomain(TrainRouteStopJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return TrainRouteStop.builder()
                .routeStopId(entity.getRouteStopId())
                .routeId(entity.getRouteId())
                .stationId(entity.getStationId())
                .stopSequence(entity.getStopSequence())
                .distanceFromOriginKm(entity.getDistanceFromOriginKm())
                .plannedArrivalOffsetMinutes(entity.getPlannedArrivalOffsetMinutes())
                .plannedDepartureOffsetMinutes(entity.getPlannedDepartureOffsetMinutes())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public TrainRouteStopJpaEntity toEntity(TrainRouteStop domain) {
        if (domain == null) {
            return null;
        }

        TrainRouteStopJpaEntity entity = TrainRouteStopJpaEntity.builder()
                .routeStopId(domain.getRouteStopId())
                .routeId(domain.getRouteId())
                .stationId(domain.getStationId())
                .stopSequence(domain.getStopSequence())
                .distanceFromOriginKm(domain.getDistanceFromOriginKm())
                .plannedArrivalOffsetMinutes(domain.getPlannedArrivalOffsetMinutes())
                .plannedDepartureOffsetMinutes(domain.getPlannedDepartureOffsetMinutes())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}