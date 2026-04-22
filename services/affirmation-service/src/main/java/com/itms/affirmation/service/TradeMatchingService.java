package com.itms.affirmation.service;

import com.itms.affirmation.model.MatchStatus;
import com.itms.affirmation.model.TradeMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TradeMatchingService {

    private static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.0001"); // 0.01 bps
    private static final BigDecimal QUANTITY_TOLERANCE = BigDecimal.ZERO;

    private final Map<String, TradeMatch> pendingMatches = new ConcurrentHashMap<>();

    public TradeMatch submitTradeForAffirmation(TradeMatch ourTrade) {
        log.info("Submitting trade for affirmation: {}", ourTrade.getOurTradeReference());
        String matchKey = buildMatchKey(ourTrade);
        pendingMatches.put(ourTrade.getOurTradeReference(), ourTrade);

        // Check if counterparty has already submitted their side
        TradeMatch counterpartyMatch = findCounterpartyMatch(ourTrade);
        if (counterpartyMatch != null) {
            return matchTrades(ourTrade, counterpartyMatch);
        }

        log.info("Trade {} is pending counterparty affirmation", ourTrade.getOurTradeReference());
        return ourTrade;
    }

    public TradeMatch matchTrades(TradeMatch ourTrade, TradeMatch counterpartyTrade) {
        log.info("Attempting to match trades: {} vs {}", ourTrade.getOurTradeReference(),
                counterpartyTrade.getOurTradeReference());

        List<String> mismatches = new ArrayList<>();

        if (!ourTrade.getIsin().equals(counterpartyTrade.getIsin())) {
            mismatches.add("ISIN mismatch: " + ourTrade.getIsin() + " vs " + counterpartyTrade.getIsin());
        }
        if (ourTrade.getQuantity().subtract(counterpartyTrade.getQuantity()).abs().compareTo(QUANTITY_TOLERANCE) > 0) {
            mismatches.add("Quantity mismatch: " + ourTrade.getQuantity() + " vs " + counterpartyTrade.getQuantity());
        }
        if (ourTrade.getPrice().subtract(counterpartyTrade.getPrice()).abs().compareTo(PRICE_TOLERANCE) > 0) {
            mismatches.add("Price mismatch: " + ourTrade.getPrice() + " vs " + counterpartyTrade.getPrice());
        }
        if (!ourTrade.getCurrency().equals(counterpartyTrade.getCurrency())) {
            mismatches.add("Currency mismatch: " + ourTrade.getCurrency() + " vs " + counterpartyTrade.getCurrency());
        }

        if (mismatches.isEmpty()) {
            ourTrade.setStatus(MatchStatus.MATCHED);
            ourTrade.setMatchedAt(LocalDateTime.now());
            log.info("Trade {} successfully matched", ourTrade.getOurTradeReference());
        } else {
            ourTrade.setStatus(MatchStatus.MISMATCHED);
            ourTrade.setMismatchReason(String.join("; ", mismatches));
            log.warn("Trade {} has mismatches: {}", ourTrade.getOurTradeReference(), ourTrade.getMismatchReason());
        }
        return ourTrade;
    }

    public TradeMatch affirmTrade(String tradeReference) {
        TradeMatch match = pendingMatches.get(tradeReference);
        if (match == null) {
            throw new IllegalArgumentException("Trade not found: " + tradeReference);
        }
        if (match.getStatus() != MatchStatus.MATCHED) {
            throw new IllegalStateException("Cannot affirm trade in status: " + match.getStatus());
        }
        match.setStatus(MatchStatus.AFFIRMED);
        log.info("Trade {} affirmed", tradeReference);
        return match;
    }

    private TradeMatch findCounterpartyMatch(TradeMatch ourTrade) {
        return pendingMatches.values().stream()
                .filter(m -> m.getCounterparty().equals(ourTrade.getCounterparty())
                        && m.getIsin().equals(ourTrade.getIsin())
                        && !m.getOurTradeReference().equals(ourTrade.getOurTradeReference()))
                .findFirst()
                .orElse(null);
    }

    private String buildMatchKey(TradeMatch trade) {
        return trade.getIsin() + ":" + trade.getCounterparty() + ":" + trade.getTradeDate().toLocalDate();
    }
}
