package com.itms.portfolio.service;

import com.itms.portfolio.model.Position;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class PositionService {

    private final RedisTemplate<String, Object> redisTemplate;

    // In-memory position store (backed by Redis in production)
    private final Map<String, Map<String, Position>> portfolioPositions = new ConcurrentHashMap<>();

    private static final String POSITION_CACHE_PREFIX = "position:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public Position updatePosition(String portfolio, String instrument, String isin,
                                    String currency, BigDecimal quantity, BigDecimal tradePrice,
                                    boolean isBuy) {
        String key = portfolio + ":" + isin;
        Map<String, Position> positions = portfolioPositions.computeIfAbsent(portfolio, p -> new ConcurrentHashMap<>());

        Position existing = positions.get(isin);
        if (existing == null) {
            existing = Position.builder()
                    .portfolio(portfolio)
                    .instrument(instrument)
                    .isin(isin)
                    .currency(currency)
                    .quantity(BigDecimal.ZERO)
                    .averageCost(BigDecimal.ZERO)
                    .currentPrice(tradePrice)
                    .marketValue(BigDecimal.ZERO)
                    .unrealizedPnl(BigDecimal.ZERO)
                    .realizedPnl(BigDecimal.ZERO)
                    .build();
        }

        if (isBuy) {
            BigDecimal newQuantity = existing.getQuantity().add(quantity);
            BigDecimal totalCost = existing.getAverageCost().multiply(existing.getQuantity())
                    .add(tradePrice.multiply(quantity));
            BigDecimal newAvgCost = newQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(newQuantity, 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            existing.setQuantity(newQuantity);
            existing.setAverageCost(newAvgCost);
        } else {
            BigDecimal realizedPnl = quantity.multiply(tradePrice.subtract(existing.getAverageCost()));
            existing.setRealizedPnl(existing.getRealizedPnl().add(realizedPnl));
            existing.setQuantity(existing.getQuantity().subtract(quantity));
        }

        existing.setCurrentPrice(tradePrice);
        positions.put(isin, existing);

        cachePosition(key, existing);
        log.info("Updated position for {}/{}: qty={}, avgCost={}", portfolio, instrument,
                existing.getQuantity(), existing.getAverageCost());
        return existing;
    }

    public List<Position> getPortfolioPositions(String portfolio) {
        Map<String, Position> positions = portfolioPositions.get(portfolio);
        return positions != null ? new ArrayList<>(positions.values()) : new ArrayList<>();
    }

    public BigDecimal getPortfolioMarketValue(String portfolio) {
        return getPortfolioPositions(portfolio).stream()
                .map(p -> p.getQuantity().multiply(p.getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPortfolioTotalPnl(String portfolio) {
        return getPortfolioPositions(portfolio).stream()
                .map(Position::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void cachePosition(String key, Position position) {
        try {
            redisTemplate.opsForValue().set(POSITION_CACHE_PREFIX + key, position, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache position {}: {}", key, e.getMessage());
        }
    }
}
