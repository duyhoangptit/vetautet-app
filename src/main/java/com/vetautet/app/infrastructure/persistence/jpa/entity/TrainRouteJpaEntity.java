
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.ticketing.model.TrainRouteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "train_routes", indexes = {
        @Index(name = "uk_train_routes_code", columnList = "route_code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    @Column(name = "route_code", nullable = false, unique = true, length = 30)
    private String routeCode;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(name = "origin_station_id", nullable = false)
    private UUID originStationId;

    @Column(name = "destination_station_id", nullable = false)
    private UUID destinationStationId;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 3)
    private TrainRouteStatus status;
}
