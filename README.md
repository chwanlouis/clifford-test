# Institutional Trade Management System (ITMS)

A robust, enterprise-grade Front-to-Back Office Trade Management System (TMS) designed for multi-asset class lifecycle management, real-time risk monitoring, and automated settlement workflows.

## 🏗 System Architecture

The ITMS is built on a distributed microservices architecture to ensure high availability, scalability, and low-latency processing.

1.  **Front Office (Execution & Capture):** Order entry, real-time position monitoring, and market data integration.
2.  **Middle Office (Risk & Compliance):** Value-at-Risk (VaR) calculations, limit monitoring, and trade validation.
3.  **Back Office (Settlement & Accounting):** Swift messaging, ledgering, reconciliation, and regulatory reporting.

## 🚀 Core Modules

### 1. Front Office
* **Trade Capture:** Intuitive UI and API endpoints for Manual/Fix/Algo trade entry.
* **Order Management System (OMS):** Routing, slicing, and dice algorithms for optimal execution.
* **Portfolio Management:** Real-time P&L (Realized/Unrealized) and "Greek" sensitivities.

### 2. Middle Office
* **Risk Engine:** Real-time exposure monitoring against pre-defined credit and market limits.
* **Trade Affirmation:** Automated matching of trade details between counterparties.
* **Compliance:** Post-trade compliance checks (MIFID II, Dodd-Frank).

### 3. Back Office
* **Settlement Engine:** Automated generation of SWIFT (MT/MX) messages for cash and security movements.
* **Corporate Actions:** Processing of dividends, splits, and mergers.
* **Accounting (GL):** Double-entry bookkeeping and sub-ledger generation for financial reporting.

## 🛠 Tech Stack

* **Backend:** Java 17+ (Spring Boot), Python (for Quant/Risk models).
* **Messaging:** Apache Kafka (Event-driven architecture).
* **Database:** PostgreSQL (Transactional), InfluxDB (Time-series market data).
* **Cache:** Redis (Real-time position caching).
* **Frontend:** React.js with OpenFin/Electron for desktop-grade experience.
* **Infrastructure:** Kubernetes, Docker, Terraform.
