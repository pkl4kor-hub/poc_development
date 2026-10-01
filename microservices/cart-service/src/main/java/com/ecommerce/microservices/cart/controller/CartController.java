package com.ecommerce.microservices.cart.controller;

import com.ecommerce.microservices.cart.service.CartService;
import com.ecommerce.microservices.common.dto.CartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private String resolveKey(String sessionId, String userId) {
        if (sessionId != null && !sessionId.isBlank()) return sessionId;
        if (userId != null && !userId.isBlank()) return "user-" + userId;
        return "default-session";
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "standard") String shippingMethod) {
        String key = resolveKey(sessionId, userId);
        return ResponseEntity.ok(cartService.getCart(key, shippingMethod));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String productId,
            @RequestBody Map<String, Object> body) {
        String key = resolveKey(sessionId, userId);
        Object qtyObj = body.get("quantity");
        int quantity = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0;
        return ResponseEntity.ok(cartService.setCartItem(key, productId, quantity));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkoutCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String key = resolveKey(sessionId, userId);
        cartService.checkoutCart(key);
        return ResponseEntity.ok().build();
    }
}
