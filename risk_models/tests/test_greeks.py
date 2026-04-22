"""Tests for Greeks Calculator."""
import numpy as np
import pytest
from risk_models.greeks_calculator import (
    calculate_greeks, black_scholes_price, implied_volatility,
    dollar_delta, portfolio_delta
)


class TestBlackScholesPrice:
    def test_atm_call(self):
        """ATM option has well-known approximate price."""
        S, K, r, sigma, T = 100.0, 100.0, 0.05, 0.20, 1.0
        price = black_scholes_price(S, K, r, sigma, T, "call")
        assert 8 < price < 12  # Rule of thumb: ~0.4 * sigma * sqrt(T) * S

    def test_put_call_parity(self):
        """Verify put-call parity: C - P = S - K*exp(-r*T)"""
        S, K, r, sigma, T = 100.0, 100.0, 0.05, 0.20, 1.0
        call = black_scholes_price(S, K, r, sigma, T, "call")
        put = black_scholes_price(S, K, r, sigma, T, "put")
        parity = call - put - S + K * np.exp(-r * T)
        assert abs(parity) < 1e-8

    def test_deep_itm_call(self):
        """Deep ITM call is approximately S - K*exp(-r*T)."""
        S, K, r, sigma, T = 200.0, 100.0, 0.05, 0.20, 1.0
        call = black_scholes_price(S, K, r, sigma, T, "call")
        expected = S - K * np.exp(-r * T)
        assert abs(call - expected) < 1.0

    def test_invalid_option_type(self):
        with pytest.raises(ValueError):
            black_scholes_price(100, 100, 0.05, 0.20, 1.0, "forward")

    def test_invalid_expiry(self):
        with pytest.raises(ValueError):
            calculate_greeks(100, 100, 0.05, 0.20, 0.0, "call")


class TestGreeks:
    def setup_method(self):
        self.S = 100.0
        self.K = 100.0
        self.r = 0.05
        self.sigma = 0.20
        self.T = 1.0

    def test_call_delta_range(self):
        """Call delta must be between 0 and 1."""
        g = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "call")
        assert 0 < g.delta < 1

    def test_put_delta_range(self):
        """Put delta must be between -1 and 0."""
        g = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "put")
        assert -1 < g.delta < 0

    def test_gamma_positive(self):
        """Gamma is always positive for long options."""
        g = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "call")
        assert g.gamma > 0

    def test_call_put_same_gamma(self):
        """Call and put with same params have identical gamma."""
        g_call = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "call")
        g_put = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "put")
        assert abs(g_call.gamma - g_put.gamma) < 1e-10

    def test_call_put_same_vega(self):
        """Call and put with same params have identical vega."""
        g_call = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "call")
        g_put = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "put")
        assert abs(g_call.vega - g_put.vega) < 1e-10

    def test_theta_negative(self):
        """Theta (time decay) is negative for long options."""
        g = calculate_greeks(self.S, self.K, self.r, self.sigma, self.T, "call")
        assert g.theta < 0

    def test_atm_delta_near_half(self):
        """ATM call delta is approximately 0.5."""
        g = calculate_greeks(self.S, self.K, 0, self.sigma, self.T, "call")
        assert abs(g.delta - 0.5) < 0.05

    def test_intrinsic_value(self):
        """Deep ITM call has intrinsic value."""
        g = calculate_greeks(200.0, 100.0, self.r, self.sigma, self.T, "call")
        assert g.intrinsic_value == pytest.approx(100.0)


class TestImpliedVolatility:
    def test_round_trip(self):
        """IV calculation should recover input volatility."""
        S, K, r, sigma, T = 100.0, 100.0, 0.05, 0.25, 0.5
        price = black_scholes_price(S, K, r, sigma, T, "call")
        iv = implied_volatility(price, S, K, r, T, "call")
        assert abs(iv - sigma) < 1e-4

    def test_iv_put_round_trip(self):
        S, K, r, sigma, T = 100.0, 110.0, 0.03, 0.30, 0.25
        price = black_scholes_price(S, K, r, sigma, T, "put")
        iv = implied_volatility(price, S, K, r, T, "put")
        assert abs(iv - sigma) < 1e-4


class TestPortfolioGreeks:
    def test_portfolio_delta(self):
        g1 = calculate_greeks(100, 100, 0.05, 0.20, 1.0, "call")
        g2 = calculate_greeks(100, 100, 0.05, 0.20, 1.0, "put")
        positions = [
            {"quantity": 100, "greeks": g1},
            {"quantity": 50, "greeks": g2},
        ]
        agg_delta = portfolio_delta(positions)
        expected = 100 * g1.delta + 50 * g2.delta
        assert abs(agg_delta - expected) < 1e-10
