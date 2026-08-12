package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ticketing.model.RailwayStation;
import com.vetautet.app.infrastructure.persistence.jpa.entity.RailwayStationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RailwayStationEntityMapper {

    public RailwayStation toDomain(RailwayStationJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return RailwayStation.builder()
                .stationId(entity.getStationId())
                .stationCode(entity.getStationCode())
                .stationName(entity.getStationName())
                .cityCode(entity.getCityCode())
                .timezoneName(entity.getTimezoneName())
                .displayOrder(entity.getDisplayOrder())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public RailwayStationJpaEntity toEntity(RailwayStation domain) {
        if (domain == null) {
            return null;
        }

        RailwayStationJpaEntity entity = RailwayStationJpaEntity.builder()
                .stationId(domain.getStationId())
                .stationCode(domain.getStationCode())
                .stationName(domain.getStationName())
                .cityCode(domain.getCityCode())
                .timezoneName(domain.getTimezoneName())
                .displayOrder(domain.getDisplayOrder())
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