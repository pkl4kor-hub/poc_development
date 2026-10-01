package com.ecommerce.microservices.cart.service;

import com.ecommerce.microservices.cart.entity.Cart;
import com.ecommerce.microservices.cart.entity.CartItem;
import com.ecommerce.microservices.cart.repository.CartRepository;
import com.ecommerce.microservices.common.dto.CartResponse;
import com.ecommerce.microservices.common.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("15000");
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("850");
    private static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("1800");

    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;

    @Value("${service.catalog.url:http://localhost:8082}")
    private String catalogServiceUrl;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
        this.restTemplate = new RestTemplate();
    }

    private Cart getOrCreateActiveCart(String sessionKey) {
        return cartRepository.findBySessionIdAndStatus(sessionKey, "ACTIVE")
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setSessionId(sessionKey);
                    c.setBuyerId(1L);
                    c.setStatus("ACTIVE");
                    return cartRepository.save(c);
                });
    }

    @Transactional
    public CartResponse getCart(String sessionKey, String shippingMethod) {
        Cart cart = getOrCreateActiveCart(sessionKey);
        return buildCartResponse(cart, shippingMethod);
    }

    @Transactional
    public CartResponse setCartItem(String sessionKey, String productId, int quantity) {
        Cart cart = getOrCreateActiveCart(sessionKey);

        if (quantity <= 0) {
            cart.getItems().removeIf(i -> i.getProductId().equals(productId));
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
            return buildCartResponse(cart, "standard");
        }

        ProductResponse product = fetchProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
        } else {
            CartItem newItem = new CartItem(cart, productId, product.getName(), quantity, product.getPrice());
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return buildCartResponse(cart, "standard");
    }

    @Transactional
    public void checkoutCart(String sessionKey) {
        cartRepository.findBySessionIdAndStatus(sessionKey, "ACTIVE").ifPresent(c -> {
            c.setStatus("CHECKED_OUT");
            c.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(c);
        });
    }

    private ProductResponse fetchProduct(String productId) {
        try {
            return restTemplate.getForObject(catalogServiceUrl + "/api/products/" + productId, ProductResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private CartResponse buildCartResponse(Cart cart, String shippingMethod) {
        if (shippingMethod == null || shippingMethod.isBlank()) {
            shippingMethod = "standard";
        }

        List<CartResponse.CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int itemCount = 0;
        boolean allAvailable = true;

        for (CartItem item : cart.getItems()) {
            ProductResponse product = fetchProduct(item.getProductId());
            boolean available = product != null && product.getStock() >= item.getQuantity();
            if (!available) {
                allAvailable = false;
            }

            BigDecimal unitPrice = product != null ? product.getPrice() : item.getUnitPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            itemCount += item.getQuantity();

            CartResponse.CartItemResponse cr = new CartResponse.CartItemResponse();
            cr.setProduct(product);
            cr.setQuantity(item.getQuantity());
            cr.setLineTotal(lineTotal);
            cr.setAvailable(available);
            itemResponses.add(cr);
        }

        BigDecimal shipping = BigDecimal.ZERO;
        if (!itemResponses.isEmpty()) {
            if ("express".equalsIgnoreCase(shippingMethod)) {
                shipping = EXPRESS_SHIPPING;
            } else {
                shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING;
            }
        }

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shipping).add(tax);

        CartResponse response = new CartResponse();
        response.setItems(itemResponses);
        response.setItemCount(itemCount);
        response.setSubtotal(subtotal);
        response.setShipping(shipping);
        response.setTax(tax);
        response.setTotal(total);
        response.setCurrency("USD");
        response.setShippingMethod(shippingMethod);
        response.setCanCheckout(!itemResponses.isEmpty() && allAvailable);
        return response;
    }
}
