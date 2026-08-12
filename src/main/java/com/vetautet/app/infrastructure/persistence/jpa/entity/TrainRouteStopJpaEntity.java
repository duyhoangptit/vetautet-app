
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "train_route_stops", uniqueConstraints = {
        @UniqueConstraint(name = "uk_train_route_stop_sequence", columnNames = {"route_id", "stop_sequence"}),
        @UniqueConstraint(name = "uk_train_route_stop_station", columnNames = {"route_id", "station_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteStopJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "route_stop_id", nullable = false, updatable = false)
    private UUID routeStopId;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "distance_from_origin_km", precision = 10, scale = 2)
    private BigDecimal distanceFromOriginKm;

    @Column(name = "planned_arrival_offset_minutes")
    private Integer plannedArrivalOffsetMinutes;

    @Column(name = "planned_departure_offset_minutes")
    private Integer plannedDepartureOffsetMinutes;
}
