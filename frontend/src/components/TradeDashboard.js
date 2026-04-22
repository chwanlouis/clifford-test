import React, { useState, useEffect } from 'react';
import { tradeApi } from '../services/api';

const TRADE_STATUSES = ['NEW', 'PENDING_VALIDATION', 'VALIDATED', 'AFFIRMED', 'SETTLED', 'CANCELLED'];
const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'FX', 'COMMODITY', 'DERIVATIVE'];
const SIDES = ['BUY', 'SELL', 'SELL_SHORT', 'COVER'];

function StatusBadge({ status }) {
  const classMap = {
    NEW: 'badge-new', PENDING_VALIDATION: 'badge-pending', VALIDATED: 'badge-validated',
    AFFIRMED: 'badge-validated', SETTLED: 'badge-settled', CANCELLED: 'badge-cancelled',
    FAILED: 'badge-cancelled',
  };
  return <span className={`badge ${classMap[status] || 'badge-new'}`}>{status}</span>;
}

function TradeForm({ onSubmit, onClose }) {
  const [form, setForm] = useState({
    instrument: '', isin: '', side: 'BUY', quantity: '', price: '',
    currency: 'USD', counterparty: '', trader: '', portfolio: '',
    assetClass: 'EQUITY', source: 'MANUAL',
    tradeDate: new Date().toISOString().split('T')[0],
    settlementDate: new Date(Date.now() + 2 * 86400000).toISOString().split('T')[0],
  });

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      quantity: parseFloat(form.quantity),
      price: parseFloat(form.price),
      tradeDate: form.tradeDate + 'T09:00:00',
      settlementDate: form.settlementDate + 'T16:00:00',
    });
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div className="card" style={{ width: 600, maxHeight: '90vh', overflow: 'auto' }}>
        <div className="card-title">New Trade</div>
        <form onSubmit={handleSubmit}>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">Instrument</label>
              <input className="form-input" name="instrument" value={form.instrument} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">ISIN</label>
              <input className="form-input" name="isin" value={form.isin} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Side</label>
              <select className="form-input" name="side" value={form.side} onChange={handleChange}>
                {SIDES.map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Asset Class</label>
              <select className="form-input" name="assetClass" value={form.assetClass} onChange={handleChange}>
                {ASSET_CLASSES.map(a => <option key={a}>{a}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Quantity</label>
              <input className="form-input" name="quantity" type="number" value={form.quantity} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Price</label>
              <input className="form-input" name="price" type="number" step="0.0001" value={form.price} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Currency</label>
              <select className="form-input" name="currency" value={form.currency} onChange={handleChange}>
                {['USD', 'EUR', 'GBP', 'JPY', 'HKD'].map(c => <option key={c}>{c}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Counterparty</label>
              <input className="form-input" name="counterparty" value={form.counterparty} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Trader</label>
              <input className="form-input" name="trader" value={form.trader} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Portfolio</label>
              <input className="form-input" name="portfolio" value={form.portfolio} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Trade Date</label>
              <input className="form-input" name="tradeDate" type="date" value={form.tradeDate} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">Settlement Date</label>
              <input className="form-input" name="settlementDate" type="date" value={form.settlementDate} onChange={handleChange} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
            <button type="button" className="btn btn-danger" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-success">Submit Trade</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function TradeDashboard() {
  const [trades, setTrades] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    loadTrades();
  }, []);

  async function loadTrades() {
    setLoading(true);
    try {
      const data = await tradeApi.getAll();
      setTrades(data);
    } catch (e) {
      console.error('Failed to load trades:', e);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmitTrade(tradeData) {
    try {
      await tradeApi.create(tradeData);
      setShowForm(false);
      await loadTrades();
    } catch (e) {
      console.error('Failed to create trade:', e);
    }
  }

  async function handleCancel(id) {
    if (!window.confirm('Cancel this trade?')) return;
    try {
      await tradeApi.cancel(id);
      await loadTrades();
    } catch (e) {
      console.error('Failed to cancel trade:', e);
    }
  }

  const filteredTrades = trades.filter(t =>
    !filter || t.instrument?.toLowerCase().includes(filter.toLowerCase())
      || t.tradeReference?.includes(filter)
      || t.portfolio?.toLowerCase().includes(filter.toLowerCase())
  );

  const totalNotional = trades.reduce((sum, t) =>
    t.status !== 'CANCELLED' ? sum + (t.quantity || 0) * (t.price || 0) : sum, 0);
  const buyCount = trades.filter(t => t.side === 'BUY').length;
  const sellCount = trades.filter(t => t.side === 'SELL').length;
  const pendingCount = trades.filter(t => t.status === 'PENDING_VALIDATION').length;

  return (
    <div>
      <div className="grid-3" style={{ marginBottom: 16 }}>
        <div className="stat-card">
          <div className="stat-label">Total Trades</div>
          <div className="stat-value">{trades.length}</div>
          <div className="stat-change neutral">B: {buyCount} | S: {sellCount}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Total Notional</div>
          <div className="stat-value">${(totalNotional / 1e6).toFixed(2)}M</div>
          <div className="stat-change neutral">USD equivalent</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Pending Validation</div>
          <div className={`stat-value ${pendingCount > 0 ? 'warning' : 'positive'}`}>{pendingCount}</div>
          <div className="stat-change neutral">Requires action</div>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div className="card-title" style={{ margin: 0, borderBottom: 'none', padding: 0 }}>
            Trade Blotter
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              className="form-input"
              placeholder="Search..."
              value={filter}
              onChange={e => setFilter(e.target.value)}
              style={{ width: 200 }}
            />
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>
              + New Trade
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#a0a0c0' }}>Loading trades...</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Instrument</th>
                <th>Side</th>
                <th>Quantity</th>
                <th>Price</th>
                <th>Notional</th>
                <th>CCY</th>
                <th>Portfolio</th>
                <th>Counterparty</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredTrades.length === 0 ? (
                <tr><td colSpan={11} style={{ textAlign: 'center', padding: 20, color: '#a0a0c0' }}>No trades found</td></tr>
              ) : filteredTrades.map(trade => (
                <tr key={trade.id}>
                  <td style={{ fontFamily: 'monospace', color: '#00d4ff' }}>{trade.tradeReference}</td>
                  <td>{trade.instrument}</td>
                  <td className={trade.side === 'BUY' ? 'positive' : 'negative'}>{trade.side}</td>
                  <td>{(trade.quantity || 0).toLocaleString()}</td>
                  <td>{(trade.price || 0).toFixed(4)}</td>
                  <td>{((trade.quantity || 0) * (trade.price || 0)).toLocaleString()}</td>
                  <td>{trade.currency}</td>
                  <td>{trade.portfolio}</td>
                  <td>{trade.counterparty}</td>
                  <td><StatusBadge status={trade.status} /></td>
                  <td>
                    {trade.status !== 'CANCELLED' && trade.status !== 'SETTLED' && (
                      <button className="btn btn-danger" style={{ padding: '2px 6px', fontSize: 11 }}
                        onClick={() => handleCancel(trade.id)}>
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showForm && <TradeForm onSubmit={handleSubmitTrade} onClose={() => setShowForm(false)} />}
    </div>
  );
}
