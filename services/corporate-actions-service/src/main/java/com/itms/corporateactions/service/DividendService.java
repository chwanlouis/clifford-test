package com.itms.corporateactions.service;

import com.itms.corporateactions.model.CorporateAction;
import com.itms.corporateactions.model.CorporateActionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DividendService {

    public Map<String, BigDecimal> processDividend(CorporateAction event,
                                                    Map<String, BigDecimal> positionsByPortfolio) {
        log.info("Processing dividend event: {} for ISIN: {}", event.getEventReference(), event.getIsin());

        Map<String, BigDecimal> dividendPayments = new HashMap<>();

        positionsByPortfolio.forEach((portfolio, quantity) -> {
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal grossAmount = quantity.multiply(event.getDividendPerShare())
                        .setScale(2, RoundingMode.HALF_UP);
                // Apply withholding tax (simplified 15%)
                BigDecimal withholdingTax = grossAmount.multiply(new BigDecimal("0.15"))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal netAmount = grossAmount.subtract(withholdingTax);
                dividendPayments.put(portfolio, netAmount);
                log.info("Dividend for portfolio {}: gross={}, net={} {}",
                        portfolio, grossAmount, netAmount, event.getDividendCurrency());
            }
        });

        return dividendPayments;
    }

    public BigDecimal calculateDividendAmount(BigDecimal quantity, BigDecimal dividendPerShare) {
        return quantity.multiply(dividendPerShare).setScale(2, RoundingMode.HALF_UP);
    }
}
