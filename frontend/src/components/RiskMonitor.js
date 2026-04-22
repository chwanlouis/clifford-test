import React, { useState } from 'react';
import { riskApi } from '../services/api';

export default function RiskMonitor() {
  const [varForm, setVarForm] = useState({
    portfolioValue: 10000000, dailyVolatility: 0.015,
    confidenceLevel: 0.99, holdingPeriodDays: 1,
  });
  const [varResult, setVarResult] = useState(null);
  const [limitForm, setLimitForm] = useState({
    portfolio: 'PORT-001', counterparty: 'Goldman Sachs',
    limitType: 'CREDIT_LIMIT', limitAmount: 50000000, currency: 'USD',
  });
  const [limitUtilizations, setLimitUtilizations] = useState([]);
  const [checkForm, setCheckForm] = useState({
    portfolio: 'PORT-001', counterparty: 'Goldman Sachs',
    limitType: 'CREDIT_LIMIT', proposedExposure: 1000000,
    currentExposure: 0, currency: 'USD',
  });
  const [checkResult, setCheckResult] = useState(null);

  async function calculateVaR() {
    try {
      const result = await riskApi.calculateParametricVaR(varForm);
      setVarResult(result);
    } catch (e) {
      console.error('VaR calculation error:', e);
    }
  }

  async function setLimit() {
    try {
      await riskApi.setLimit({
        portfolio: limitForm.portfolio,
        counterparty: limitForm.counterparty,
        limitType: limitForm.limitType,
        limitAmount: parseFloat(limitForm.limitAmount),
        currency: limitForm.currency,
      });
      await loadUtilizations();
    } catch (e) {
      console.error('Set limit error:', e);
    }
  }

  async function loadUtilizations() {
    try {
      const data = await riskApi.getLimitUtilizations();
      setLimitUtilizations(data);
    } catch (e) {
      console.error('Load utilizations error:', e);
    }
  }

  async function checkLimit() {
    try {
      const result = await riskApi.checkLimit(checkForm);
      setCheckResult(result);
    } catch (e) {
      console.error('Check limit error:', e);
    }
  }

  return (
    <div>
      <div className="grid-2">
        <div className="card">
          <div className="card-title">VaR Calculator</div>
          <div className="grid-2">
            {[
              ['portfolioValue', 'Portfolio Value (USD)'],
              ['dailyVolatility', 'Daily Volatility'],
              ['confidenceLevel', 'Confidence Level'],
              ['holdingPeriodDays', 'Holding Period (days)'],
            ].map(([name, label]) => (
              <div className="form-group" key={name}>
                <label className="form-label">{label}</label>
                <input className="form-input" type="number" step="0.001" name={name}
                  value={varForm[name]}
                  onChange={e => setVarForm({ ...varForm, [name]: parseFloat(e.target.value) })} />
              </div>
            ))}
          </div>
          <button className="btn btn-primary" onClick={calculateVaR}>Calculate VaR</button>
          {varResult !== null && (
            <div style={{ marginTop: 16 }}>
              <div className="stat-card">
                <div className="stat-label">Parametric VaR ({(varForm.confidenceLevel * 100).toFixed(0)}% CI, {varForm.holdingPeriodDays}d)</div>
                <div className="stat-value negative">${Number(varResult).toLocaleString()}</div>
                <div className="stat-change neutral">
                  {(Number(varResult) / varForm.portfolioValue * 100).toFixed(2)}% of portfolio value
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-title">Limit Management</div>
          <div className="grid-2">
            {[['portfolio', 'Portfolio'], ['counterparty', 'Counterparty'],
              ['limitAmount', 'Limit Amount'], ['currency', 'Currency']].map(([name, label]) => (
              <div className="form-group" key={name}>
                <label className="form-label">{label}</label>
                <input className="form-input" name={name} value={limitForm[name]}
                  onChange={e => setLimitForm({ ...limitForm, [name]: e.target.value })} />
              </div>
            ))}
            <div className="form-group">
              <label className="form-label">Limit Type</label>
              <select className="form-input" value={limitForm.limitType}
                onChange={e => setLimitForm({ ...limitForm, limitType: e.target.value })}>
                {['CREDIT_LIMIT', 'MARKET_RISK_VAR', 'POSITION_LIMIT', 'NOTIONAL_LIMIT',
                  'CONCENTRATION_LIMIT', 'STOP_LOSS'].map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button className="btn btn-primary" onClick={setLimit}>Set Limit</button>
            <button className="btn btn-success" onClick={loadUtilizations}>Refresh</button>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-title">Limit Check</div>
        <div className="grid-3">
          {[['portfolio', 'Portfolio'], ['counterparty', 'Counterparty'],
            ['proposedExposure', 'Proposed Exposure'], ['currentExposure', 'Current Exposure'],
            ['currency', 'Currency']].map(([name, label]) => (
            <div className="form-group" key={name}>
              <label className="form-label">{label}</label>
              <input className="form-input" name={name} value={checkForm[name]}
                onChange={e => setCheckForm({ ...checkForm, [name]: e.target.value })} />
            </div>
          ))}
          <div className="form-group">
            <label className="form-label">Limit Type</label>
            <select className="form-input" value={checkForm.limitType}
              onChange={e => setCheckForm({ ...checkForm, limitType: e.target.value })}>
              {['CREDIT_LIMIT', 'MARKET_RISK_VAR', 'POSITION_LIMIT', 'NOTIONAL_LIMIT'].map(t =>
                <option key={t}>{t}</option>)}
            </select>
          </div>
        </div>
        <button className="btn btn-primary" onClick={checkLimit}>Check Limit</button>
        {checkResult && (
          <div style={{
            marginTop: 12, padding: 12,
            background: checkResult.breached ? 'rgba(255,68,68,0.1)' : 'rgba(0,200,150,0.1)',
            borderRadius: 4, border: `1px solid ${checkResult.breached ? '#ff4444' : '#00c896'}`
          }}>
            <div className={checkResult.breached ? 'negative' : 'positive'} style={{ fontWeight: 'bold', marginBottom: 4 }}>
              {checkResult.breached ? '⚠ LIMIT BREACHED' : '✓ Within Limits'}
            </div>
            <div style={{ color: '#a0a0c0' }}>{checkResult.message}</div>
            <div style={{ marginTop: 4 }}>
              Utilization: <span style={{ color: '#00d4ff' }}>
                {(checkResult.utilizationPercent || 0).toFixed(2)}%
              </span>
            </div>
          </div>
        )}
      </div>

      {limitUtilizations.length > 0 && (
        <div className="card">
          <div className="card-title">Limit Utilizations</div>
          <table className="table">
            <thead>
              <tr>
                <th>Portfolio</th><th>Counterparty</th><th>Type</th>
                <th>Limit</th><th>Utilization</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              {limitUtilizations.map((u, i) => (
                <tr key={i}>
                  <td>{u.portfolio}</td>
                  <td>{u.counterparty}</td>
                  <td>{u.limitType}</td>
                  <td>${(u.limitAmount || 0).toLocaleString()}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div style={{ flex: 1, background: '#0d0d1f', borderRadius: 4, height: 6 }}>
                        <div style={{
                          width: `${Math.min(100, u.utilizationPercent || 0)}%`,
                          height: '100%',
                          background: u.breached ? '#ff4444' : '#00c896',
                          borderRadius: 4,
                        }} />
                      </div>
                      <span>{(u.utilizationPercent || 0).toFixed(1)}%</span>
                    </div>
                  </td>
                  <td className={u.breached ? 'negative' : 'positive'}>
                    {u.breached ? 'BREACHED' : 'OK'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
