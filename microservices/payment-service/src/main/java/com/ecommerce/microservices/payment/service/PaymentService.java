package com.ecommerce.microservices.payment.service;

import com.ecommerce.microservices.common.dto.PaymentChargeRequest;
import com.ecommerce.microservices.common.dto.PaymentResponse;
import com.ecommerce.microservices.payment.entity.PaymentTransaction;
import com.ecommerce.microservices.payment.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;

    public PaymentService(PaymentTransactionRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentChargeRequest request) {
        boolean isSuccess = request.getMockOutcome() == null || request.getMockOutcome();

        String status = isSuccess ? "SUCCESS" : "DECLINED";
        String message = isSuccess ? "Payment approved" : "Payment card declined";
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);

        PaymentTransaction txn = new PaymentTransaction(
                txnId,
                request.getOrderId(),
                request.getBuyerId(),
                request.getAmount(),
                request.getPaymentMethod(),
                status
        );
        paymentRepository.save(txn);

        return new PaymentResponse(txnId, request.getOrderId(), status, message);
    }
}
