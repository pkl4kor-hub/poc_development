package com.ecommerce.microservices.common.dto;

import java.time.LocalDateTime;

public class PaymentResponse {
    private String transactionId;
    private String orderId;
    private String status; // SUCCESS, DECLINED, FAILED
    private String message;
    private LocalDateTime timestamp;

    public PaymentResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentResponse(String transactionId, String orderId, String status, String message) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
