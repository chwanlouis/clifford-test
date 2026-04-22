package com.itms.tradecapture.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String tradeReference;

    @NotBlank
    @Column(nullable = false)
    private String instrument;

    @NotBlank
    @Column(nullable = false)
    private String isin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeSide side;

    @Positive
    @Column(nullable = false)
    private BigDecimal quantity;

    @Positive
    @Column(nullable = false)
    private BigDecimal price;

    @NotBlank
    @Column(nullable = false)
    private String currency;

    @NotBlank
    @Column(nullable = false)
    private String counterparty;

    @NotBlank
    @Column(nullable = false)
    private String trader;

    @NotBlank
    @Column(nullable = false)
    private String portfolio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetClass assetClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime tradeDate;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime settlementDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tradeReference == null) {
            tradeReference = "TRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (status == null) {
            status = TradeStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getNotional() {
        return quantity.multiply(price);
    }
}
