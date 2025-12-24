package com.smartvillage.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @PostMapping("/tax")
    public ResponseEntity<?> mockTaxPayment(@RequestBody Map<String, Object> body) {
        // Very small mock: return a fake payment id and status
        return ResponseEntity.ok(Map.of(
                "payment_id", UUID.randomUUID().toString(),
                "order_id", UUID.randomUUID().toString(),
                "status", "pending",
                "amount", body.getOrDefault("amount", 0)
        ));
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<?> getStatus(@PathVariable String paymentId) {
        return ResponseEntity.ok(Map.of(
                "payment_id", paymentId,
                "status", "success"
        ));
    }
}
