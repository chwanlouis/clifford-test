package com.itms.affirmation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_matches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String ourTradeReference;

    @Column(nullable = false)
    private String counterpartyTradeReference;

    @Column(nullable = false)
    private String isin;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String counterparty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private String mismatchReason;

    @Column(nullable = false)
    private LocalDateTime tradeDate;

    private LocalDateTime matchedAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = MatchStatus.PENDING;
        }
    }
}
