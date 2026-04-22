package com.itms.corporateactions.service;

import com.itms.corporateactions.model.CorporateAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StockSplitService {

    public Map<String, BigDecimal> processStockSplit(CorporateAction event,
                                                      Map<String, BigDecimal> positionsByPortfolio) {
        log.info("Processing stock split: {} for ISIN: {} ({}:{})",
                event.getEventReference(), event.getIsin(),
                event.getSplitRatioNew(), event.getSplitRatioOld());

        BigDecimal splitFactor = event.getSplitRatioNew().divide(event.getSplitRatioOld(), 10, RoundingMode.HALF_UP);
        Map<String, BigDecimal> adjustedPositions = new HashMap<>();

        positionsByPortfolio.forEach((portfolio, quantity) -> {
            BigDecimal newQuantity = quantity.multiply(splitFactor).setScale(0, RoundingMode.DOWN);
            adjustedPositions.put(portfolio, newQuantity);
            log.info("Stock split for portfolio {}: {} -> {} shares (factor: {})",
                    portfolio, quantity, newQuantity, splitFactor);
        });

        return adjustedPositions;
    }

    public BigDecimal adjustPriceForSplit(BigDecimal originalPrice, BigDecimal splitRatioNew,
                                           BigDecimal splitRatioOld) {
        BigDecimal splitFactor = splitRatioNew.divide(splitRatioOld, 10, RoundingMode.HALF_UP);
        return originalPrice.divide(splitFactor, 4, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> processReverseSplit(CorporateAction event,
                                                        Map<String, BigDecimal> positionsByPortfolio) {
        log.info("Processing reverse split: {} for ISIN: {}", event.getEventReference(), event.getIsin());

        BigDecimal reverseFactor = event.getSplitRatioOld().divide(event.getSplitRatioNew(), 10, RoundingMode.HALF_UP);
        Map<String, BigDecimal> adjustedPositions = new HashMap<>();

        positionsByPortfolio.forEach((portfolio, quantity) -> {
            BigDecimal newQuantity = quantity.divide(reverseFactor, 0, RoundingMode.DOWN);
            // Cash-in-lieu for fractional shares
            BigDecimal fractional = quantity.subtract(newQuantity.multiply(reverseFactor));
            adjustedPositions.put(portfolio, newQuantity);
            if (fractional.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Cash-in-lieu for {} fractional shares in portfolio {}", fractional, portfolio);
            }
        });

        return adjustedPositions;
    }
}
