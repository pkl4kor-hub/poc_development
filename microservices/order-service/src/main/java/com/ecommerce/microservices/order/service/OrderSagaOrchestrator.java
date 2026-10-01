package com.ecommerce.microservices.order.service;

import com.ecommerce.microservices.common.dto.*;
import com.ecommerce.microservices.order.entity.IdempotencyRecord;
import com.ecommerce.microservices.order.entity.Order;
import com.ecommerce.microservices.order.entity.OrderItem;
import com.ecommerce.microservices.order.repository.IdempotencyRepository;
import com.ecommerce.microservices.order.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${service.cart.url:http://localhost:8083}")
    private String cartServiceUrl;

    @Value("${service.catalog.url:http://localhost:8082}")
    private String catalogServiceUrl;

    @Value("${service.payment.url:http://localhost:8085}")
    private String paymentServiceUrl;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                 IdempotencyRepository idempotencyRepository,
                                 ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public OrderResponse executeOrderSaga(String sessionKey, String idempotencyKey, CheckoutRequest request) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var prior = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
            if (prior.isPresent()) {
                try {
                    OrderResponse cached = objectMapper.readValue(prior.get().getResponseBody(), OrderResponse.class);
                    cached.setReplayed(true);
                    return cached;
                } catch (Exception ignored) {}
            }
        }

        if ("declined".equalsIgnoreCase(request.getMockPayment())) {
            throw new IllegalArgumentException("Card declined. Choose 'Approve payment' to complete your order.");
        }

        String shippingMethod = request.getShippingMethod() != null ? request.getShippingMethod() : "standard";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Session-Id", sessionKey);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<CartResponse> cartResponse = restTemplate.exchange(
                cartServiceUrl + "/api/cart?shippingMethod=" + shippingMethod, HttpMethod.GET, entity, CartResponse.class);

        CartResponse cart = cartResponse.getBody();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Add a tool to your cart before checking out.");
        }

        String orderId = "WB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order();
        order.setId(orderId);
        order.setSessionId(sessionKey);
        order.setBuyerId(1L);
        order.setStatus("PENDING");
        order.setSubtotal(cart.getSubtotal());
        order.setTax(cart.getTax());
        order.setShipping(cart.getShipping());
        order.setTotal(cart.getTotal());
        order.setCurrency(cart.getCurrency());
        order.setShippingMethod(shippingMethod);
        order.setPaymentStatus("PENDING");
        order.setIdempotencyKey(idempotencyKey);

        try {
            order.setCustomerJson(objectMapper.writeValueAsString(request.getCustomer()));
        } catch (Exception ignored) {}

        List<OrderItem> items = new ArrayList<>();
        List<StockReservationRequest.ItemReservation> reservations = new ArrayList<>();

        for (CartResponse.CartItemResponse ci : cart.getItems()) {
            OrderItem oi = new OrderItem(order, ci.getProduct().getId(), ci.getProduct().getName(),
                    ci.getQuantity(), ci.getProduct().getPrice(), ci.getLineTotal());
            items.add(oi);
            reservations.add(new StockReservationRequest.ItemReservation(ci.getProduct().getId(), ci.getQuantity()));
        }
        order.setItems(items);
        orderRepository.save(order);

        StockReservationRequest stockReq = new StockReservationRequest(orderId, reservations);
        try {
            ResponseEntity<Void> reserveRes = restTemplate.postForEntity(
                    catalogServiceUrl + "/api/catalog/products/reserve", stockReq, Void.class);
            if (!reserveRes.getStatusCode().is2xxSuccessful()) {
                order.setStatus("FAILED");
                orderRepository.save(order);
                throw new IllegalStateException("Insufficient stock to complete order.");
            }
        } catch (Exception ex) {
            order.setStatus("FAILED");
            orderRepository.save(order);
            throw new IllegalStateException("Insufficient stock to complete order.");
        }

        try {
            PaymentChargeRequest payReq = new PaymentChargeRequest(
                    orderId, 1L, cart.getTotal(), "MOCK_CARD", true);
            ResponseEntity<PaymentResponse> payRes = restTemplate.postForEntity(
                    paymentServiceUrl + "/api/payments/charge", payReq, PaymentResponse.class);

            if (!payRes.getStatusCode().is2xxSuccessful() || payRes.getBody() == null
                    || !"SUCCESS".equals(payRes.getBody().getStatus())) {
                restTemplate.postForEntity(catalogServiceUrl + "/api/catalog/products/release", stockReq, Void.class);
                order.setStatus("FAILED");
                order.setPaymentStatus("DECLINED");
                orderRepository.save(order);
                throw new IllegalStateException("Payment was declined.");
            }

            Map<String, Object> paymentMap = Map.of(
                    "reference", payRes.getBody().getTransactionId(),
                    "method", "MOCK_CARD",
                    "status", "approved"
            );
            order.setPaymentJson(objectMapper.writeValueAsString(paymentMap));
        } catch (Exception ex) {
            restTemplate.postForEntity(catalogServiceUrl + "/api/catalog/products/release", stockReq, Void.class);
            order.setStatus("FAILED");
            order.setPaymentStatus("DECLINED");
            orderRepository.save(order);
            throw new IllegalStateException("Payment processing failed: " + ex.getMessage());
        }

        try {
            restTemplate.exchange(cartServiceUrl + "/api/cart/checkout", HttpMethod.POST, entity, Void.class);
        } catch (Exception ignored) {}

        order.setStatus("confirmed");
        order.setPaymentStatus("approved");
        order = orderRepository.save(order);

        OrderResponse res = toResponse(order);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String body = objectMapper.writeValueAsString(res);
                IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, "hash", body, 201);
                idempotencyRepository.save(record);
            } catch (Exception ignored) {}
        }
        return res;
    }

    public List<OrderResponse> getOrdersBySession(String sessionKey) {
        return orderRepository.findBySessionIdOrderByCreatedAtDesc(sessionKey).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(String id) {
        return orderRepository.findById(id).map(this::toResponse).orElse(null);
    }

    public OrderResponse toResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setCreatedAt(order.getCreatedAt());
        res.setStatus(order.getStatus());
        res.setSubtotal(order.getSubtotal());
        res.setTax(order.getTax());
        res.setShipping(order.getShipping());
        res.setTotal(order.getTotal());
        res.setCurrency(order.getCurrency());
        res.setShippingMethod(order.getShippingMethod());
        res.setReplayed(false);

        try {
            if (order.getCustomerJson() != null) {
                res.setCustomer(objectMapper.readValue(order.getCustomerJson(), new TypeReference<Map<String, Object>>() {}));
            }
            if (order.getPaymentJson() != null) {
                res.setPayment(objectMapper.readValue(order.getPaymentJson(), new TypeReference<Map<String, Object>>() {}));
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> itemMaps = order.getItems().stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", i.getProductId());
            m.put("productName", i.getProductName());
            m.put("quantity", i.getQuantity());
            m.put("unitPrice", i.getUnitPrice());
            m.put("lineTotal", i.getLineTotal());
            return m;
        }).collect(Collectors.toList());
        res.setItems(itemMaps);

        return res;
    }
}
