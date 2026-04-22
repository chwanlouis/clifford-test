package com.itms.oms.service;

import com.itms.oms.model.ExecutionAlgorithm;
import com.itms.oms.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OrderRoutingService {

    private static final Map<String, String> VENUE_ROUTING = new HashMap<>();

    static {
        VENUE_ROUTING.put("US", "NYSE");
        VENUE_ROUTING.put("GB", "LSE");
        VENUE_ROUTING.put("DE", "XETRA");
        VENUE_ROUTING.put("JP", "TSE");
        VENUE_ROUTING.put("HK", "HKEX");
    }

    public String routeOrder(Order order) {
        String venue = determineVenue(order);
        log.info("Routing order {} to venue {}", order.getOrderReference(), venue);
        return venue;
    }

    private String determineVenue(Order order) {
        if (order.getAlgorithm() == ExecutionAlgorithm.DARK_POOL) {
            return "DARK_POOL_" + extractCountryCode(order.getIsin());
        }
        if (order.getAlgorithm() == ExecutionAlgorithm.SMART_ROUTE) {
            return smartRoute(order);
        }
        String countryCode = extractCountryCode(order.getIsin());
        return VENUE_ROUTING.getOrDefault(countryCode, "NYSE");
    }

    private String smartRoute(Order order) {
        // Smart Order Router: chooses best venue based on liquidity and price
        log.info("Smart routing order {}", order.getOrderReference());
        String countryCode = extractCountryCode(order.getIsin());
        String primaryVenue = VENUE_ROUTING.getOrDefault(countryCode, "NYSE");
        // In production: query market data for best bid/offer across venues
        return primaryVenue;
    }

    private String extractCountryCode(String isin) {
        if (isin != null && isin.length() >= 2) {
            return isin.substring(0, 2);
        }
        return "US";
    }
}
