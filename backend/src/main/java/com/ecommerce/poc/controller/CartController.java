package com.ecommerce.poc.controller;

import com.ecommerce.poc.dto.CartResponse;
import com.ecommerce.poc.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public ResponseEntity<CartResponse> cart(@RequestParam(defaultValue = "standard") String shippingMethod) {
        return ResponseEntity.ok(cartService.getCart(shippingMethod));
    }

    @PutMapping("/cart/items/{productId}")
    public ResponseEntity<CartResponse> setItem(@PathVariable String productId, @RequestBody Map<String, Object> body) {
        Object quantityObject = body.get("quantity");
        int quantity = quantityObject == null ? 0 : ((Number) quantityObject).intValue();
        return ResponseEntity.ok(cartService.setCartItem(productId, quantity));
    }
}
