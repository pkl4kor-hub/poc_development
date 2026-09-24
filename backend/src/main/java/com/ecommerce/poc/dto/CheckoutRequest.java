package com.ecommerce.poc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class CheckoutRequest {
    @NotNull(message = "Customer details are required.")
    @Valid
    private Map<String, Object> customer;

    @NotBlank(message = "Delivery method is required.")
    private String shippingMethod;

    @NotBlank(message = "Payment outcome is required.")
    private String mockPayment;

    private Long expectedTotal;

    public Map<String, Object> getCustomer() { return customer; }
    public void setCustomer(Map<String, Object> customer) { this.customer = customer; }
    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }
    public String getMockPayment() { return mockPayment; }
    public void setMockPayment(String mockPayment) { this.mockPayment = mockPayment; }
    public Long getExpectedTotal() { return expectedTotal; }
    public void setExpectedTotal(Long expectedTotal) { this.expectedTotal = expectedTotal; }
}
