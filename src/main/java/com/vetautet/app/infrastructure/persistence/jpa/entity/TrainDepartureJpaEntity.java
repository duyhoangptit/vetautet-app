

package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.ticketing.model.TrainDepartureStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "train_departures", uniqueConstraints = {
        @UniqueConstraint(name = "uk_train_departures_business", columnNames = {"route_id", "train_id", "business_date"})
}, indexes = {
        @Index(name = "uk_train_departures_code", columnList = "departure_code", unique = true),
        @Index(name = "idx_train_departures_business_date", columnList = "business_date,status"),
        @Index(name = "idx_train_departures_route_time", columnList = "route_id,planned_departure_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TrainDepartureJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "departure_id", nullable = false, updatable = false)
    private UUID departureId;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "train_id", nullable = false)
    private UUID trainId;

    @Column(name = "departure_code", nullable = false, unique = true, length = 40)
    private String departureCode;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "origin_station_id", nullable = false)
    private UUID originStationId;

    @Column(name = "destination_station_id", nullable = false)
    private UUID destinationStationId;

    @Column(name = "planned_departure_at", nullable = false)
    private Instant plannedDepartureAt;

    @Column(name = "planned_arrival_at", nullable = false)
    private Instant plannedArrivalAt;

    @Column(name = "sale_opens_at", nullable = false)
    private Instant saleOpensAt;

    @Column(name = "sale_closes_at", nullable = false)
    private Instant saleClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 3)
    private TrainDepartureStatus status;
}
