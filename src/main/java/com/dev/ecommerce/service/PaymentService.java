package com.dev.ecommerce.service;

import com.dev.ecommerce.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Map<String, String> MOCK_PAYMENT_URLS = Map.of(
            "CREDIT_CARD", "https://mock-payment.example.com/pay/",
            "BANK_TRANSFER", "https://mock-payment.example.com/transfer/",
            "WALLET", "https://mock-payment.example.com/wallet/"
    );

    /**
     * Creates a mock payment session. In production, this would call
     * a real payment gateway (Stripe, VNPay, PayOS, etc.).
     */
    public String createPaymentSession(Order order) {
        String baseUrl = MOCK_PAYMENT_URLS.getOrDefault(
                order.getPaymentMethod().name(),
                "https://mock-payment.example.com/default/"
        );
        String ref = "PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Mock payment session created for order {}: {}", order.getOrderNumber(), ref);
        return baseUrl + ref;
    }

    /**
     * Verifies a mock payment. In production, this would verify signature/webhook.
     */
    public boolean verifyPayment(String paymentReference) {
        if (paymentReference == null || paymentReference.isBlank()) {
            return false;
        }
        boolean verified = paymentReference.startsWith("PAY-") || paymentReference.startsWith("TXN-");
        log.info("Mock payment verification for ref {}: {}", paymentReference, verified);
        return verified;
    }

    /**
     * Processes refund (mock). In production, calls gateway refund API.
     */
    public boolean processRefund(String paymentReference, BigDecimal amount) {
        if (paymentReference == null || paymentReference.isBlank()) {
            return false;
        }
        String refundRef = "REF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Mock refund processed for {} amount {}: {}", paymentReference, amount, refundRef);
        return true;
    }

    /**
     * Confirms COD payment — cash collected on delivery.
     */
    public boolean confirmCodPayment(Order order) {
        log.info("COD confirmed for order {}", order.getOrderNumber());
        return true;
    }
}
