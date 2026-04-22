package com.itms.risk.controller;

import com.itms.risk.model.LimitType;
import com.itms.risk.model.RiskCheckResult;
import com.itms.risk.model.RiskLimit;
import com.itms.risk.service.LimitMonitoringService;
import com.itms.risk.service.VaRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final LimitMonitoringService limitMonitoringService;
    private final VaRService varService;

    @PostMapping("/limits")
    public ResponseEntity<Void> setLimit(@RequestBody RiskLimit limit) {
        limitMonitoringService.setLimit(limit);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check")
    public ResponseEntity<RiskCheckResult> checkLimit(
            @RequestParam String portfolio,
            @RequestParam String counterparty,
            @RequestParam LimitType limitType,
            @RequestParam BigDecimal proposedExposure,
            @RequestParam BigDecimal currentExposure,
            @RequestParam String currency) {
        return ResponseEntity.ok(limitMonitoringService.checkLimit(
                portfolio, counterparty, limitType, proposedExposure, currentExposure, currency));
    }

    @GetMapping("/limits/utilization")
    public ResponseEntity<List<RiskCheckResult>> getLimitUtilizations() {
        return ResponseEntity.ok(limitMonitoringService.getAllLimitUtilizations());
    }

    @PostMapping("/var/parametric")
    public ResponseEntity<BigDecimal> calculateParametricVaR(
            @RequestParam BigDecimal portfolioValue,
            @RequestParam BigDecimal dailyVolatility,
            @RequestParam(defaultValue = "0.99") double confidenceLevel,
            @RequestParam(defaultValue = "1") int holdingPeriodDays) {
        return ResponseEntity.ok(varService.calculateParametricVaR(
                portfolioValue, dailyVolatility, confidenceLevel, holdingPeriodDays));
    }

    @PostMapping("/var/montecarlo")
    public ResponseEntity<BigDecimal> calculateMonteCarloVaR(
            @RequestParam BigDecimal portfolioValue,
            @RequestParam BigDecimal annualReturn,
            @RequestParam BigDecimal annualVolatility,
            @RequestParam(defaultValue = "0.99") double confidenceLevel,
            @RequestParam(defaultValue = "10000") int numSimulations) {
        return ResponseEntity.ok(varService.calculateMonteCarloVaR(
                portfolioValue, annualReturn, annualVolatility, confidenceLevel, numSimulations));
    }
}
