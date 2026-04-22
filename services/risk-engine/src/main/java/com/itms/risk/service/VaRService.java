package com.itms.risk.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class VaRService {

    private static final MathContext MC = MathContext.DECIMAL64;

    /**
     * Historical VaR calculation at given confidence level.
     * @param returns list of historical daily P&L returns
     * @param confidenceLevel e.g. 0.99 for 99% VaR
     * @return VaR estimate (positive number representing potential loss)
     */
    public BigDecimal calculateHistoricalVaR(List<BigDecimal> returns, double confidenceLevel) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }
        log.info("Calculating Historical VaR at {}% confidence level over {} observations",
                confidenceLevel * 100, returns.size());

        double[] sortedReturns = returns.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .sorted()
                .toArray();

        int cutoffIndex = (int) Math.floor((1 - confidenceLevel) * sortedReturns.length);
        double var = -sortedReturns[Math.max(0, cutoffIndex)];
        log.info("Historical VaR: {}", var);
        return BigDecimal.valueOf(var).round(MC);
    }

    /**
     * Parametric (Variance-Covariance) VaR.
     * @param portfolioValue current portfolio value
     * @param dailyVolatility portfolio daily return volatility (std dev)
     * @param confidenceLevel e.g. 0.99
     * @param holdingPeriodDays number of days to scale VaR
     */
    public BigDecimal calculateParametricVaR(BigDecimal portfolioValue, BigDecimal dailyVolatility,
                                              double confidenceLevel, int holdingPeriodDays) {
        log.info("Calculating Parametric VaR: portfolioValue={}, vol={}, conf={}, days={}",
                portfolioValue, dailyVolatility, confidenceLevel, holdingPeriodDays);

        double zScore = getZScore(confidenceLevel);
        double vol = dailyVolatility.doubleValue();
        double pv = portfolioValue.doubleValue();
        double scaledVol = vol * Math.sqrt(holdingPeriodDays);
        double var = pv * zScore * scaledVol;

        log.info("Parametric VaR ({}d, {}% CI): {}", holdingPeriodDays, confidenceLevel * 100, var);
        return BigDecimal.valueOf(var).round(MC);
    }

    /**
     * Monte Carlo VaR simulation.
     * @param portfolioValue current portfolio value
     * @param annualReturn expected annual return
     * @param annualVolatility annualized volatility
     * @param confidenceLevel confidence level
     * @param numSimulations number of Monte Carlo paths
     */
    public BigDecimal calculateMonteCarloVaR(BigDecimal portfolioValue, BigDecimal annualReturn,
                                              BigDecimal annualVolatility, double confidenceLevel,
                                              int numSimulations) {
        log.info("Running Monte Carlo VaR with {} simulations", numSimulations);
        java.util.Random random = new java.util.Random(42);
        double pv = portfolioValue.doubleValue();
        double mu = annualReturn.doubleValue() / 252;
        double sigma = annualVolatility.doubleValue() / Math.sqrt(252);

        double[] simulatedPnLs = new double[numSimulations];
        for (int i = 0; i < numSimulations; i++) {
            double z = random.nextGaussian();
            double returnSim = mu + sigma * z;
            simulatedPnLs[i] = pv * returnSim;
        }

        Arrays.sort(simulatedPnLs);
        int cutoffIndex = (int) Math.floor((1 - confidenceLevel) * numSimulations);
        double var = -simulatedPnLs[Math.max(0, cutoffIndex)];
        log.info("Monte Carlo VaR: {}", var);
        return BigDecimal.valueOf(var).round(MC);
    }

    private double getZScore(double confidenceLevel) {
        // Common z-scores for standard normal distribution
        if (confidenceLevel >= 0.999) return 3.090;
        if (confidenceLevel >= 0.995) return 2.576;
        if (confidenceLevel >= 0.99) return 2.326;
        if (confidenceLevel >= 0.975) return 1.960;
        if (confidenceLevel >= 0.95) return 1.645;
        return 1.282; // 90%
    }
}
