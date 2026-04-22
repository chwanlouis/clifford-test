package com.itms.tradecapture.repository;

import com.itms.tradecapture.model.Trade;
import com.itms.tradecapture.model.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Optional<Trade> findByTradeReference(String tradeReference);

    List<Trade> findByStatus(TradeStatus status);

    List<Trade> findByPortfolio(String portfolio);

    List<Trade> findByTrader(String trader);

    List<Trade> findByCounterparty(String counterparty);

    List<Trade> findByTradeDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT t FROM Trade t WHERE t.portfolio = :portfolio AND t.status NOT IN ('CANCELLED', 'FAILED')")
    List<Trade> findActiveTradesByPortfolio(String portfolio);

    @Query("SELECT t FROM Trade t WHERE t.status = 'PENDING_VALIDATION' ORDER BY t.createdAt ASC")
    List<Trade> findPendingValidationTrades();
}
