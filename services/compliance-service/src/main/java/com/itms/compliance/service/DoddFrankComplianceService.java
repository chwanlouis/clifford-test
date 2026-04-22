package com.itms.compliance.service;

import com.itms.compliance.model.ComplianceCheckResult;
import com.itms.compliance.model.ComplianceRegime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DoddFrankComplianceService {

    // Dodd-Frank reporting thresholds (USD)
    private static final BigDecimal SWAP_REPORTING_THRESHOLD = new BigDecimal("8000000");
    private static final BigDecimal BLOCK_TRADE_MINIMUM = new BigDecimal("50000000");

    public ComplianceCheckResult checkDoddFrank(String tradeReference, String instrument,
                                                 String assetClass, BigDecimal notional,
                                                 String counterpartyType, String counterpartyLei) {
        log.info("Running Dodd-Frank compliance check for trade: {}", tradeReference);
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Swap Dealer Registration check (Title VII)
        if (isSwapProduct(assetClass)) {
            if (counterpartyLei == null || counterpartyLei.isEmpty()) {
                violations.add("LEI required for swap counterparty under Dodd-Frank Title VII");
            }
            // SDR Reporting
            if (notional.compareTo(SWAP_REPORTING_THRESHOLD) > 0) {
                warnings.add("Swap must be reported to SDR within 15 minutes (real-time reporting)");
            }
            // Clearing mandate check (Section 2(h))
            if ("FINANCIAL_ENTITY".equals(counterpartyType)) {
                warnings.add("Mandatory clearing obligation may apply for this swap (Section 2(h))");
            }
        }

        // Block trade check
        if (notional.compareTo(BLOCK_TRADE_MINIMUM) > 0) {
            warnings.add("Potential block trade - delayed reporting may be available");
        }

        // Volcker Rule check (Proprietary Trading prohibition)
        if ("PROP_TRADING".equals(counterpartyType)) {
            violations.add("Volcker Rule: Proprietary trading in covered financial products is prohibited");
        }

        boolean passed = violations.isEmpty();
        log.info("Dodd-Frank check for {}: passed={}, violations={}", tradeReference, passed, violations.size());

        return ComplianceCheckResult.builder()
                .tradeReference(tradeReference)
                .regime(ComplianceRegime.DODD_FRANK)
                .passed(passed)
                .violations(violations)
                .warnings(warnings)
                .checkedAt(LocalDateTime.now())
                .reportingObligationStatus(isSwapProduct(assetClass) ? "SDR_REQUIRED" : "NOT_REQUIRED")
                .build();
    }

    private boolean isSwapProduct(String assetClass) {
        return "INTEREST_RATE_SWAP".equals(assetClass)
                || "CREDIT_DEFAULT_SWAP".equals(assetClass)
                || "COMMODITY_SWAP".equals(assetClass)
                || "EQUITY_SWAP".equals(assetClass);
    }
}
