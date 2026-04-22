package com.itms.portfolio.service;

import com.itms.portfolio.model.Greeks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

@Service
@Slf4j
public class GreeksService {

    private static final MathContext MC = MathContext.DECIMAL64;

    public Greeks calculateBlackScholesGreeks(
            String instrument,
            String portfolio,
            BigDecimal spotPrice,
            BigDecimal strikePrice,
            BigDecimal riskFreeRate,
            BigDecimal volatility,
            BigDecimal timeToExpiry,
            boolean isCall) {

        log.info("Calculating Greeks for instrument: {} in portfolio: {}", instrument, portfolio);

        double S = spotPrice.doubleValue();
        double K = strikePrice.doubleValue();
        double r = riskFreeRate.doubleValue();
        double sigma = volatility.doubleValue();
        double T = timeToExpiry.doubleValue();

        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);

        double delta = isCall ? normalCdf(d1) : normalCdf(d1) - 1.0;
        double gamma = normalPdf(d1) / (S * sigma * Math.sqrt(T));
        double vega = S * normalPdf(d1) * Math.sqrt(T) / 100.0;
        double theta = isCall
                ? (-(S * normalPdf(d1) * sigma) / (2.0 * Math.sqrt(T)) - r * K * Math.exp(-r * T) * normalCdf(d2)) / 365.0
                : (-(S * normalPdf(d1) * sigma) / (2.0 * Math.sqrt(T)) + r * K * Math.exp(-r * T) * normalCdf(-d2)) / 365.0;
        double rho = isCall
                ? K * T * Math.exp(-r * T) * normalCdf(d2) / 100.0
                : -K * T * Math.exp(-r * T) * normalCdf(-d2) / 100.0;

        return Greeks.builder()
                .instrument(instrument)
                .portfolio(portfolio)
                .delta(BigDecimal.valueOf(delta).round(MC))
                .gamma(BigDecimal.valueOf(gamma).round(MC))
                .theta(BigDecimal.valueOf(theta).round(MC))
                .vega(BigDecimal.valueOf(vega).round(MC))
                .rho(BigDecimal.valueOf(rho).round(MC))
                .calculationDate(LocalDate.now())
                .underlyingPrice(spotPrice)
                .impliedVolatility(volatility)
                .riskFreeRate(riskFreeRate)
                .timeToExpiry(timeToExpiry)
                .build();
    }

    private double normalCdf(double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    private double normalPdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2.0 * Math.PI);
    }

    private double erf(double x) {
        // Abramowitz and Stegun approximation
        double a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741;
        double a4 = -1.453152027, a5 = 1.061405429, p = 0.3275911;
        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x);
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        return sign * y;
    }
}
