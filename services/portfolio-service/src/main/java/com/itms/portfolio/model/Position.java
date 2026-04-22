package com.itms.portfolio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "positions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String portfolio;

    @Column(nullable = false)
    private String instrument;

    @Column(nullable = false)
    private String isin;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal averageCost;

    @Column(nullable = false)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private BigDecimal marketValue;

    @Column(nullable = false)
    private BigDecimal unrealizedPnl;

    @Column(nullable = false)
    private BigDecimal realizedPnl;

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        marketValue = quantity.multiply(currentPrice);
        unrealizedPnl = quantity.multiply(currentPrice.subtract(averageCost));
    }

    public BigDecimal getTotalPnl() {
        return unrealizedPnl.add(realizedPnl);
    }
}
