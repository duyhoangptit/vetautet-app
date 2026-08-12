
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.ticketing.model.StationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "railway_stations", indexes = {
        @Index(name = "uk_railway_stations_code", columnList = "station_code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RailwayStationJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "station_id", nullable = false, updatable = false)
    private UUID stationId;

    @Column(name = "station_code", nullable = false, unique = true, length = 20)
    private String stationCode;

    @Column(name = "station_name", nullable = false, length = 150)
    private String stationName;

    @Column(name = "city_code", nullable = false, length = 20)
    private String cityCode;

    @Column(name = "timezone_name", nullable = false, length = 50)
    private String timezoneName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 3)
    private StationStatus status;
}