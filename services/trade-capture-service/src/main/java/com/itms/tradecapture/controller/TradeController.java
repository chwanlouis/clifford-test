package com.itms.tradecapture.controller;

import com.itms.tradecapture.model.Trade;
import com.itms.tradecapture.model.TradeStatus;
import com.itms.tradecapture.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trades")
@Slf4j
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<Trade> captureTrade(@Valid @RequestBody Trade trade) {
        Trade captured = tradeService.captureTrade(trade);
        return ResponseEntity.status(HttpStatus.CREATED).body(captured);
    }

    @GetMapping
    public ResponseEntity<List<Trade>> getAllTrades() {
        return ResponseEntity.ok(tradeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trade> getTradeById(@PathVariable UUID id) {
        return tradeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<Trade> getTradeByReference(@PathVariable String reference) {
        return tradeService.findByReference(reference)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portfolio/{portfolio}")
    public ResponseEntity<List<Trade>> getTradesByPortfolio(@PathVariable String portfolio) {
        return ResponseEntity.ok(tradeService.findByPortfolio(portfolio));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Trade>> getTradesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(tradeService.findByDateRange(from, to));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Trade> updateTradeStatus(@PathVariable UUID id,
                                                    @RequestParam TradeStatus status) {
        return ResponseEntity.ok(tradeService.updateTradeStatus(id, status));
    }

    @PutMapping("/{id}/amend")
    public ResponseEntity<Trade> amendTrade(@PathVariable UUID id,
                                             @Valid @RequestBody Trade amendment) {
        return ResponseEntity.ok(tradeService.amendTrade(id, amendment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Trade> cancelTrade(@PathVariable UUID id) {
        return ResponseEntity.ok(tradeService.cancelTrade(id));
    }
}
