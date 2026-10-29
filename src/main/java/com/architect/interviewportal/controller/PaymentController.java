package com.architect.interviewportal.controller;

import com.architect.interviewportal.service.EntitlementService;
import com.architect.interviewportal.service.RazorpayPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final RazorpayPaymentService paymentService;
    private final EntitlementService entitlementService;

    @Value("${pricing.topic-rupees:49}")
    private int topicRupees;

    @Value("${pricing.bundle-rupees:499}")
    private int bundleRupees;

    public PaymentController(RazorpayPaymentService paymentService, EntitlementService entitlementService) {
        this.paymentService = paymentService;
        this.entitlementService = entitlementService;
    }

    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricing() {
        return ResponseEntity.ok(Map.of(
                "topicRupees", topicRupees,
                "bundleRupees", bundleRupees,
                "currency", "INR",
                "totalModules", EntitlementService.ALL_TOPIC_IDS.size()
        ));
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.get("email");
        String topicId = (String) payload.getOrDefault("topicId", "core-java");
        boolean isBundle = Boolean.TRUE.equals(payload.get("isBundle"));

        Map<String, Object> orderDetails = paymentService.createOrder(email, topicId, isBundle);
        return ResponseEntity.ok(orderDetails);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("orderId");
        String paymentId = (String) payload.get("paymentId");
        String signature = (String) payload.get("signature");
        String email = (String) payload.get("email");
        String topicId = (String) payload.getOrDefault("topicId", "core-java");
        boolean isBundle = Boolean.TRUE.equals(payload.get("isBundle"));
        boolean isSandbox = Boolean.TRUE.equals(payload.get("isSandbox"));

        Map<String, Object> result = paymentService.verifyPayment(
                orderId, paymentId, signature, email, topicId, isBundle, isSandbox
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(@RequestParam(name = "email", required = false) String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "email", "",
                    "isBundleUnlocked", false,
                    "unlockedTopics", List.of()
            ));
        }
        String cleanEmail = email.trim().toLowerCase();
        boolean isBundle = entitlementService.isBundleUnlocked(cleanEmail);
        List<String> unlocked = entitlementService.getUnlockedTopicIds(cleanEmail);

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", cleanEmail,
                "isBundleUnlocked", isBundle,
                "unlockedTopics", unlocked
        ));
    }

    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restoreAccess(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required to restore access"));
        }
        String cleanEmail = email.trim().toLowerCase();
        List<String> unlocked = entitlementService.getUnlockedTopicIds(cleanEmail);
        boolean isBundle = entitlementService.isBundleUnlocked(cleanEmail);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "email", cleanEmail,
                "unlockedTopics", unlocked,
                "isBundleUnlocked", isBundle,
                "hasPurchases", !unlocked.isEmpty()
        ));
    }
}
