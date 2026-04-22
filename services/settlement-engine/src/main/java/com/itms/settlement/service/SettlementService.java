package com.itms.settlement.service;

import com.itms.settlement.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {

    private final SwiftMessageService swiftMessageService;

    private final Map<String, SettlementInstruction> instructions = new ConcurrentHashMap<>();

    public SettlementInstruction createSettlementInstruction(SettlementInstruction instruction) {
        log.info("Creating settlement instruction for trade: {}", instruction.getTradeReference());
        instructions.put(instruction.getTradeReference(), instruction);

        // Auto-generate SWIFT message
        SwiftMessage swiftMsg = generateSwiftForInstruction(instruction);
        log.info("Generated {} SWIFT message for settlement: {}", swiftMsg.getMessageType(),
                instruction.getSettlementReference());

        return instruction;
    }

    public Optional<SettlementInstruction> findByTradeReference(String tradeReference) {
        return Optional.ofNullable(instructions.get(tradeReference));
    }

    public List<SettlementInstruction> getPendingSettlements() {
        return instructions.values().stream()
                .filter(i -> i.getStatus() == SettlementStatus.PENDING
                        || i.getStatus() == SettlementStatus.INSTRUCTED)
                .toList();
    }

    public List<SettlementInstruction> getSettlementsForDate(LocalDate date) {
        return instructions.values().stream()
                .filter(i -> i.getSettlementDate().equals(date))
                .toList();
    }

    public SettlementInstruction updateSettlementStatus(String tradeReference, SettlementStatus newStatus) {
        SettlementInstruction instruction = instructions.get(tradeReference);
        if (instruction == null) {
            throw new IllegalArgumentException("Settlement instruction not found: " + tradeReference);
        }
        instruction.setStatus(newStatus);
        if (newStatus == SettlementStatus.SETTLED) {
            instruction.setSettledAt(LocalDateTime.now());
        }
        log.info("Settlement {} status updated to {}", instruction.getSettlementReference(), newStatus);
        return instruction;
    }

    public List<SettlementInstruction> getAll() {
        return new ArrayList<>(instructions.values());
    }

    private SwiftMessage generateSwiftForInstruction(SettlementInstruction instruction) {
        if (instruction.getType() == SettlementType.DVP) {
            return swiftMessageService.generateMT541(
                    instruction.getDeliverBic(),
                    instruction.getReceiveBic(),
                    instruction.getSettlementReference(),
                    instruction.getIsin(),
                    instruction.getQuantity(),
                    instruction.getSettlementAmount(),
                    instruction.getCurrency(),
                    instruction.getSettlementDate(),
                    instruction.getReceiveAccount());
        } else {
            return swiftMessageService.generateMT540(
                    instruction.getDeliverBic(),
                    instruction.getReceiveBic(),
                    instruction.getSettlementReference(),
                    instruction.getIsin(),
                    instruction.getQuantity(),
                    instruction.getSettlementDate(),
                    instruction.getReceiveAccount());
        }
    }
}
