import React from 'react';
import { Routes, Route, NavLink } from 'react-router-dom';
import TradeDashboard from './components/TradeDashboard';
import OrderManagement from './components/OrderManagement';
import PortfolioView from './components/PortfolioView';
import RiskMonitor from './components/RiskMonitor';
import SettlementStatus from './components/SettlementStatus';

function App() {
  return (
    <div className="app-container">
      <nav className="navbar">
        <span className="navbar-brand">⚡ ITMS</span>
        <NavLink to="/" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`} end>
          Trade Capture
        </NavLink>
        <NavLink to="/orders" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          OMS
        </NavLink>
        <NavLink to="/portfolio" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Portfolio
        </NavLink>
        <NavLink to="/risk" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Risk
        </NavLink>
        <NavLink to="/settlement" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Settlement
        </NavLink>
      </nav>
      <main className="main-content">
        <Routes>
          <Route path="/" element={<TradeDashboard />} />
          <Route path="/orders" element={<OrderManagement />} />
          <Route path="/portfolio" element={<PortfolioView />} />
          <Route path="/risk" element={<RiskMonitor />} />
          <Route path="/settlement" element={<SettlementStatus />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
