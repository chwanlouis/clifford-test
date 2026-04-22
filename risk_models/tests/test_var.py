"""Tests for VaR Calculator."""
import numpy as np
import pytest
from risk_models.var_calculator import (
    historical_var, historical_cvar, parametric_var,
    monte_carlo_var, portfolio_var, stress_test_var
)


class TestHistoricalVaR:
    def test_basic_var(self):
        np.random.seed(42)
        returns = np.random.normal(0, 0.01, 1000)
        var = historical_var(returns, 0.99)
        assert var > 0

    def test_higher_confidence_gives_higher_var(self):
        np.random.seed(42)
        returns = np.random.normal(0, 0.01, 1000)
        var_95 = historical_var(returns, 0.95)
        var_99 = historical_var(returns, 0.99)
        assert var_99 >= var_95

    def test_empty_returns_raises(self):
        with pytest.raises(ValueError):
            historical_var(np.array([]), 0.99)

    def test_cvar_gte_var(self):
        np.random.seed(42)
        returns = np.random.normal(0, 0.01, 1000)
        var = historical_var(returns, 0.99)
        cvar = historical_cvar(returns, 0.99)
        assert cvar >= var


class TestParametricVaR:
    def test_basic_parametric_var(self):
        pv = 1_000_000
        vol = 0.01
        var = parametric_var(pv, vol, 0.99)
        assert var > 0
        # Expected: PV * z_99 * vol = 1M * 2.326 * 0.01 = ~23,260
        assert 20000 < var < 30000

    def test_scaling_with_holding_period(self):
        pv = 1_000_000
        vol = 0.01
        var_1d = parametric_var(pv, vol, 0.99, 1)
        var_10d = parametric_var(pv, vol, 0.99, 10)
        # sqrt-of-time rule: 10d VaR ≈ 1d VaR * sqrt(10)
        ratio = var_10d / var_1d
        assert abs(ratio - np.sqrt(10)) < 0.01

    def test_higher_volatility_gives_higher_var(self):
        pv = 1_000_000
        var_low = parametric_var(pv, 0.01, 0.99)
        var_high = parametric_var(pv, 0.02, 0.99)
        assert var_high > var_low


class TestMonteCarloVaR:
    def test_basic_mc_var(self):
        pv = 1_000_000
        var, cvar = monte_carlo_var(pv, 0.05, 0.15, 0.99, 10000, seed=42)
        assert var > 0
        assert cvar >= var

    def test_reproducible_with_seed(self):
        pv = 1_000_000
        var1, _ = monte_carlo_var(pv, 0.05, 0.15, 0.99, 10000, seed=42)
        var2, _ = monte_carlo_var(pv, 0.05, 0.15, 0.99, 10000, seed=42)
        assert var1 == var2


class TestPortfolioVaR:
    def test_two_asset_portfolio(self):
        weights = np.array([0.6, 0.4])
        # Diagonal covariance (uncorrelated)
        cov = np.array([[0.01**2, 0], [0, 0.02**2]])
        pv = 1_000_000
        var = portfolio_var(weights, cov, pv, 0.99)
        assert var > 0


class TestStressTest:
    def test_stress_scenarios(self):
        pv = 10_000_000
        scenarios = {"2008 Crisis": -0.40, "COVID": -0.30}
        results = stress_test_var(pv, scenarios)
        assert results["2008 Crisis"] == pv * 0.40
        assert results["COVID"] == pv * 0.30
