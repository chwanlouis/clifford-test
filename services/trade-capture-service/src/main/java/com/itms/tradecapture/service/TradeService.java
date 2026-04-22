package com.itms.tradecapture.service;

import com.itms.tradecapture.kafka.TradeEventProducer;
import com.itms.tradecapture.model.Trade;
import com.itms.tradecapture.model.TradeStatus;
import com.itms.tradecapture.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final TradeEventProducer tradeEventProducer;

    @Transactional
    public Trade captureTrade(Trade trade) {
        log.info("Capturing trade: instrument={}, side={}, qty={}", trade.getInstrument(), trade.getSide(), trade.getQuantity());
        trade.setStatus(TradeStatus.PENDING_VALIDATION);
        Trade savedTrade = tradeRepository.save(trade);
        tradeEventProducer.publishTradeCreatedEvent(savedTrade);
        log.info("Trade captured with reference: {}", savedTrade.getTradeReference());
        return savedTrade;
    }

    @Transactional(readOnly = true)
    public Optional<Trade> findById(UUID id) {
        return tradeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Trade> findByReference(String reference) {
        return tradeRepository.findByTradeReference(reference);
    }

    @Transactional(readOnly = true)
    public List<Trade> findByPortfolio(String portfolio) {
        return tradeRepository.findByPortfolio(portfolio);
    }

    @Transactional(readOnly = true)
    public List<Trade> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return tradeRepository.findByTradeDateBetween(from, to);
    }

    @Transactional(readOnly = true)
    public List<Trade> findAll() {
        return tradeRepository.findAll();
    }

    @Transactional
    public Trade updateTradeStatus(UUID id, TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + id));
        log.info("Updating trade {} status from {} to {}", trade.getTradeReference(), trade.getStatus(), newStatus);
        trade.setStatus(newStatus);
        Trade updated = tradeRepository.save(trade);
        tradeEventProducer.publishTradeStatusChangedEvent(updated);
        return updated;
    }

    @Transactional
    public Trade amendTrade(UUID id, Trade amendment) {
        Trade existing = tradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + id));
        if (existing.getStatus() == TradeStatus.SETTLED || existing.getStatus() == TradeStatus.CANCELLED) {
            throw new IllegalStateException("Cannot amend trade in status: " + existing.getStatus());
        }
        existing.setQuantity(amendment.getQuantity());
        existing.setPrice(amendment.getPrice());
        existing.setSettlementDate(amendment.getSettlementDate());
        Trade updated = tradeRepository.save(existing);
        tradeEventProducer.publishTradeAmendedEvent(updated);
        return updated;
    }

    @Transactional
    public Trade cancelTrade(UUID id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + id));
        if (trade.getStatus() == TradeStatus.SETTLED) {
            throw new IllegalStateException("Cannot cancel a settled trade");
        }
        trade.setStatus(TradeStatus.CANCELLED);
        Trade cancelled = tradeRepository.save(trade);
        tradeEventProducer.publishTradeCancelledEvent(cancelled);
        return cancelled;
    }
}
