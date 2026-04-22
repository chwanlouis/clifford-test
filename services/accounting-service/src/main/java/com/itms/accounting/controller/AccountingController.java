package com.itms.accounting.controller;

import com.itms.accounting.model.JournalEntry;
import com.itms.accounting.model.JournalEntryType;
import com.itms.accounting.service.GeneralLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final GeneralLedgerService generalLedgerService;

    @PostMapping("/journal-entries")
    public ResponseEntity<JournalEntry> postJournalEntry(
            @RequestParam String tradeReference,
            @RequestParam String debitAccount,
            @RequestParam String creditAccount,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam String description,
            @RequestParam JournalEntryType type) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(generalLedgerService.postJournalEntry(
                        tradeReference, debitAccount, creditAccount, amount, currency, description, type));
    }

    @GetMapping("/journal-entries")
    public ResponseEntity<List<JournalEntry>> getAllEntries() {
        return ResponseEntity.ok(generalLedgerService.getAllEntries());
    }

    @GetMapping("/journal-entries/trade/{tradeReference}")
    public ResponseEntity<List<JournalEntry>> getEntriesByTrade(@PathVariable String tradeReference) {
        return ResponseEntity.ok(generalLedgerService.getEntriesByTradeReference(tradeReference));
    }

    @PostMapping("/trades/{tradeReference}/buy")
    public ResponseEntity<List<JournalEntry>> generateBuyEntries(
            @PathVariable String tradeReference,
            @RequestParam String portfolio,
            @RequestParam BigDecimal notional,
            @RequestParam String currency,
            @RequestParam(required = false) BigDecimal commission) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(generalLedgerService.generateTradeBuyEntries(
                        tradeReference, portfolio, notional, currency, commission));
    }

    @PostMapping("/trades/{tradeReference}/sell")
    public ResponseEntity<List<JournalEntry>> generateSellEntries(
            @PathVariable String tradeReference,
            @RequestParam String portfolio,
            @RequestParam BigDecimal proceeds,
            @RequestParam BigDecimal costBasis,
            @RequestParam String currency) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(generalLedgerService.generateTradeSellEntries(
                        tradeReference, portfolio, proceeds, costBasis, currency));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<Map<String, BigDecimal>> getTrialBalance() {
        return ResponseEntity.ok(generalLedgerService.getTrialBalance());
    }
}
