package com.ecommerce.poc.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderResponse {
    private String id;
    private String createdAt;
    private String status;
    private Map<String, Object> customer;
    private Map<String, Object> payment;
    private List<Map<String, Object>> items;
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal total;
    private String currency;
    private String shippingMethod;
    private boolean replayed;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Object> getCustomer() { return customer; }
    public void setCustomer(Map<String, Object> customer) { this.customer = customer; }
    public Map<String, Object> getPayment() { return payment; }
    public void setPayment(Map<String, Object> payment) { this.payment = payment; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }
    public boolean isReplayed() { return replayed; }
    public void setReplayed(boolean replayed) { this.replayed = replayed; }
}
