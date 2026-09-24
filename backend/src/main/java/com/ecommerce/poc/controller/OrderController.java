package com.ecommerce.poc.controller;

import com.ecommerce.poc.dto.CheckoutRequest;
import com.ecommerce.poc.dto.OrderResponse;
import com.ecommerce.poc.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(order.isReplayed() ? 200 : 201).body(Map.of("order", order, "replayed", order.isReplayed()));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, List<OrderResponse>>> listOrders() {
        return ResponseEntity.ok(Map.of("items", orderService.listOrders()));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
}
