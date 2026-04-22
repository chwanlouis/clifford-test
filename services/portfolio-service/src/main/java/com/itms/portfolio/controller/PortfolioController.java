package com.itms.portfolio.controller;

import com.itms.portfolio.model.Greeks;
import com.itms.portfolio.model.Position;
import com.itms.portfolio.service.GreeksService;
import com.itms.portfolio.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PositionService positionService;
    private final GreeksService greeksService;

    @GetMapping("/{portfolio}/positions")
    public ResponseEntity<List<Position>> getPositions(@PathVariable String portfolio) {
        return ResponseEntity.ok(positionService.getPortfolioPositions(portfolio));
    }

    @GetMapping("/{portfolio}/market-value")
    public ResponseEntity<BigDecimal> getMarketValue(@PathVariable String portfolio) {
        return ResponseEntity.ok(positionService.getPortfolioMarketValue(portfolio));
    }

    @GetMapping("/{portfolio}/pnl")
    public ResponseEntity<BigDecimal> getTotalPnl(@PathVariable String portfolio) {
        return ResponseEntity.ok(positionService.getPortfolioTotalPnl(portfolio));
    }

    @GetMapping("/{portfolio}/greeks/{instrument}")
    public ResponseEntity<Greeks> getGreeks(
            @PathVariable String portfolio,
            @PathVariable String instrument,
            @RequestParam BigDecimal spotPrice,
            @RequestParam BigDecimal strikePrice,
            @RequestParam BigDecimal riskFreeRate,
            @RequestParam BigDecimal volatility,
            @RequestParam BigDecimal timeToExpiry,
            @RequestParam(defaultValue = "true") boolean isCall) {
        Greeks greeks = greeksService.calculateBlackScholesGreeks(
                instrument, portfolio, spotPrice, strikePrice, riskFreeRate, volatility, timeToExpiry, isCall);
        return ResponseEntity.ok(greeks);
    }
}
