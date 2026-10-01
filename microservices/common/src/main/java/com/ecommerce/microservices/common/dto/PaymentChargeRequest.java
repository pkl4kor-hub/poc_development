package com.ecommerce.microservices.common.dto;

import java.math.BigDecimal;

public class PaymentChargeRequest {
    private String orderId;
    private Long buyerId;
    private BigDecimal amount;
    private String paymentMethod;
    private Boolean mockOutcome; // true = success, false = decline

    public PaymentChargeRequest() {}

    public PaymentChargeRequest(String orderId, Long buyerId, BigDecimal amount, String paymentMethod, Boolean mockOutcome) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.mockOutcome = mockOutcome;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Boolean getMockOutcome() { return mockOutcome; }
    public void setMockOutcome(Boolean mockOutcome) { this.mockOutcome = mockOutcome; }
}
