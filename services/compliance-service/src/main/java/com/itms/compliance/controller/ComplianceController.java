package com.itms.compliance.controller;

import com.itms.compliance.model.ComplianceCheckResult;
import com.itms.compliance.service.DoddFrankComplianceService;
import com.itms.compliance.service.MifidComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final MifidComplianceService mifidService;
    private final DoddFrankComplianceService doddFrankService;

    @PostMapping("/mifid2/check")
    public ResponseEntity<ComplianceCheckResult> checkMifid2(
            @RequestParam String tradeReference,
            @RequestParam String instrument,
            @RequestParam String assetClass,
            @RequestParam BigDecimal notional,
            @RequestParam(defaultValue = "EXCHANGE") String venue,
            @RequestParam(defaultValue = "AGENCY") String executionType,
            @RequestParam(defaultValue = "PROFESSIONAL") String counterpartyType) {
        ComplianceCheckResult result = mifidService.checkMifidII(
                tradeReference, instrument, assetClass, notional, venue, executionType,
                counterpartyType, new HashMap<>());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/dodd-frank/check")
    public ResponseEntity<ComplianceCheckResult> checkDoddFrank(
            @RequestParam String tradeReference,
            @RequestParam String instrument,
            @RequestParam String assetClass,
            @RequestParam BigDecimal notional,
            @RequestParam(defaultValue = "FINANCIAL_ENTITY") String counterpartyType,
            @RequestParam(required = false) String counterpartyLei) {
        ComplianceCheckResult result = doddFrankService.checkDoddFrank(
                tradeReference, instrument, assetClass, notional, counterpartyType, counterpartyLei);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/full-check")
    public ResponseEntity<List<ComplianceCheckResult>> fullComplianceCheck(
            @RequestParam String tradeReference,
            @RequestParam String instrument,
            @RequestParam String assetClass,
            @RequestParam BigDecimal notional,
            @RequestParam(defaultValue = "EXCHANGE") String venue,
            @RequestParam(defaultValue = "PROFESSIONAL") String counterpartyType,
            @RequestParam(required = false) String counterpartyLei) {
        ComplianceCheckResult mifidResult = mifidService.checkMifidII(
                tradeReference, instrument, assetClass, notional, venue, "AGENCY",
                counterpartyType, new HashMap<>());
        ComplianceCheckResult doddFrankResult = doddFrankService.checkDoddFrank(
                tradeReference, instrument, assetClass, notional, counterpartyType, counterpartyLei);
        return ResponseEntity.ok(List.of(mifidResult, doddFrankResult));
    }
}
