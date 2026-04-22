package com.itms.compliance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCheckResult {
    private String tradeReference;
    private ComplianceRegime regime;
    private boolean passed;
    private List<String> violations;
    private List<String> warnings;
    private LocalDateTime checkedAt;
    private String reportingObligationStatus;
}
