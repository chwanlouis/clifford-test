package com.itms.settlement.controller;

import com.itms.settlement.model.SettlementInstruction;
import com.itms.settlement.model.SettlementStatus;
import com.itms.settlement.model.SwiftMessage;
import com.itms.settlement.service.SettlementService;
import com.itms.settlement.service.SwiftMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final SwiftMessageService swiftMessageService;

    @PostMapping("/instructions")
    public ResponseEntity<SettlementInstruction> createInstruction(
            @RequestBody SettlementInstruction instruction) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(settlementService.createSettlementInstruction(instruction));
    }

    @GetMapping("/instructions")
    public ResponseEntity<List<SettlementInstruction>> getAllInstructions() {
        return ResponseEntity.ok(settlementService.getAll());
    }

    @GetMapping("/instructions/pending")
    public ResponseEntity<List<SettlementInstruction>> getPendingSettlements() {
        return ResponseEntity.ok(settlementService.getPendingSettlements());
    }

    @GetMapping("/instructions/date/{date}")
    public ResponseEntity<List<SettlementInstruction>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(settlementService.getSettlementsForDate(date));
    }

    @PatchMapping("/instructions/{tradeReference}/status")
    public ResponseEntity<SettlementInstruction> updateStatus(
            @PathVariable String tradeReference,
            @RequestParam SettlementStatus status) {
        return ResponseEntity.ok(settlementService.updateSettlementStatus(tradeReference, status));
    }

    @PostMapping("/swift/mt103")
    public ResponseEntity<SwiftMessage> generateMT103(
            @RequestParam String senderBic,
            @RequestParam String receiverBic,
            @RequestParam String ref,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valueDate,
            @RequestParam String beneficiaryName,
            @RequestParam String beneficiaryAccount) {
        return ResponseEntity.ok(swiftMessageService.generateMT103(
                senderBic, receiverBic, ref, amount, currency, valueDate,
                beneficiaryName, beneficiaryAccount));
    }
}
