package com.ecommerce.microservices.payment.controller;

import com.ecommerce.microservices.common.dto.PaymentChargeRequest;
import com.ecommerce.microservices.common.dto.PaymentResponse;
import com.ecommerce.microservices.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public ResponseEntity<PaymentResponse> charge(@RequestBody PaymentChargeRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        if ("DECLINED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
