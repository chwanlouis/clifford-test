package com.itms.oms.service;

import com.itms.oms.model.Order;
import com.itms.oms.model.OrderSide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderSlicingService {

    private static final int DEFAULT_SLICES = 10;

    public List<Order> sliceOrder(Order parentOrder, int numberOfSlices) {
        log.info("Slicing order {} into {} slices", parentOrder.getOrderReference(), numberOfSlices);
        List<Order> slices = new ArrayList<>();
        BigDecimal sliceSize = parentOrder.getTotalQuantity()
                .divide(BigDecimal.valueOf(numberOfSlices), 0, RoundingMode.DOWN);
        BigDecimal remaining = parentOrder.getTotalQuantity();

        for (int i = 0; i < numberOfSlices; i++) {
            BigDecimal qty = (i == numberOfSlices - 1) ? remaining : sliceSize;
            remaining = remaining.subtract(qty);

            Order slice = Order.builder()
                    .instrument(parentOrder.getInstrument())
                    .isin(parentOrder.getIsin())
                    .side(parentOrder.getSide())
                    .orderType(parentOrder.getOrderType())
                    .totalQuantity(qty)
                    .limitPrice(parentOrder.getLimitPrice())
                    .currency(parentOrder.getCurrency())
                    .portfolio(parentOrder.getPortfolio())
                    .trader(parentOrder.getTrader())
                    .algorithm(parentOrder.getAlgorithm())
                    .build();
            slices.add(slice);
        }

        log.info("Created {} slices for order {}", slices.size(), parentOrder.getOrderReference());
        return slices;
    }

    public List<Order> twapSlice(Order parentOrder, int intervalMinutes, int durationMinutes) {
        int numberOfSlices = durationMinutes / intervalMinutes;
        log.info("TWAP slicing order {} into {} slices over {} minutes",
                parentOrder.getOrderReference(), numberOfSlices, durationMinutes);
        return sliceOrder(parentOrder, numberOfSlices);
    }

    public List<Order> vwapSlice(Order parentOrder, List<Double> volumeProfile) {
        log.info("VWAP slicing order {} using volume profile", parentOrder.getOrderReference());
        List<Order> slices = new ArrayList<>();
        double totalVolume = volumeProfile.stream().mapToDouble(Double::doubleValue).sum();

        for (double bucketVolume : volumeProfile) {
            double proportion = bucketVolume / totalVolume;
            BigDecimal qty = parentOrder.getTotalQuantity()
                    .multiply(BigDecimal.valueOf(proportion))
                    .setScale(0, RoundingMode.DOWN);

            Order slice = Order.builder()
                    .instrument(parentOrder.getInstrument())
                    .isin(parentOrder.getIsin())
                    .side(parentOrder.getSide())
                    .orderType(parentOrder.getOrderType())
                    .totalQuantity(qty)
                    .limitPrice(parentOrder.getLimitPrice())
                    .currency(parentOrder.getCurrency())
                    .portfolio(parentOrder.getPortfolio())
                    .trader(parentOrder.getTrader())
                    .algorithm(parentOrder.getAlgorithm())
                    .build();
            slices.add(slice);
        }
        return slices;
    }
}
