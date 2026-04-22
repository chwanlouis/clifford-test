package com.itms.oms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderReference;

    @Column(nullable = false)
    private String instrument;

    @Column(nullable = false)
    private String isin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Column(nullable = false)
    private BigDecimal totalQuantity;

    private BigDecimal limitPrice;

    private BigDecimal stopPrice;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String portfolio;

    @Column(nullable = false)
    private String trader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionAlgorithm algorithm;

    @Column(nullable = false)
    private BigDecimal filledQuantity;

    private BigDecimal averageFillPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderReference == null) {
            orderReference = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (filledQuantity == null) {
            filledQuantity = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getRemainingQuantity() {
        return totalQuantity.subtract(filledQuantity);
    }

    public boolean isFullyFilled() {
        return filledQuantity.compareTo(totalQuantity) >= 0;
    }
}
