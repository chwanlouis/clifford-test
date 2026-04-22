import axios from 'axios';

const BASE_URLS = {
  trades: process.env.REACT_APP_TRADE_SERVICE_URL || 'http://localhost:8081',
  oms: process.env.REACT_APP_OMS_SERVICE_URL || 'http://localhost:8082',
  portfolio: process.env.REACT_APP_PORTFOLIO_SERVICE_URL || 'http://localhost:8083',
  risk: process.env.REACT_APP_RISK_ENGINE_URL || 'http://localhost:8084',
  settlement: process.env.REACT_APP_SETTLEMENT_URL || 'http://localhost:8087',
};

function createClient(baseURL) {
  const client = axios.create({ baseURL });
  client.interceptors.response.use(
    response => response.data,
    error => {
      console.error('API Error:', error.response?.data || error.message);
      return Promise.reject(error);
    }
  );
  return client;
}

const tradeClient = createClient(BASE_URLS.trades);
const omsClient = createClient(BASE_URLS.oms);
const portfolioClient = createClient(BASE_URLS.portfolio);
const riskClient = createClient(BASE_URLS.risk);
const settlementClient = createClient(BASE_URLS.settlement);

export const tradeApi = {
  getAll: () => tradeClient.get('/api/v1/trades'),
  getById: (id) => tradeClient.get(`/api/v1/trades/${id}`),
  create: (trade) => tradeClient.post('/api/v1/trades', trade),
  updateStatus: (id, status) => tradeClient.patch(`/api/v1/trades/${id}/status`, null, { params: { status } }),
  amend: (id, trade) => tradeClient.put(`/api/v1/trades/${id}/amend`, trade),
  cancel: (id) => tradeClient.delete(`/api/v1/trades/${id}`),
  getByPortfolio: (portfolio) => tradeClient.get(`/api/v1/trades/portfolio/${portfolio}`),
  getByReference: (ref) => tradeClient.get(`/api/v1/trades/reference/${ref}`),
};

export const omsApi = {
  routeOrder: (order) => omsClient.post('/api/v1/orders/route', order),
  sliceOrder: (order, slices) => omsClient.post('/api/v1/orders/slice', order, { params: { slices } }),
  twapSlice: (order, intervalMinutes, durationMinutes) =>
    omsClient.post('/api/v1/orders/twap', order, { params: { intervalMinutes, durationMinutes } }),
};

export const portfolioApi = {
  getPositions: (portfolio) => portfolioClient.get(`/api/v1/portfolio/${portfolio}/positions`),
  getMarketValue: (portfolio) => portfolioClient.get(`/api/v1/portfolio/${portfolio}/market-value`),
  getPnl: (portfolio) => portfolioClient.get(`/api/v1/portfolio/${portfolio}/pnl`),
  getGreeks: (portfolio, instrument, params) =>
    portfolioClient.get(`/api/v1/portfolio/${portfolio}/greeks/${instrument}`, { params }),
};

export const riskApi = {
  setLimit: (limit) => riskClient.post('/api/v1/risk/limits', limit),
  checkLimit: (params) => riskClient.post('/api/v1/risk/check', null, { params }),
  getLimitUtilizations: () => riskClient.get('/api/v1/risk/limits/utilization'),
  calculateParametricVaR: (params) => riskClient.post('/api/v1/risk/var/parametric', null, { params }),
  calculateMonteCarloVaR: (params) => riskClient.post('/api/v1/risk/var/montecarlo', null, { params }),
};

export const settlementApi = {
  create: (instruction) => settlementClient.post('/api/v1/settlement/instructions', instruction),
  getAll: () => settlementClient.get('/api/v1/settlement/instructions'),
  getPending: () => settlementClient.get('/api/v1/settlement/instructions/pending'),
  getByDate: (date) => settlementClient.get(`/api/v1/settlement/instructions/date/${date}`),
  updateStatus: (tradeRef, status) =>
    settlementClient.patch(`/api/v1/settlement/instructions/${tradeRef}/status`, null, { params: { status } }),
};
