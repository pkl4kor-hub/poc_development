package com.ecommerce.microservices.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, FAILED, CANCELLED

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal tax;

    @Column(nullable = false)
    private BigDecimal shipping;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(name = "shipping_method")
    private String shippingMethod = "standard";

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String customerJson;

    @Column(columnDefinition = "TEXT")
    private String paymentJson;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private String createdAt = Instant.now().toString();

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getCustomerJson() { return customerJson; }
    public void setCustomerJson(String customerJson) { this.customerJson = customerJson; }

    public String getPaymentJson() { return paymentJson; }
    public void setPaymentJson(String paymentJson) { this.paymentJson = paymentJson; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
