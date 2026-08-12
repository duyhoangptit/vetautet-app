


package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.booking.model.BookingOrderItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_order_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_order_item_line", columnNames = {"booking_order_id", "line_no"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookingOrderItemJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "booking_order_item_id", nullable = false, updatable = false)
    private UUID bookingOrderItemId;

    @Column(name = "booking_order_id", nullable = false)
    private UUID bookingOrderId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "inventory_bucket_id", nullable = false)
    private UUID inventoryBucketId;

    @Column(name = "travel_from_stop_sequence", nullable = false)
    private Integer travelFromStopSequence;

    @Column(name = "travel_to_stop_sequence", nullable = false)
    private Integer travelToStopSequence;

    @Column(name = "seat_class_code", nullable = false, length = 20)
    private String seatClassCode;

    @Column(name = "quota_code", nullable = false, length = 20)
    private String quotaCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "line_total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 3)
    private BookingOrderItemStatus itemStatus;
}
