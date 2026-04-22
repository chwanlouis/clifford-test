package com.itms.affirmation.controller;

import com.itms.affirmation.model.TradeMatch;
import com.itms.affirmation.service.TradeMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/affirmation")
@RequiredArgsConstructor
public class AffirmationController {

    private final TradeMatchingService tradeMatchingService;

    @PostMapping("/submit")
    public ResponseEntity<TradeMatch> submitTrade(@RequestBody TradeMatch trade) {
        TradeMatch result = tradeMatchingService.submitTradeForAffirmation(trade);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{tradeReference}/affirm")
    public ResponseEntity<TradeMatch> affirmTrade(@PathVariable String tradeReference) {
        return ResponseEntity.ok(tradeMatchingService.affirmTrade(tradeReference));
    }

    @PostMapping("/match")
    public ResponseEntity<TradeMatch> matchTrades(@RequestBody TradeMatch ourTrade,
                                                   @RequestBody TradeMatch counterpartyTrade) {
        return ResponseEntity.ok(tradeMatchingService.matchTrades(ourTrade, counterpartyTrade));
    }
}
