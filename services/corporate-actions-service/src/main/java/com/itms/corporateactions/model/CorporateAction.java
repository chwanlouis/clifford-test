package com.itms.corporateactions.model;

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
@Table(name = "corporate_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorporateAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String eventReference;

    @Column(nullable = false)
    private String isin;

    @Column(nullable = false)
    private String issuer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorporateActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorporateActionStatus status;

    @Column(nullable = false)
    private LocalDate announcementDate;

    private LocalDate exDate;
    private LocalDate recordDate;

    @Column(nullable = false)
    private LocalDate paymentDate;

    // Dividend fields
    private BigDecimal dividendPerShare;
    private String dividendCurrency;

    // Split fields
    private BigDecimal splitRatioNew;     // e.g. 4 in 4:1 split
    private BigDecimal splitRatioOld;     // e.g. 1 in 4:1 split

    // Merger fields
    private String newIsin;
    private BigDecimal exchangeRatio;

    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (eventReference == null) {
            eventReference = "CA-" + type.name().substring(0, 3) + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
        if (status == null) {
            status = CorporateActionStatus.ANNOUNCED;
        }
    }
}
