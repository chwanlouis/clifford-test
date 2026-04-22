-- ITMS Database Schema - V1 Initial Schema
-- PostgreSQL

-- ===========================
-- Trade Capture Database
-- ===========================
CREATE DATABASE itms_trades;
\c itms_trades;

CREATE TABLE IF NOT EXISTS trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_reference VARCHAR(50) UNIQUE NOT NULL,
    instrument VARCHAR(100) NOT NULL,
    isin VARCHAR(12) NOT NULL,
    side VARCHAR(20) NOT NULL,
    quantity NUMERIC(20, 6) NOT NULL,
    price NUMERIC(20, 6) NOT NULL,
    currency CHAR(3) NOT NULL,
    counterparty VARCHAR(200) NOT NULL,
    trader VARCHAR(100) NOT NULL,
    portfolio VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    source VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    trade_date TIMESTAMP NOT NULL,
    settlement_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_trades_reference ON trades(trade_reference);
CREATE INDEX idx_trades_portfolio ON trades(portfolio);
CREATE INDEX idx_trades_status ON trades(status);
CREATE INDEX idx_trades_trade_date ON trades(trade_date);
CREATE INDEX idx_trades_isin ON trades(isin);
CREATE INDEX idx_trades_counterparty ON trades(counterparty);

-- ===========================
-- OMS Database
-- ===========================
CREATE DATABASE itms_oms;
\c itms_oms;

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_reference VARCHAR(50) UNIQUE NOT NULL,
    instrument VARCHAR(100) NOT NULL,
    isin VARCHAR(12) NOT NULL,
    side VARCHAR(20) NOT NULL,
    order_type VARCHAR(30) NOT NULL,
    total_quantity NUMERIC(20, 6) NOT NULL,
    limit_price NUMERIC(20, 6),
    stop_price NUMERIC(20, 6),
    currency CHAR(3) NOT NULL,
    portfolio VARCHAR(100) NOT NULL,
    trader VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    algorithm VARCHAR(50) NOT NULL DEFAULT 'NONE',
    filled_quantity NUMERIC(20, 6) DEFAULT 0,
    average_fill_price NUMERIC(20, 6),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_orders_reference ON orders(order_reference);
CREATE INDEX idx_orders_portfolio ON orders(portfolio);
CREATE INDEX idx_orders_status ON orders(status);

-- ===========================
-- Portfolio Database
-- ===========================
CREATE DATABASE itms_portfolio;
\c itms_portfolio;

CREATE TABLE IF NOT EXISTS positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio VARCHAR(100) NOT NULL,
    instrument VARCHAR(100) NOT NULL,
    isin VARCHAR(12) NOT NULL,
    currency CHAR(3) NOT NULL,
    quantity NUMERIC(20, 6) NOT NULL DEFAULT 0,
    average_cost NUMERIC(20, 6) NOT NULL DEFAULT 0,
    current_price NUMERIC(20, 6) NOT NULL DEFAULT 0,
    market_value NUMERIC(20, 6) NOT NULL DEFAULT 0,
    unrealized_pnl NUMERIC(20, 6) NOT NULL DEFAULT 0,
    realized_pnl NUMERIC(20, 6) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT NOW(),
    UNIQUE(portfolio, isin)
);

CREATE INDEX idx_positions_portfolio ON positions(portfolio);
CREATE INDEX idx_positions_isin ON positions(isin);

-- ===========================
-- Risk Engine Database
-- ===========================
CREATE DATABASE itms_risk;
\c itms_risk;

CREATE TABLE IF NOT EXISTS risk_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio VARCHAR(100) NOT NULL,
    counterparty VARCHAR(200) NOT NULL,
    limit_type VARCHAR(50) NOT NULL,
    limit_amount NUMERIC(20, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(portfolio, counterparty, limit_type)
);

CREATE TABLE IF NOT EXISTS risk_breaches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio VARCHAR(100) NOT NULL,
    counterparty VARCHAR(200),
    limit_type VARCHAR(50) NOT NULL,
    breach_amount NUMERIC(20, 2),
    limit_amount NUMERIC(20, 2),
    breach_timestamp TIMESTAMP DEFAULT NOW(),
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_risk_limits_portfolio ON risk_limits(portfolio);
CREATE INDEX idx_risk_breaches_portfolio ON risk_breaches(portfolio);
CREATE INDEX idx_risk_breaches_resolved ON risk_breaches(resolved);

-- ===========================
-- Affirmation Database
-- ===========================
CREATE DATABASE itms_affirmation;
\c itms_affirmation;

CREATE TABLE IF NOT EXISTS trade_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    our_trade_reference VARCHAR(50) NOT NULL,
    counterparty_trade_reference VARCHAR(50),
    isin VARCHAR(12) NOT NULL,
    quantity NUMERIC(20, 6) NOT NULL,
    price NUMERIC(20, 6) NOT NULL,
    currency CHAR(3) NOT NULL,
    counterparty VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    mismatch_reason TEXT,
    trade_date TIMESTAMP NOT NULL,
    matched_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_trade_matches_reference ON trade_matches(our_trade_reference);
CREATE INDEX idx_trade_matches_status ON trade_matches(status);

-- ===========================
-- Settlement Database
-- ===========================
CREATE DATABASE itms_settlement;
\c itms_settlement;

CREATE TABLE IF NOT EXISTS settlement_instructions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_reference VARCHAR(50) UNIQUE NOT NULL,
    trade_reference VARCHAR(50) NOT NULL,
    isin VARCHAR(12) NOT NULL,
    quantity NUMERIC(20, 6) NOT NULL,
    settlement_amount NUMERIC(20, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    deliver_bic VARCHAR(11) NOT NULL,
    receive_bic VARCHAR(11) NOT NULL,
    deliver_account VARCHAR(50) NOT NULL,
    receive_account VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    type VARCHAR(10) NOT NULL DEFAULT 'DVP',
    settlement_date DATE NOT NULL,
    settled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_settlement_reference ON settlement_instructions(settlement_reference);
CREATE INDEX idx_settlement_trade ON settlement_instructions(trade_reference);
CREATE INDEX idx_settlement_status ON settlement_instructions(status);
CREATE INDEX idx_settlement_date ON settlement_instructions(settlement_date);

-- ===========================
-- Corporate Actions Database
-- ===========================
CREATE DATABASE itms_corporate_actions;
\c itms_corporate_actions;

CREATE TABLE IF NOT EXISTS corporate_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_reference VARCHAR(50) UNIQUE NOT NULL,
    isin VARCHAR(12) NOT NULL,
    issuer VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ANNOUNCED',
    announcement_date DATE NOT NULL,
    ex_date DATE,
    record_date DATE,
    payment_date DATE NOT NULL,
    dividend_per_share NUMERIC(20, 6),
    dividend_currency CHAR(3),
    split_ratio_new NUMERIC(10, 4),
    split_ratio_old NUMERIC(10, 4),
    new_isin VARCHAR(12),
    exchange_ratio NUMERIC(10, 6),
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_ca_isin ON corporate_actions(isin);
CREATE INDEX idx_ca_type ON corporate_actions(type);
CREATE INDEX idx_ca_payment_date ON corporate_actions(payment_date);

-- ===========================
-- Accounting Database
-- ===========================
CREATE DATABASE itms_accounting;
\c itms_accounting;

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_code VARCHAR(50) UNIQUE NOT NULL,
    account_name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance NUMERIC(20, 6) DEFAULT 0,
    portfolio VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_reference VARCHAR(50) UNIQUE NOT NULL,
    trade_reference VARCHAR(50) NOT NULL,
    debit_account VARCHAR(50) NOT NULL REFERENCES accounts(account_code),
    credit_account VARCHAR(50) NOT NULL REFERENCES accounts(account_code),
    amount NUMERIC(20, 6) NOT NULL,
    currency CHAR(3) NOT NULL,
    entry_date DATE NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    posted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_journal_trade ON journal_entries(trade_reference);
CREATE INDEX idx_journal_entry_date ON journal_entries(entry_date);
CREATE INDEX idx_journal_status ON journal_entries(status);
CREATE INDEX idx_accounts_code ON accounts(account_code);

-- Standard Chart of Accounts
INSERT INTO accounts (account_code, account_name, type, currency) VALUES
    ('CASH-DEFAULT', 'Cash - Default', 'ASSET', 'USD'),
    ('SECURITIES-DEFAULT', 'Securities Portfolio - Default', 'ASSET', 'USD'),
    ('REALIZED-PNL-DEFAULT', 'Realized P&L', 'INCOME', 'USD'),
    ('UNREALIZED-PNL-DEFAULT', 'Unrealized P&L', 'INCOME', 'USD'),
    ('COMMISSION-EXPENSE', 'Commission Expense', 'EXPENSE', 'USD'),
    ('DIVIDEND-INCOME', 'Dividend Income', 'INCOME', 'USD')
ON CONFLICT (account_code) DO NOTHING;
