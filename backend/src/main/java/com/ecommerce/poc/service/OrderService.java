package com.ecommerce.poc.service;

import com.ecommerce.poc.dto.CheckoutRequest;
import com.ecommerce.poc.dto.OrderResponse;
import com.ecommerce.poc.entity.*;
import com.ecommerce.poc.exception.ConflictException;
import com.ecommerce.poc.exception.ResourceNotFoundException;
import com.ecommerce.poc.exception.ValidationException;
import com.ecommerce.poc.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("15000");
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("850");
    private static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("1800");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, CartRepository cartRepository,
                        CartItemRepository cartItemRepository, UserRepository userRepository,
                        IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper, SessionService sessionService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
    }

    @Transactional
    public OrderResponse createOrder(CheckoutRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || !idempotencyKey.matches("[a-zA-Z0-9-]{16,100}")) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "A valid Idempotency-Key header is required.");
        }
        ensureCustomer(request.getCustomer());
        if (!"approved".equals(request.getMockPayment()) && !"declined".equals(request.getMockPayment())) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT", "Choose a mock payment outcome.");
        }
        if (request.getExpectedTotal() == null || request.getExpectedTotal() < 0) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_TOTAL", "Refresh your order total before paying.");
        }
        String shippingMethod = request.getShippingMethod() == null || request.getShippingMethod().isBlank() ? "standard" : request.getShippingMethod();
        String fingerprint = sha256(request.getCustomer(), shippingMethod, request.getMockPayment(), request.getExpectedTotal());

        User buyer = currentUser();
        Optional<IdempotencyRecord> prior = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (prior.isPresent()) {
            if (!prior.get().getRequestHash().equals(fingerprint)) {
                throw new ConflictException("IDEMPOTENCY_CONFLICT", "This payment reference was already used for a different request.");
            }
            return parseResponse(prior.get().getResponseBody(), true);
        }

        Cart cart = cartRepository.findByBuyerIdAndStatus(buyer.getId(), "ACTIVE")
                .orElseThrow(() -> new ValidationException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "Add a tool to your cart before checking out."));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "Add a tool to your cart before checking out.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "This tool could not be found."));
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new ValidationException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Stock has changed. Update the quantities in your cart.");
            }
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setSellerId(product.getSellerId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setLineTotal(lineTotal);
            orderItems.add(orderItem);
        }

        BigDecimal shipping = resolveShipping(subtotal, shippingMethod);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shipping).add(tax);
        if (total.longValue() != request.getExpectedTotal().longValue()) {
            throw new ValidationException(HttpStatus.CONFLICT, "TOTAL_CHANGED", "Your total has changed. Review your cart before paying.");
        }
        if (!"approved".equals(request.getMockPayment())) {
            throw new ValidationException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED", "Mock payment declined. No order was created and your cart is unchanged.");
        }

        String orderId = "WB-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(buyer.getId());
        order.setStatus("confirmed");
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShipping(shipping);
        order.setTotal(total);
        order.setPaymentStatus("paid");
        order.setIdempotencyKey(idempotencyKey);
        order.setCreatedAt(Instant.now().toString());

        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            int updated = product.getStockQuantity() - item.getQuantity();
            if (updated < 0) {
                throw new ValidationException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Stock changed while checking out. Please review your cart.");
            }
            product.setStockQuantity(updated);
            productRepository.save(product);
            item.setOrder(order);
        }
        order.setItems(orderItems);
        orderRepository.save(order);

        cartItemRepository.deleteByCartId(cart.getId());
        cart.setStatus("CHECKED_OUT");
        cartRepository.save(cart);

        String responseBody = toJson(createOrderResponse(order, request.getCustomer(), shippingMethod, false));
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(fingerprint);
        record.setResponseBody(responseBody);
        record.setResponseStatus(HttpStatus.CREATED.value());
        idempotencyRepository.save(record);

        return createOrderResponse(order, request.getCustomer(), shippingMethod, false);
    }

    public List<OrderResponse> listOrders() {
        User buyer = currentUser();
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream()
                .map(order -> createOrderResponse(order, Map.of(), "standard", false))
                .toList();
    }

    public OrderResponse getOrder(String orderId) {
        User buyer = currentUser();
        return createOrderResponse(orderRepository.findByBuyerIdAndId(buyer.getId(), orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "This order could not be found in your local session.")), Map.of(), "standard", false);
    }

    private void ensureCustomer(Map<String, Object> customer) {
        if (customer == null || customer.isEmpty()) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_CUSTOMER", "Enter your delivery details.");
        }
        String[] fields = {"firstName", "lastName", "email", "address", "city", "postalCode", "country"};
        for (String field : fields) {
            Object value = customer.get(field);
            if (!(value instanceof String text) || text.isBlank() || text.length() > 200) {
                throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_CUSTOMER", "Enter a valid " + field + ".");
            }
            customer.put(field, text.trim());
        }
        String email = (String) customer.get("email");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "Enter a valid email address.");
        }
        if (!"US".equalsIgnoreCase((String) customer.get("country"))) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_COUNTRY", "Choose a supported delivery country.");
        }
        String postalCode = (String) customer.get("postalCode");
        if (!postalCode.matches("^\\d{5}(-\\d{4})?$")) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_POSTAL_CODE", "Enter a valid US ZIP code.");
        }
    }

    private BigDecimal resolveShipping(BigDecimal subtotal, String shippingMethod) {
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 && "standard".equalsIgnoreCase(shippingMethod)) {
            return BigDecimal.ZERO;
        }
        return "express".equalsIgnoreCase(shippingMethod) ? EXPRESS_SHIPPING : STANDARD_SHIPPING;
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            username = auth.getName();
        }
        if (username == null) {
            String sessionId = null;
            if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                sessionId = attrs.getRequest().getHeader("X-Session-Id");
            }
            if (sessionId != null && !sessionId.isBlank()) {
                username = sessionService.resolveUsername(sessionId);
            }
        }
        if (username == null) {
            throw new ValidationException(HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED", "Create a local shopping session first.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException(HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED", "Create a local shopping session first."));
    }

    private String sha256(Map<String, Object> customer, String shippingMethod, String mockPayment, Long expectedTotal) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "customer", customer,
                    "shippingMethod", shippingMethod,
                    "mockPayment", mockPayment,
                    "expectedTotal", expectedTotal
            ));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Unable to validate your order.");
        }
    }

    private OrderResponse createOrderResponse(Order order, Map<String, Object> customer, String shippingMethod, boolean replayed) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCreatedAt(order.getCreatedAt());
        response.setStatus(order.getStatus());
        response.setCustomer(customer);
        response.setPayment(Map.of("provider", "mock", "status", "paid", "reference", "MOCK-" + UUID.randomUUID()));
        response.setItems(order.getItems().stream().map(item -> {
            Map<String, Object> value = new HashMap<>();
            value.put("productId", item.getProductId());
            value.put("name", item.getProductName());
            value.put("subtitle", "");
            value.put("image", "/images/" + item.getProductId() + ".png");
            value.put("sku", "");
            value.put("unitPrice", item.getUnitPrice());
            value.put("quantity", item.getQuantity());
            value.put("lineTotal", item.getLineTotal());
            return value;
        }).collect(Collectors.toList()));
        response.setSubtotal(order.getSubtotal());
        response.setShipping(order.getShipping());
        response.setTax(order.getTax());
        response.setTotal(order.getTotal());
        response.setCurrency("USD");
        response.setShippingMethod(shippingMethod);
        response.setReplayed(replayed);
        return response;
    }

    private OrderResponse parseResponse(String jsonBody, boolean replayed) {
        try {
            OrderResponse response = objectMapper.readValue(jsonBody, OrderResponse.class);
            response.setReplayed(replayed);
            return response;
        } catch (Exception exception) {
            throw new ValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "REPLAY_ERROR", "Unable to replay the previous order.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ValidationException(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_SERIALIZATION_ERROR", "Unable to save this order.");
        }
    }
}
