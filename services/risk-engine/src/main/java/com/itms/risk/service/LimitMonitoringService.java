package com.itms.risk.service;

import com.itms.risk.model.LimitType;
import com.itms.risk.model.RiskCheckResult;
import com.itms.risk.model.RiskLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LimitMonitoringService {

    // In production: loaded from DB and cached
    private final Map<String, RiskLimit> limitsCache = new ConcurrentHashMap<>();

    public RiskCheckResult checkLimit(String portfolio, String counterparty,
                                       LimitType limitType, BigDecimal proposedExposure,
                                       BigDecimal currentExposure, String currency) {
        String key = buildKey(portfolio, counterparty, limitType);
        RiskLimit limit = limitsCache.get(key);

        if (limit == null) {
            log.warn("No limit defined for key: {}", key);
            return RiskCheckResult.builder()
                    .portfolio(portfolio)
                    .counterparty(counterparty)
                    .limitType(limitType)
                    .currentExposure(proposedExposure)
                    .limitAmount(BigDecimal.valueOf(Long.MAX_VALUE))
                    .utilizationPercent(BigDecimal.ZERO)
                    .breached(false)
                    .message("No limit defined - trade allowed")
                    .checkedAt(LocalDateTime.now())
                    .build();
        }

        BigDecimal totalExposure = currentExposure.add(proposedExposure);
        BigDecimal utilization = totalExposure.divide(limit.getLimitAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        boolean breached = totalExposure.compareTo(limit.getLimitAmount()) > 0;

        if (breached) {
            log.warn("LIMIT BREACH: portfolio={}, counterparty={}, type={}, exposure={}, limit={}",
                    portfolio, counterparty, limitType, totalExposure, limit.getLimitAmount());
        }

        return RiskCheckResult.builder()
                .portfolio(portfolio)
                .counterparty(counterparty)
                .limitType(limitType)
                .currentExposure(totalExposure)
                .limitAmount(limit.getLimitAmount())
                .utilizationPercent(utilization)
                .breached(breached)
                .message(breached ? "LIMIT BREACHED" : "Within limits (" + utilization.setScale(2, RoundingMode.HALF_UP) + "% utilized)")
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public void setLimit(RiskLimit limit) {
        String key = buildKey(limit.getPortfolio(), limit.getCounterparty(), limit.getLimitType());
        limitsCache.put(key, limit);
        log.info("Limit set: {}", key);
    }

    public List<RiskCheckResult> getAllLimitUtilizations() {
        List<RiskCheckResult> results = new ArrayList<>();
        limitsCache.forEach((key, limit) -> {
            BigDecimal utilization = BigDecimal.ZERO; // In production: fetch from exposure service
            results.add(RiskCheckResult.builder()
                    .portfolio(limit.getPortfolio())
                    .counterparty(limit.getCounterparty())
                    .limitType(limit.getLimitType())
                    .limitAmount(limit.getLimitAmount())
                    .utilizationPercent(utilization)
                    .breached(false)
                    .checkedAt(LocalDateTime.now())
                    .build());
        });
        return results;
    }

    private String buildKey(String portfolio, String counterparty, LimitType limitType) {
        return portfolio + ":" + counterparty + ":" + limitType.name();
    }
}
