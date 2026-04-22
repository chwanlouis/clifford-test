package com.itms.corporateactions.controller;

import com.itms.corporateactions.model.CorporateAction;
import com.itms.corporateactions.service.DividendService;
import com.itms.corporateactions.service.StockSplitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/corporate-actions")
@RequiredArgsConstructor
public class CorporateActionsController {

    private final DividendService dividendService;
    private final StockSplitService stockSplitService;

    @PostMapping("/dividend/process")
    public ResponseEntity<Map<String, BigDecimal>> processDividend(
            @RequestBody CorporateAction event,
            @RequestParam Map<String, BigDecimal> positionsByPortfolio) {
        return ResponseEntity.ok(dividendService.processDividend(event, positionsByPortfolio));
    }

    @PostMapping("/split/process")
    public ResponseEntity<Map<String, BigDecimal>> processStockSplit(
            @RequestBody CorporateAction event,
            @RequestParam Map<String, BigDecimal> positionsByPortfolio) {
        return ResponseEntity.ok(stockSplitService.processStockSplit(event, positionsByPortfolio));
    }

    @PostMapping("/events")
    public ResponseEntity<CorporateAction> createEvent(@RequestBody CorporateAction event) {
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }
}
