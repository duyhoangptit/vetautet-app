
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.inventory.model.InventorySaleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "departure_inventory_buckets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_departure_inventory_bucket", columnNames = {"departure_id", "seat_class_code", "quota_code", "bucket_no"})
}, indexes = {
        @Index(name = "idx_departure_inventory_sale_status", columnList = "departure_id,sale_status,seat_class_code,quota_code"),
        @Index(name = "idx_departure_inventory_hot_bucket", columnList = "departure_id,seat_class_code,quota_code,bucket_no,available_quantity")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureInventoryBucketJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "inventory_bucket_id", nullable = false, updatable = false)
    private UUID inventoryBucketId;

    @Column(name = "departure_id", nullable = false)
    private UUID departureId;

    @Column(name = "seat_class_code", nullable = false, length = 20)
    private String seatClassCode;

    @Column(name = "quota_code", nullable = false, length = 20)
    private String quotaCode;

    @Column(name = "bucket_no", nullable = false)
    private Short bucketNo;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity;

    @Column(name = "oversell_limit", nullable = false)
    private Integer oversellLimit;

    @Column(name = "fare_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal fareAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 3)
    private InventorySaleStatus saleStatus;
}
