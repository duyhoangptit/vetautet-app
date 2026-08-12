package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ticketing.model.TrainRoute;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainRouteJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TrainRouteEntityMapper {

    public TrainRoute toDomain(TrainRouteJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return TrainRoute.builder()
                .routeId(entity.getRouteId())
                .routeCode(entity.getRouteCode())
                .routeName(entity.getRouteName())
                .originStationId(entity.getOriginStationId())
                .destinationStationId(entity.getDestinationStationId())
                .distanceKm(entity.getDistanceKm())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public TrainRouteJpaEntity toEntity(TrainRoute domain) {
        if (domain == null) {
            return null;
        }

        TrainRouteJpaEntity entity = TrainRouteJpaEntity.builder()
                .routeId(domain.getRouteId())
                .routeCode(domain.getRouteCode())
                .routeName(domain.getRouteName())
                .originStationId(domain.getOriginStationId())
                .destinationStationId(domain.getDestinationStationId())
                .distanceKm(domain.getDistanceKm())
                .status(domain.getStatus())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}