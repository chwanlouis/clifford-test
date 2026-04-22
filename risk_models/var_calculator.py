"""
Value-at-Risk (VaR) Calculator for ITMS Risk Engine.
Supports Historical, Parametric (Variance-Covariance), and Monte Carlo methods.
"""

import numpy as np
import pandas as pd
from scipy import stats
from typing import List, Optional, Tuple
import logging

logger = logging.getLogger(__name__)


def historical_var(returns: np.ndarray, confidence_level: float = 0.99) -> float:
    """
    Calculate Historical VaR from a series of P&L returns.

    Args:
        returns: Array of historical daily returns (as decimals, e.g. 0.01 = 1%)
        confidence_level: Confidence level (e.g. 0.99 for 99%)

    Returns:
        VaR as a positive number (representing potential loss)
    """
    if len(returns) == 0:
        raise ValueError("Returns array is empty")

    sorted_returns = np.sort(returns)
    cutoff_index = int(np.floor((1 - confidence_level) * len(sorted_returns)))
    var = -sorted_returns[max(0, cutoff_index)]
    logger.info(f"Historical VaR ({confidence_level*100:.1f}% CI): {var:.6f}")
    return float(var)


def historical_cvar(returns: np.ndarray, confidence_level: float = 0.99) -> float:
    """
    Calculate Historical Conditional VaR (Expected Shortfall / CVaR).
    CVaR is the expected loss given that the loss exceeds VaR.

    Args:
        returns: Array of historical daily returns
        confidence_level: Confidence level

    Returns:
        CVaR as a positive number
    """
    if len(returns) == 0:
        raise ValueError("Returns array is empty")

    sorted_returns = np.sort(returns)
    cutoff_index = int(np.floor((1 - confidence_level) * len(sorted_returns)))
    tail_returns = sorted_returns[:max(1, cutoff_index)]
    cvar = -np.mean(tail_returns)
    logger.info(f"Historical CVaR ({confidence_level*100:.1f}% CI): {cvar:.6f}")
    return float(cvar)


def parametric_var(portfolio_value: float, daily_volatility: float,
                   confidence_level: float = 0.99, holding_period_days: int = 1) -> float:
    """
    Parametric (Variance-Covariance) VaR calculation.
    Assumes returns are normally distributed.

    Args:
        portfolio_value: Current portfolio value (in base currency)
        daily_volatility: Daily return standard deviation (as decimal)
        confidence_level: Confidence level
        holding_period_days: Holding period to scale VaR (sqrt-of-time rule)

    Returns:
        VaR as a positive dollar/currency amount
    """
    z_score = stats.norm.ppf(confidence_level)
    scaled_vol = daily_volatility * np.sqrt(holding_period_days)
    var = portfolio_value * z_score * scaled_vol
    logger.info(
        f"Parametric VaR ({confidence_level*100:.1f}% CI, {holding_period_days}d): "
        f"{var:.2f} (vol={daily_volatility:.4f}, z={z_score:.4f})"
    )
    return float(var)


def portfolio_var(weights: np.ndarray, covariance_matrix: np.ndarray,
                  portfolio_value: float, confidence_level: float = 0.99,
                  holding_period_days: int = 1) -> float:
    """
    Multi-asset portfolio parametric VaR using covariance matrix.

    Args:
        weights: Portfolio weights (must sum to ~1)
        covariance_matrix: N x N daily return covariance matrix
        portfolio_value: Current portfolio value
        confidence_level: Confidence level
        holding_period_days: Holding period (days)

    Returns:
        Portfolio VaR
    """
    portfolio_variance = weights @ covariance_matrix @ weights
    portfolio_vol = np.sqrt(portfolio_variance)
    return parametric_var(portfolio_value, portfolio_vol, confidence_level, holding_period_days)


def monte_carlo_var(portfolio_value: float, annual_return: float,
                    annual_volatility: float, confidence_level: float = 0.99,
                    num_simulations: int = 10000, holding_period_days: int = 1,
                    seed: Optional[int] = None) -> Tuple[float, float]:
    """
    Monte Carlo VaR simulation.

    Args:
        portfolio_value: Current portfolio value
        annual_return: Expected annual return (as decimal)
        annual_volatility: Annual volatility (as decimal)
        confidence_level: Confidence level
        num_simulations: Number of Monte Carlo paths
        holding_period_days: Holding period (days)
        seed: Random seed for reproducibility

    Returns:
        Tuple of (VaR, CVaR) as positive numbers
    """
    rng = np.random.default_rng(seed)
    daily_return = annual_return / 252
    daily_vol = annual_volatility / np.sqrt(252)

    # Simulate holding period returns
    if holding_period_days == 1:
        z = rng.standard_normal(num_simulations)
        sim_returns = daily_return + daily_vol * z
    else:
        # Multi-period simulation
        z = rng.standard_normal((num_simulations, holding_period_days))
        daily_rets = daily_return + daily_vol * z
        sim_returns = np.prod(1 + daily_rets, axis=1) - 1

    sim_pnl = portfolio_value * sim_returns
    sorted_pnl = np.sort(sim_pnl)

    cutoff_index = int(np.floor((1 - confidence_level) * num_simulations))
    var = -sorted_pnl[max(0, cutoff_index)]
    cvar = -np.mean(sorted_pnl[:max(1, cutoff_index)])

    logger.info(
        f"Monte Carlo VaR ({num_simulations} sims, {confidence_level*100:.1f}% CI): "
        f"VaR={var:.2f}, CVaR={cvar:.2f}"
    )
    return float(var), float(cvar)


def rolling_var(returns: pd.Series, window: int = 252,
                confidence_level: float = 0.99) -> pd.Series:
    """
    Calculate rolling Historical VaR over a time series.

    Args:
        returns: Pandas Series of daily returns
        window: Rolling window size (default: 1 year = 252 trading days)
        confidence_level: Confidence level

    Returns:
        Pandas Series of rolling VaR values
    """
    def _var(x):
        return historical_var(x.values, confidence_level)

    return returns.rolling(window).apply(_var, raw=False)


def stress_test_var(portfolio_value: float, scenarios: dict) -> dict:
    """
    Apply predefined stress scenarios to estimate losses.

    Args:
        portfolio_value: Current portfolio value
        scenarios: Dict of {scenario_name: portfolio_return_shock}

    Returns:
        Dict of {scenario_name: loss_amount}
    """
    results = {}
    for scenario, shock in scenarios.items():
        loss = portfolio_value * abs(shock)
        results[scenario] = loss
        logger.info(f"Stress scenario '{scenario}' (shock={shock:.2%}): loss={loss:.2f}")
    return results


# Standard stress scenarios based on historical events
STANDARD_SCENARIOS = {
    "Black Monday 1987": -0.228,
    "LTCM Crisis 1998": -0.098,
    "Dot-com Crash 2000-2002": -0.49,
    "Global Financial Crisis 2008": -0.507,
    "COVID-19 Crash 2020": -0.34,
    "2σ Shock": -2 * 0.01,       # 2 standard deviation shock (assuming 1% daily vol)
    "3σ Shock": -3 * 0.01,
}


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # Example usage
    np.random.seed(42)
    sample_returns = np.random.normal(0.0005, 0.015, 252)

    print("=" * 60)
    print("ITMS Risk Engine - VaR Calculator")
    print("=" * 60)

    pv = 10_000_000  # $10M portfolio

    hist_var = historical_var(sample_returns, 0.99)
    print(f"Historical VaR (99%): ${hist_var * pv:,.2f}")

    hist_cvar = historical_cvar(sample_returns, 0.99)
    print(f"Historical CVaR (99%): ${hist_cvar * pv:,.2f}")

    param_var = parametric_var(pv, 0.015, 0.99, 1)
    print(f"Parametric VaR (99%, 1-day): ${param_var:,.2f}")

    param_var_10d = parametric_var(pv, 0.015, 0.99, 10)
    print(f"Parametric VaR (99%, 10-day): ${param_var_10d:,.2f}")

    mc_var, mc_cvar = monte_carlo_var(pv, 0.08, 0.20, 0.99, 100000, seed=42)
    print(f"Monte Carlo VaR (99%, 100k sims): ${mc_var:,.2f}")
    print(f"Monte Carlo CVaR (99%, 100k sims): ${mc_cvar:,.2f}")

    stress = stress_test_var(pv, STANDARD_SCENARIOS)
    print("\nStress Test Results:")
    for scenario, loss in stress.items():
        print(f"  {scenario}: ${loss:,.2f}")
