package com.itms.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Greeks {
    private String instrument;
    private String portfolio;
    private BigDecimal delta;    // dV/dS - sensitivity to underlying price
    private BigDecimal gamma;    // d²V/dS² - rate of change of delta
    private BigDecimal theta;    // dV/dt - time decay
    private BigDecimal vega;     // dV/dσ - sensitivity to volatility
    private BigDecimal rho;      // dV/dr - sensitivity to interest rate
    private LocalDate calculationDate;
    private BigDecimal underlyingPrice;
    private BigDecimal impliedVolatility;
    private BigDecimal riskFreeRate;
    private BigDecimal timeToExpiry;
}
