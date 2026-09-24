package com.ecommerce.poc.controller;

import com.ecommerce.poc.dto.ChatbotRequest;
import com.ecommerce.poc.dto.ChatbotResponse;
import com.ecommerce.poc.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chatbot/message")
    public ResponseEntity<ChatbotResponse> message(@Valid @RequestBody ChatbotRequest request) {
        return ResponseEntity.ok(chatbotService.answer(request.getMessage()));
    }
}
