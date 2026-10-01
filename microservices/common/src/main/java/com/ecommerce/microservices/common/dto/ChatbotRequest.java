package com.ecommerce.microservices.common.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatbotRequest {
    @NotBlank(message = "Message is required.")
    private String message;

    public ChatbotRequest() {}
    public ChatbotRequest(String message) { this.message = message; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
