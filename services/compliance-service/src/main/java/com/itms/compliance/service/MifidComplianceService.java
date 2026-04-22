package com.itms.compliance.service;

import com.itms.compliance.model.ComplianceCheckResult;
import com.itms.compliance.model.ComplianceRegime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MifidComplianceService {

    // MiFID II thresholds
    private static final BigDecimal LARGE_IN_SCALE_EQUITY_THRESHOLD = new BigDecimal("15000"); // EUR
    private static final BigDecimal SYSTEMATIC_INTERNALISER_THRESHOLD = new BigDecimal("1000000");

    public ComplianceCheckResult checkMifidII(String tradeReference, String instrument,
                                               String assetClass, BigDecimal notional,
                                               String venue, String executionType,
                                               String counterpartyType, Map<String, String> tradeData) {
        log.info("Running MiFID II compliance check for trade: {}", tradeReference);
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Best Execution (RTS 27/28)
        if ("OTC".equals(venue) && notional.compareTo(LARGE_IN_SCALE_EQUITY_THRESHOLD) > 0) {
            warnings.add("Large OTC trade - best execution documentation required");
        }

        // Transaction Reporting (RTS 22/23)
        boolean reportingRequired = isReportingRequired(assetClass, counterpartyType);
        String reportingStatus = reportingRequired ? "REQUIRED_T+1" : "NOT_REQUIRED";

        // Pre-trade transparency
        if ("EQUITY".equals(assetClass) && !"DARK_POOL".equals(venue)) {
            if (notional.compareTo(LARGE_IN_SCALE_EQUITY_THRESHOLD) < 0) {
                warnings.add("Pre-trade transparency waiver may not apply for notional below LIS threshold");
            }
        }

        // Systematic Internaliser check
        if (notional.compareTo(SYSTEMATIC_INTERNALISER_THRESHOLD) > 0) {
            warnings.add("Trade may be subject to SI regime obligations");
        }

        // Position limits check (commodities under MiFID II Art.57)
        if ("COMMODITY".equals(assetClass)) {
            warnings.add("Commodity position limits apply - check against Art.57 thresholds");
        }

        boolean passed = violations.isEmpty();
        log.info("MiFID II check for {}: passed={}, violations={}", tradeReference, passed, violations.size());

        return ComplianceCheckResult.builder()
                .tradeReference(tradeReference)
                .regime(ComplianceRegime.MIFID_II)
                .passed(passed)
                .violations(violations)
                .warnings(warnings)
                .checkedAt(LocalDateTime.now())
                .reportingObligationStatus(reportingStatus)
                .build();
    }

    private boolean isReportingRequired(String assetClass, String counterpartyType) {
        // All instruments under MiFID II Art.26 must be reported if the firm is subject to MiFID
        return !"RETAIL".equals(counterpartyType);
    }
}
