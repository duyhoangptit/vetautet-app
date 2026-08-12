
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ticketing.model.Train;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TrainEntityMapper {

    public Train toDomain(TrainJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Train.builder()
                .trainId(entity.getTrainId())
                .trainCode(entity.getTrainCode())
                .trainName(entity.getTrainName())
                .seatLayoutVersion(entity.getSeatLayoutVersion())
                .operatorCode(entity.getOperatorCode())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public TrainJpaEntity toEntity(Train domain) {
        if (domain == null) {
            return null;
        }

        TrainJpaEntity entity = TrainJpaEntity.builder()
                .trainId(domain.getTrainId())
                .trainCode(domain.getTrainCode())
                .trainName(domain.getTrainName())
                .seatLayoutVersion(domain.getSeatLayoutVersion())
                .operatorCode(domain.getOperatorCode())
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