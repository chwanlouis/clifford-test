"""
Options Greeks Calculator for ITMS Portfolio Service.
Implements Black-Scholes model for European options.
"""

import numpy as np
from scipy import stats
from dataclasses import dataclass
from typing import Optional
import logging

logger = logging.getLogger(__name__)


@dataclass
class OptionGreeks:
    """Container for Black-Scholes option Greeks."""
    delta: float    # dV/dS
    gamma: float    # d²V/dS²
    theta: float    # dV/dt (per day)
    vega: float     # dV/dσ (per 1% change in vol)
    rho: float      # dV/dr (per 1% change in rate)
    option_price: float
    intrinsic_value: float
    time_value: float
    implied_volatility: Optional[float] = None


def _d1_d2(S: float, K: float, r: float, sigma: float, T: float):
    """Compute d1 and d2 for Black-Scholes formula."""
    if T <= 0:
        raise ValueError("Time to expiry must be positive")
    if sigma <= 0:
        raise ValueError("Volatility must be positive")
    d1 = (np.log(S / K) + (r + 0.5 * sigma ** 2) * T) / (sigma * np.sqrt(T))
    d2 = d1 - sigma * np.sqrt(T)
    return d1, d2


def black_scholes_price(S: float, K: float, r: float, sigma: float, T: float,
                         option_type: str = "call") -> float:
    """
    Black-Scholes option price for European options.

    Args:
        S: Current underlying price
        K: Strike price
        r: Risk-free rate (annual, as decimal)
        sigma: Volatility (annual, as decimal)
        T: Time to expiry (in years)
        option_type: "call" or "put"

    Returns:
        Option price
    """
    d1, d2 = _d1_d2(S, K, r, sigma, T)
    if option_type.lower() == "call":
        price = S * stats.norm.cdf(d1) - K * np.exp(-r * T) * stats.norm.cdf(d2)
    elif option_type.lower() == "put":
        price = K * np.exp(-r * T) * stats.norm.cdf(-d2) - S * stats.norm.cdf(-d1)
    else:
        raise ValueError(f"Invalid option type: {option_type}. Use 'call' or 'put'")
    return float(price)


def calculate_greeks(S: float, K: float, r: float, sigma: float, T: float,
                      option_type: str = "call") -> OptionGreeks:
    """
    Calculate all Black-Scholes Greeks for a European option.

    Args:
        S: Current underlying price
        K: Strike price
        r: Risk-free rate (annual, as decimal)
        sigma: Volatility (annual, as decimal)
        T: Time to expiry (in years)
        option_type: "call" or "put"

    Returns:
        OptionGreeks dataclass with all Greeks
    """
    d1, d2 = _d1_d2(S, K, r, sigma, T)
    N = stats.norm.cdf
    n = stats.norm.pdf

    # Option price
    if option_type.lower() == "call":
        price = S * N(d1) - K * np.exp(-r * T) * N(d2)
        delta = N(d1)
        rho = K * T * np.exp(-r * T) * N(d2) / 100.0
        theta = (-(S * n(d1) * sigma) / (2.0 * np.sqrt(T))
                 - r * K * np.exp(-r * T) * N(d2)) / 365.0
    else:
        price = K * np.exp(-r * T) * N(-d2) - S * N(-d1)
        delta = N(d1) - 1.0
        rho = -K * T * np.exp(-r * T) * N(-d2) / 100.0
        theta = (-(S * n(d1) * sigma) / (2.0 * np.sqrt(T))
                 + r * K * np.exp(-r * T) * N(-d2)) / 365.0

    gamma = n(d1) / (S * sigma * np.sqrt(T))
    vega = S * n(d1) * np.sqrt(T) / 100.0  # Per 1% change in vol

    intrinsic = max(0.0, S - K) if option_type.lower() == "call" else max(0.0, K - S)
    time_value = price - intrinsic

    logger.debug(
        f"Greeks for {option_type} S={S}, K={K}, r={r:.3f}, σ={sigma:.3f}, T={T:.4f}: "
        f"Δ={delta:.4f}, Γ={gamma:.4f}, θ={theta:.4f}, ν={vega:.4f}, ρ={rho:.4f}"
    )

    return OptionGreeks(
        delta=float(delta),
        gamma=float(gamma),
        theta=float(theta),
        vega=float(vega),
        rho=float(rho),
        option_price=float(price),
        intrinsic_value=float(intrinsic),
        time_value=float(time_value),
    )


def implied_volatility(option_price: float, S: float, K: float, r: float, T: float,
                        option_type: str = "call", tolerance: float = 1e-6,
                        max_iterations: int = 100) -> float:
    """
    Calculate implied volatility using Newton-Raphson method.

    Args:
        option_price: Observed market price of option
        S: Current underlying price
        K: Strike price
        r: Risk-free rate
        T: Time to expiry
        option_type: "call" or "put"
        tolerance: Convergence tolerance
        max_iterations: Max Newton-Raphson iterations

    Returns:
        Implied volatility as a decimal (e.g. 0.25 = 25%)
    """
    sigma = 0.30  # Initial guess: 30% vol
    for i in range(max_iterations):
        price = black_scholes_price(S, K, r, sigma, T, option_type)
        greeks = calculate_greeks(S, K, r, sigma, T, option_type)
        vega_raw = greeks.vega * 100  # Convert back from per-1%

        if abs(vega_raw) < 1e-10:
            logger.warning("Vega too small for Newton-Raphson convergence")
            break

        diff = price - option_price
        if abs(diff) < tolerance:
            logger.debug(f"IV converged at σ={sigma:.6f} after {i+1} iterations")
            return float(sigma)

        sigma -= diff / vega_raw

        if sigma <= 0:
            sigma = 0.001

    return float(sigma)


def portfolio_delta(positions: list) -> float:
    """
    Calculate aggregate portfolio delta.

    Args:
        positions: List of dicts with keys: {quantity, greeks: OptionGreeks}

    Returns:
        Aggregate portfolio delta
    """
    return sum(pos["quantity"] * pos["greeks"].delta for pos in positions)


def portfolio_gamma(positions: list) -> float:
    """Calculate aggregate portfolio gamma."""
    return sum(pos["quantity"] * pos["greeks"].gamma for pos in positions)


def portfolio_vega(positions: list) -> float:
    """Calculate aggregate portfolio vega."""
    return sum(pos["quantity"] * pos["greeks"].vega for pos in positions)


def dollar_delta(S: float, quantity: float, delta: float) -> float:
    """Calculate dollar delta (position-weighted)."""
    return S * quantity * delta


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    print("=" * 60)
    print("ITMS Options Greeks Calculator")
    print("=" * 60)

    # AAPL example
    S = 175.0    # Spot price
    K = 180.0    # Strike
    r = 0.05     # 5% risk-free rate
    sigma = 0.25 # 25% implied vol
    T = 30/365   # 30 days to expiry

    call_greeks = calculate_greeks(S, K, r, sigma, T, "call")
    put_greeks = calculate_greeks(S, K, r, sigma, T, "put")

    print(f"\nUnderlying: ${S:.2f}, Strike: ${K:.2f}, r: {r:.2%}, σ: {sigma:.2%}, T: {T*365:.0f}d")
    print("\n--- CALL OPTION ---")
    print(f"  Price:    ${call_greeks.option_price:.4f}")
    print(f"  Delta:    {call_greeks.delta:.4f}")
    print(f"  Gamma:    {call_greeks.gamma:.6f}")
    print(f"  Theta:    {call_greeks.theta:.4f} ($/day)")
    print(f"  Vega:     {call_greeks.vega:.4f} ($/1% vol)")
    print(f"  Rho:      {call_greeks.rho:.4f} ($/1% rate)")
    print(f"  Intrinsic: ${call_greeks.intrinsic_value:.4f}")
    print(f"  Time Val: ${call_greeks.time_value:.4f}")

    print("\n--- PUT OPTION ---")
    print(f"  Price:    ${put_greeks.option_price:.4f}")
    print(f"  Delta:    {put_greeks.delta:.4f}")
    print(f"  Gamma:    {put_greeks.gamma:.6f}")
    print(f"  Theta:    {put_greeks.theta:.4f} ($/day)")
    print(f"  Vega:     {put_greeks.vega:.4f} ($/1% vol)")
    print(f"  Rho:      {put_greeks.rho:.4f} ($/1% rate)")

    # Put-Call Parity check
    parity_diff = call_greeks.option_price - put_greeks.option_price - S + K * np.exp(-r * T)
    print(f"\nPut-Call Parity Check: {parity_diff:.8f} (should be ~0)")

    # Implied volatility round-trip
    iv = implied_volatility(call_greeks.option_price, S, K, r, T, "call")
    print(f"\nImplied Volatility (round-trip): {iv:.4f} (input: {sigma:.4f})")
