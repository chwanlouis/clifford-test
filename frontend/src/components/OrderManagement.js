import React, { useState } from 'react';
import { omsApi } from '../services/api';

const ALGORITHMS = ['NONE', 'TWAP', 'VWAP', 'POV', 'IS', 'ARRIVAL_PRICE', 'DARK_POOL', 'SMART_ROUTE'];
const ORDER_TYPES = ['MARKET', 'LIMIT', 'STOP', 'STOP_LIMIT', 'ICEBERG', 'TWAP', 'VWAP'];

export default function OrderManagement() {
  const [form, setForm] = useState({
    instrument: '', isin: '', side: 'BUY', orderType: 'LIMIT',
    totalQuantity: '', limitPrice: '', currency: 'USD',
    portfolio: '', trader: '', algorithm: 'NONE',
  });
  const [slices, setSlices] = useState([]);
  const [venue, setVenue] = useState('');
  const [numSlices, setNumSlices] = useState(10);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  async function handleRoute() {
    try {
      const order = { ...form, totalQuantity: parseFloat(form.totalQuantity), limitPrice: parseFloat(form.limitPrice) };
      const result = await omsApi.routeOrder(order);
      setVenue(result);
    } catch (e) {
      console.error('Routing error:', e);
    }
  }

  async function handleSlice() {
    try {
      const order = { ...form, totalQuantity: parseFloat(form.totalQuantity), limitPrice: parseFloat(form.limitPrice) };
      const result = await omsApi.sliceOrder(order, numSlices);
      setSlices(result);
    } catch (e) {
      console.error('Slicing error:', e);
    }
  }

  return (
    <div>
      <div className="grid-2">
        <div className="card">
          <div className="card-title">Order Entry</div>
          <div className="grid-2">
            {[
              ['instrument', 'Instrument'], ['isin', 'ISIN'], ['totalQuantity', 'Quantity'],
              ['limitPrice', 'Limit Price'], ['portfolio', 'Portfolio'], ['trader', 'Trader'],
            ].map(([name, label]) => (
              <div className="form-group" key={name}>
                <label className="form-label">{label}</label>
                <input className="form-input" name={name} value={form[name]} onChange={handleChange} />
              </div>
            ))}
            <div className="form-group">
              <label className="form-label">Side</label>
              <select className="form-input" name="side" value={form.side} onChange={handleChange}>
                {['BUY', 'SELL', 'SELL_SHORT', 'COVER'].map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Order Type</label>
              <select className="form-input" name="orderType" value={form.orderType} onChange={handleChange}>
                {ORDER_TYPES.map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Algorithm</label>
              <select className="form-input" name="algorithm" value={form.algorithm} onChange={handleChange}>
                {ALGORITHMS.map(a => <option key={a}>{a}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Currency</label>
              <select className="form-input" name="currency" value={form.currency} onChange={handleChange}>
                {['USD', 'EUR', 'GBP', 'JPY'].map(c => <option key={c}>{c}</option>)}
              </select>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button className="btn btn-primary" onClick={handleRoute}>Route Order</button>
            <button className="btn btn-success" onClick={handleSlice}>Slice Order</button>
          </div>
          {venue && (
            <div style={{ marginTop: 12, padding: 8, background: 'rgba(0,212,255,0.1)', borderRadius: 4 }}>
              <span className="neutral">Routing Venue: </span>
              <span style={{ color: '#00d4ff' }}>{venue}</span>
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-title">TWAP/VWAP Configuration</div>
          <div className="form-group">
            <label className="form-label">Number of Slices</label>
            <input className="form-input" type="number" value={numSlices}
              onChange={e => setNumSlices(parseInt(e.target.value))} />
          </div>
          <div style={{ color: '#a0a0c0', fontSize: 11, marginTop: 8 }}>
            Each slice: {form.totalQuantity ? (parseFloat(form.totalQuantity) / numSlices).toFixed(0) : '-'} shares
          </div>
        </div>
      </div>

      {slices.length > 0 && (
        <div className="card">
          <div className="card-title">Order Slices ({slices.length})</div>
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Instrument</th>
                <th>Side</th>
                <th>Quantity</th>
                <th>Limit Price</th>
                <th>Algorithm</th>
              </tr>
            </thead>
            <tbody>
              {slices.map((slice, i) => (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td>{slice.instrument}</td>
                  <td className={slice.side === 'BUY' ? 'positive' : 'negative'}>{slice.side}</td>
                  <td>{(slice.totalQuantity || 0).toLocaleString()}</td>
                  <td>{slice.limitPrice || 'MARKET'}</td>
                  <td>{slice.algorithm}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
