import React, { useState } from 'react';
import { settlementApi } from '../services/api';

const STATUS_COLORS = {
  PENDING: '#ffaa00', INSTRUCTED: '#00d4ff', MATCHED: '#c896ff',
  SETTLED: '#00c896', FAILED: '#ff4444', CANCELLED: '#666', RECYCLED: '#ff8800',
};

export default function SettlementStatus() {
  const [instructions, setInstructions] = useState([]);
  const [form, setForm] = useState({
    tradeReference: '', isin: '', quantity: '', settlementAmount: '',
    currency: 'USD', deliverBic: '', receiveBic: '',
    deliverAccount: '', receiveAccount: '', type: 'DVP',
    settlementDate: new Date(Date.now() + 2 * 86400000).toISOString().split('T')[0],
  });

  async function loadInstructions() {
    try {
      const data = await settlementApi.getAll();
      setInstructions(data);
    } catch (e) {
      console.error('Load instructions error:', e);
    }
  }

  async function handleCreate(e) {
    e.preventDefault();
    try {
      await settlementApi.create({
        ...form,
        quantity: parseFloat(form.quantity),
        settlementAmount: parseFloat(form.settlementAmount),
        settlementDate: form.settlementDate,
      });
      await loadInstructions();
    } catch (e) {
      console.error('Create instruction error:', e);
    }
  }

  async function updateStatus(tradeRef, status) {
    try {
      await settlementApi.updateStatus(tradeRef, status);
      await loadInstructions();
    } catch (e) {
      console.error('Update status error:', e);
    }
  }

  const pendingCount = instructions.filter(i => i.status === 'PENDING' || i.status === 'INSTRUCTED').length;
  const settledCount = instructions.filter(i => i.status === 'SETTLED').length;
  const failedCount = instructions.filter(i => i.status === 'FAILED').length;

  return (
    <div>
      <div className="grid-3" style={{ marginBottom: 16 }}>
        <div className="stat-card">
          <div className="stat-label">Pending Settlement</div>
          <div className="stat-value warning">{pendingCount}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Settled Today</div>
          <div className="stat-value positive">{settledCount}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Failed</div>
          <div className="stat-value negative">{failedCount}</div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-title">New Settlement Instruction</div>
          <form onSubmit={handleCreate}>
            <div className="grid-2">
              {[
                ['tradeReference', 'Trade Reference'], ['isin', 'ISIN'],
                ['quantity', 'Quantity'], ['settlementAmount', 'Settlement Amount'],
                ['currency', 'Currency'], ['deliverBic', 'Deliver BIC'],
                ['receiveBic', 'Receive BIC'], ['deliverAccount', 'Deliver Account'],
                ['receiveAccount', 'Receive Account'],
              ].map(([name, label]) => (
                <div className="form-group" key={name}>
                  <label className="form-label">{label}</label>
                  <input className="form-input" name={name} value={form[name]}
                    onChange={e => setForm({ ...form, [name]: e.target.value })} />
                </div>
              ))}
              <div className="form-group">
                <label className="form-label">Type</label>
                <select className="form-input" value={form.type}
                  onChange={e => setForm({ ...form, type: e.target.value })}>
                  {['DVP', 'FOP', 'RVP', 'DFP'].map(t => <option key={t}>{t}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Settlement Date</label>
                <input className="form-input" type="date" name="settlementDate"
                  value={form.settlementDate}
                  onChange={e => setForm({ ...form, settlementDate: e.target.value })} />
              </div>
            </div>
            <button type="submit" className="btn btn-primary" style={{ marginTop: 8 }}>
              Create Instruction
            </button>
          </form>
        </div>

        <div className="card">
          <div className="card-title">Settlement Actions</div>
          <button className="btn btn-primary" onClick={loadInstructions} style={{ marginBottom: 12 }}>
            Refresh Instructions
          </button>
          <div style={{ color: '#a0a0c0', fontSize: 12 }}>
            <p>Settlement lifecycle:</p>
            <div style={{ marginTop: 8 }}>
              {['PENDING → INSTRUCTED → MATCHED → SETTLED'].map((flow, i) => (
                <div key={i} style={{ padding: '4px 0', color: '#00d4ff' }}>{flow}</div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-title">Settlement Instructions</div>
        <table className="table">
          <thead>
            <tr>
              <th>Reference</th><th>Trade Ref</th><th>ISIN</th><th>Qty</th>
              <th>Amount</th><th>CCY</th><th>Type</th><th>Settle Date</th>
              <th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {instructions.length === 0 ? (
              <tr><td colSpan={10} style={{ textAlign: 'center', padding: 20, color: '#a0a0c0' }}>
                No settlement instructions
              </td></tr>
            ) : instructions.map(inst => (
              <tr key={inst.id}>
                <td style={{ fontFamily: 'monospace', color: '#00d4ff' }}>{inst.settlementReference}</td>
                <td style={{ fontFamily: 'monospace' }}>{inst.tradeReference}</td>
                <td>{inst.isin}</td>
                <td>{(inst.quantity || 0).toLocaleString()}</td>
                <td>{(inst.settlementAmount || 0).toLocaleString()}</td>
                <td>{inst.currency}</td>
                <td>{inst.type}</td>
                <td>{inst.settlementDate}</td>
                <td>
                  <span className="badge" style={{
                    background: `${STATUS_COLORS[inst.status]}22`,
                    color: STATUS_COLORS[inst.status] || '#a0a0c0'
                  }}>
                    {inst.status}
                  </span>
                </td>
                <td>
                  {inst.status === 'PENDING' && (
                    <button className="btn btn-primary" style={{ padding: '2px 6px', fontSize: 11 }}
                      onClick={() => updateStatus(inst.tradeReference, 'INSTRUCTED')}>
                      Instruct
                    </button>
                  )}
                  {inst.status === 'MATCHED' && (
                    <button className="btn btn-success" style={{ padding: '2px 6px', fontSize: 11 }}
                      onClick={() => updateStatus(inst.tradeReference, 'SETTLED')}>
                      Settle
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
