package com.itms.risk.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_limits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String portfolio;

    @Column(nullable = false)
    private String counterparty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LimitType limitType;

    @Column(nullable = false)
    private BigDecimal limitAmount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
