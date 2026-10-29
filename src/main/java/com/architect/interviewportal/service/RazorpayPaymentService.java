package com.architect.interviewportal.service;

import com.architect.interviewportal.entity.PaymentOrder;
import com.architect.interviewportal.repository.PaymentOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    private final PaymentOrderRepository orderRepository;
    private final EntitlementService entitlementService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${razorpay.key-id:test_key_sample}")
    private String keyId;

    @Value("${razorpay.key-secret:test_secret_sample}")
    private String keySecret;

    @Value("${razorpay.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${pricing.topic-rupees:49}")
    private int topicRupees;

    @Value("${pricing.bundle-rupees:499}")
    private int bundleRupees;

    public RazorpayPaymentService(PaymentOrderRepository orderRepository,
                                  EntitlementService entitlementService,
                                  ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.entitlementService = entitlementService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Transactional
    public Map<String, Object> createOrder(String email, String topicId, boolean isBundle) {
        int rupees = isBundle ? bundleRupees : topicRupees;
        int paise = rupees * 100;
        String cleanEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : "guest@javaprep.local";
        String effectiveTopic = isBundle ? "all-access-bundle" : topicId;

        boolean isSandbox = mockEnabled || keyId == null || keyId.startsWith("test_key") || keyId.isBlank();
        String generatedOrderId;

        if (isSandbox) {
            generatedOrderId = "order_demo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("Generating Sandbox / Demo Order: {} for user: {} (₹{})", generatedOrderId, cleanEmail, rupees);
        } else {
            generatedOrderId = callRazorpayCreateOrderApi(paise, "Receipt_" + System.currentTimeMillis());
        }

        PaymentOrder order = new PaymentOrder(generatedOrderId, cleanEmail, effectiveTopic, isBundle, rupees);
        orderRepository.save(order);

        return Map.of(
                "orderId", generatedOrderId,
                "amountRupees", rupees,
                "amountPaise", paise,
                "currency", "INR",
                "keyId", isSandbox ? "rzp_test_demo_mode" : keyId,
                "isSandbox", isSandbox,
                "topicId", effectiveTopic,
                "isBundle", isBundle,
                "userEmail", cleanEmail
        );
    }

    @Transactional
    public Map<String, Object> verifyPayment(String orderId,
                                            String paymentId,
                                            String signature,
                                            String email,
                                            String topicId,
                                            boolean isBundle,
                                            boolean isSandbox) {
        PaymentOrder order = orderRepository.findByOrderId(orderId).orElse(null);

        String cleanEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : 
                (order != null ? order.getUserEmail() : "guest@javaprep.local");

        boolean isEffectiveSandbox = isSandbox || mockEnabled || keySecret == null || keySecret.startsWith("test_secret");

        if (!isEffectiveSandbox) {
            boolean valid = verifyHmacSha256(orderId + "|" + paymentId, signature, keySecret);
            if (!valid) {
                log.warn("Invalid Razorpay payment signature for order: {}", orderId);
                if (order != null) {
                    order.setStatus("FAILED");
                    orderRepository.save(order);
                }
                return Map.of("success", false, "message", "Invalid payment signature verification failed");
            }
        }

        if (order != null) {
            order.setStatus("PAID");
            order.setRazorpayPaymentId(paymentId != null ? paymentId : "pay_demo_" + System.currentTimeMillis());
            order.setRazorpaySignature(signature != null ? signature : "sig_demo_verified");
            order.setCompletedAt(Instant.now());
            orderRepository.save(order);
        }

        if (isBundle || "all-access-bundle".equalsIgnoreCase(topicId)) {
            entitlementService.grantBundle(cleanEmail);
        } else {
            entitlementService.grantTopic(cleanEmail, topicId);
        }

        return Map.of(
                "success", true,
                "message", isBundle ? "All 20 topics unlocked successfully!" : "Topic unlocked successfully!",
                "orderId", orderId,
                "userEmail", cleanEmail,
                "unlockedTopics", entitlementService.getUnlockedTopicIds(cleanEmail),
                "isBundleUnlocked", entitlementService.isBundleUnlocked(cleanEmail)
        );
    }

    private String callRazorpayCreateOrderApi(int paise, String receipt) {
        try {
            String url = "https://api.razorpay.com/v1/orders";
            String jsonPayload = objectMapper.writeValueAsString(Map.of(
                    "amount", paise,
                    "currency", "INR",
                    "receipt", receipt,
                    "payment_capture", 1
            ));

            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<?, ?> map = objectMapper.readValue(response.body(), Map.class);
                return (String) map.get("id");
            } else {
                throw new RuntimeException("Razorpay Order creation failed: " + response.body());
            }
        } catch (Exception e) {
            log.error("Exception calling Razorpay API: {}", e.getMessage());
            return "order_fallback_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }
    }

    public static boolean verifyHmacSha256(String data, String signature, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(generated.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
