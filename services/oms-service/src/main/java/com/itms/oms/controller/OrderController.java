package com.itms.oms.controller;

import com.itms.oms.model.Order;
import com.itms.oms.service.OrderRoutingService;
import com.itms.oms.service.OrderSlicingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRoutingService orderRoutingService;
    private final OrderSlicingService orderSlicingService;

    @PostMapping("/route")
    public ResponseEntity<String> routeOrder(@Valid @RequestBody Order order) {
        String venue = orderRoutingService.routeOrder(order);
        return ResponseEntity.ok(venue);
    }

    @PostMapping("/slice")
    public ResponseEntity<List<Order>> sliceOrder(@Valid @RequestBody Order order,
                                                   @RequestParam(defaultValue = "10") int slices) {
        List<Order> slicedOrders = orderSlicingService.sliceOrder(order, slices);
        return ResponseEntity.status(HttpStatus.CREATED).body(slicedOrders);
    }

    @PostMapping("/twap")
    public ResponseEntity<List<Order>> twapSlice(@Valid @RequestBody Order order,
                                                  @RequestParam(defaultValue = "30") int intervalMinutes,
                                                  @RequestParam(defaultValue = "390") int durationMinutes) {
        List<Order> slices = orderSlicingService.twapSlice(order, intervalMinutes, durationMinutes);
        return ResponseEntity.status(HttpStatus.CREATED).body(slices);
    }
}
