import React, { useState } from 'react';
import { portfolioApi } from '../services/api';

export default function PortfolioView() {
  const [portfolio, setPortfolio] = useState('PORT-001');
  const [positions, setPositions] = useState([]);
  const [marketValue, setMarketValue] = useState(null);
  const [totalPnl, setTotalPnl] = useState(null);
  const [greeks, setGreeks] = useState(null);
  const [greeksForm, setGreeksForm] = useState({
    instrument: 'AAPL', spotPrice: 175, strikePrice: 180, riskFreeRate: 0.05,
    volatility: 0.25, timeToExpiry: 0.25, isCall: true,
  });

  async function loadPortfolio() {
    try {
      const [pos, mv, pnl] = await Promise.all([
        portfolioApi.getPositions(portfolio),
        portfolioApi.getMarketValue(portfolio),
        portfolioApi.getPnl(portfolio),
      ]);
      setPositions(pos);
      setMarketValue(mv);
      setTotalPnl(pnl);
    } catch (e) {
      console.error('Portfolio load error:', e);
    }
  }

  async function loadGreeks() {
    try {
      const result = await portfolioApi.getGreeks(portfolio, greeksForm.instrument, greeksForm);
      setGreeks(result);
    } catch (e) {
      console.error('Greeks calculation error:', e);
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, alignItems: 'center' }}>
        <input
          className="form-input" style={{ width: 200 }}
          placeholder="Portfolio ID"
          value={portfolio}
          onChange={e => setPortfolio(e.target.value)}
        />
        <button className="btn btn-primary" onClick={loadPortfolio}>Load Portfolio</button>
      </div>

      {(marketValue !== null || totalPnl !== null) && (
        <div className="grid-2" style={{ marginBottom: 16 }}>
          <div className="stat-card">
            <div className="stat-label">Market Value</div>
            <div className="stat-value">${(marketValue || 0).toLocaleString()}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Total P&L</div>
            <div className={`stat-value ${totalPnl >= 0 ? 'positive' : 'negative'}`}>
              {totalPnl >= 0 ? '+' : ''}${(totalPnl || 0).toLocaleString()}
            </div>
          </div>
        </div>
      )}

      <div className="card">
        <div className="card-title">Positions</div>
        <table className="table">
          <thead>
            <tr>
              <th>Instrument</th>
              <th>ISIN</th>
              <th>Quantity</th>
              <th>Avg Cost</th>
              <th>Current Price</th>
              <th>Market Value</th>
              <th>Unrealized P&L</th>
              <th>Realized P&L</th>
            </tr>
          </thead>
          <tbody>
            {positions.length === 0 ? (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: 20, color: '#a0a0c0' }}>No positions</td></tr>
            ) : positions.map(pos => (
              <tr key={pos.isin}>
                <td>{pos.instrument}</td>
                <td style={{ fontFamily: 'monospace' }}>{pos.isin}</td>
                <td>{(pos.quantity || 0).toLocaleString()}</td>
                <td>{(pos.averageCost || 0).toFixed(4)}</td>
                <td>{(pos.currentPrice || 0).toFixed(4)}</td>
                <td>{(pos.marketValue || 0).toLocaleString()}</td>
                <td className={pos.unrealizedPnl >= 0 ? 'positive' : 'negative'}>
                  {(pos.unrealizedPnl || 0).toLocaleString()}
                </td>
                <td className={pos.realizedPnl >= 0 ? 'positive' : 'negative'}>
                  {(pos.realizedPnl || 0).toLocaleString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <div className="card-title">Options Greeks Calculator</div>
        <div className="grid-3" style={{ marginBottom: 12 }}>
          {[
            ['instrument', 'Instrument'], ['spotPrice', 'Spot Price'],
            ['strikePrice', 'Strike Price'], ['riskFreeRate', 'Risk-Free Rate'],
            ['volatility', 'Volatility'], ['timeToExpiry', 'Time to Expiry (yr)'],
          ].map(([name, label]) => (
            <div className="form-group" key={name}>
              <label className="form-label">{label}</label>
              <input className="form-input" type={name === 'instrument' ? 'text' : 'number'}
                step="0.01" name={name} value={greeksForm[name]}
                onChange={e => setGreeksForm({ ...greeksForm, [name]: e.target.value })} />
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center', marginBottom: 12 }}>
          <label style={{ color: '#a0a0c0', display: 'flex', gap: 8, alignItems: 'center' }}>
            <input type="checkbox" checked={greeksForm.isCall}
              onChange={e => setGreeksForm({ ...greeksForm, isCall: e.target.checked })} />
            Call Option
          </label>
          <button className="btn btn-primary" onClick={loadGreeks}>Calculate Greeks</button>
        </div>
        {greeks && (
          <div className="grid-3">
            {[
              ['Delta (Δ)', greeks.delta, 'Δ Price / Δ Spot'],
              ['Gamma (Γ)', greeks.gamma, 'Δ Delta / Δ Spot'],
              ['Theta (θ)', greeks.theta, 'Time decay / day'],
              ['Vega (ν)', greeks.vega, 'Δ Price / 1% vol'],
              ['Rho (ρ)', greeks.rho, 'Δ Price / 1% rate'],
            ].map(([label, value, desc]) => (
              <div key={label} className="stat-card">
                <div className="stat-label">{label}</div>
                <div className="stat-value" style={{ fontSize: 18, color: '#00d4ff' }}>
                  {typeof value === 'number' ? value.toFixed(6) : value}
                </div>
                <div className="stat-change neutral">{desc}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
