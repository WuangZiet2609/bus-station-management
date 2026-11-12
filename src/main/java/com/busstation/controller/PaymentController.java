package com.busstation.controller;

import com.busstation.dto.request.PaymentRequest;
import com.busstation.dto.response.PaymentInitResponse;
import com.busstation.dto.response.PaymentResponse;
import com.busstation.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.util.Map
import java.util.UUID

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<PaymentInitResponse> initPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.initPayment(request));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> params) {
        paymentService.handleVnpayReturn(params);
        String returnUrl = "http://localhost:3000/payment/vnpay-result?" + buildQueryString(params);
        return ResponseEntity.status(302).header("Location", returnUrl).build();
    }

    @PostMapping("/momo/notify")
    public ResponseEntity<Map<String, String>> momoNotify(@RequestBody Map<String, String> params) {
        paymentService.handleMomoNotify(params);
        return ResponseEntity.ok(Map.of("resultCode", "0", "message", "Success"));
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}