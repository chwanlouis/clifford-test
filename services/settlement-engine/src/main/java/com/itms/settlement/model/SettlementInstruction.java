package com.itms.settlement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_instructions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String settlementReference;

    @Column(nullable = false)
    private String tradeReference;

    @Column(nullable = false)
    private String isin;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal settlementAmount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String deliverBic;

    @Column(nullable = false)
    private String receiveBic;

    @Column(nullable = false)
    private String deliverAccount;

    @Column(nullable = false)
    private String receiveAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementType type;

    @Column(nullable = false)
    private LocalDate settlementDate;

    private LocalDateTime settledAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SettlementStatus.PENDING;
        }
        if (settlementReference == null) {
            settlementReference = "SET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
