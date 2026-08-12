
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.ticketing.model.TrainStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "trains", indexes = {
        @Index(name = "uk_trains_code", columnList = "train_code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TrainJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "train_id", nullable = false, updatable = false)
    private UUID trainId;

    @Column(name = "train_code", nullable = false, unique = true, length = 30)
    private String trainCode;

    @Column(name = "train_name", nullable = false, length = 150)
    private String trainName;

    @Column(name = "seat_layout_version", nullable = false, length = 30)
    private String seatLayoutVersion;

    @Column(name = "operator_code", nullable = false, length = 30)
    private String operatorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 3)
    private TrainStatus status;
}

