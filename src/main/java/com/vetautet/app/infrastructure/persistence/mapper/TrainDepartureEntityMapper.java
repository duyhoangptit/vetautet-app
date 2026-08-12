
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ticketing.model.TrainDeparture;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainDepartureJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TrainDepartureEntityMapper {

    public TrainDeparture toDomain(TrainDepartureJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return TrainDeparture.builder()
                .departureId(entity.getDepartureId())
                .routeId(entity.getRouteId())
                .trainId(entity.getTrainId())
                .departureCode(entity.getDepartureCode())
                .businessDate(entity.getBusinessDate())
                .originStationId(entity.getOriginStationId())
                .destinationStationId(entity.getDestinationStationId())
                .plannedDepartureAt(entity.getPlannedDepartureAt())
                .plannedArrivalAt(entity.getPlannedArrivalAt())
                .saleOpensAt(entity.getSaleOpensAt())
                .saleClosesAt(entity.getSaleClosesAt())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public TrainDepartureJpaEntity toEntity(TrainDeparture domain) {
        if (domain == null) {
            return null;
        }

        TrainDepartureJpaEntity entity = TrainDepartureJpaEntity.builder()
                .departureId(domain.getDepartureId())
                .routeId(domain.getRouteId())
                .trainId(domain.getTrainId())
                .departureCode(domain.getDepartureCode())
                .businessDate(domain.getBusinessDate())
                .originStationId(domain.getOriginStationId())
                .destinationStationId(domain.getDestinationStationId())
                .plannedDepartureAt(domain.getPlannedDepartureAt())
                .plannedArrivalAt(domain.getPlannedArrivalAt())
                .saleOpensAt(domain.getSaleOpensAt())
                .saleClosesAt(domain.getSaleClosesAt())
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