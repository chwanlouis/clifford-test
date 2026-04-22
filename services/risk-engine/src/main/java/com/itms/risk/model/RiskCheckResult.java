package com.itms.risk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResult {
    private String portfolio;
    private String counterparty;
    private LimitType limitType;
    private BigDecimal currentExposure;
    private BigDecimal limitAmount;
    private BigDecimal utilizationPercent;
    private boolean breached;
    private String message;
    private LocalDateTime checkedAt;
}
