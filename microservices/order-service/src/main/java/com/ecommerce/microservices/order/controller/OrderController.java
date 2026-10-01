package com.ecommerce.microservices.order.controller;

import com.ecommerce.microservices.common.dto.CheckoutRequest;
import com.ecommerce.microservices.common.dto.OrderResponse;
import com.ecommerce.microservices.order.service.OrderSagaOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;

    public OrderController(OrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    private String resolveKey(String sessionId, String userId) {
        if (sessionId != null && !sessionId.isBlank()) return sessionId;
        if (userId != null && !userId.isBlank()) return "user-" + userId;
        return "default-session";
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CheckoutRequest request) {
        try {
            String sessionKey = resolveKey(sessionId, userId);
            OrderResponse order = orchestrator.executeOrderSaga(sessionKey, idempotencyKey, request);
            return ResponseEntity.status(order.isReplayed() ? HttpStatus.OK : HttpStatus.CREATED)
                    .body(Map.of("order", order, "replayed", order.isReplayed()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", Map.of("message", e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", Map.of("message", "Order creation failed: " + e.getMessage())));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, List<OrderResponse>>> listOrders(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String sessionKey = resolveKey(sessionId, userId);
        return ResponseEntity.ok(Map.of("items", orchestrator.getOrdersBySession(sessionKey)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        OrderResponse order = orchestrator.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
}
