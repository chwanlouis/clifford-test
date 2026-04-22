package com.itms.oms.model;

public enum ExecutionAlgorithm {
    NONE,
    TWAP,
    VWAP,
    POV,        // Percentage of Volume
    IS,         // Implementation Shortfall
    ARRIVAL_PRICE,
    DARK_POOL,
    SMART_ROUTE
}
