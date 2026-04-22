import { configureStore, createSlice } from '@reduxjs/toolkit';

const tradesSlice = createSlice({
  name: 'trades',
  initialState: { items: [], loading: false, error: null },
  reducers: {
    setTrades: (state, action) => { state.items = action.payload; },
    setLoading: (state, action) => { state.loading = action.payload; },
    setError: (state, action) => { state.error = action.payload; },
  },
});

const portfolioSlice = createSlice({
  name: 'portfolio',
  initialState: { positions: {}, marketValues: {}, pnl: {} },
  reducers: {
    setPositions: (state, action) => {
      state.positions[action.payload.portfolio] = action.payload.positions;
    },
    setMarketValue: (state, action) => {
      state.marketValues[action.payload.portfolio] = action.payload.value;
    },
    setPnl: (state, action) => {
      state.pnl[action.payload.portfolio] = action.payload.value;
    },
  },
});

const riskSlice = createSlice({
  name: 'risk',
  initialState: { limits: [], varResults: {} },
  reducers: {
    setLimits: (state, action) => { state.limits = action.payload; },
    setVaRResult: (state, action) => {
      state.varResults[action.payload.key] = action.payload.value;
    },
  },
});

export const store = configureStore({
  reducer: {
    trades: tradesSlice.reducer,
    portfolio: portfolioSlice.reducer,
    risk: riskSlice.reducer,
  },
});

export const { setTrades, setLoading, setError } = tradesSlice.actions;
export const { setPositions, setMarketValue, setPnl } = portfolioSlice.actions;
export const { setLimits, setVaRResult } = riskSlice.actions;
