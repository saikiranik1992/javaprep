package com.architect.interviewportal.controller;

import com.architect.interviewportal.entity.UserSubscription;
import com.architect.interviewportal.service.SubscriptionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMySubscriptions(HttpSession session) {
        String authEmail = (String) session.getAttribute("AUTH_USER_EMAIL");
        if (authEmail == null || authEmail.isBlank()) {
            return ResponseEntity.ok(Map.of("authenticated", false, "subscriptions", List.of()));
        }

        List<UserSubscription> subs = subscriptionService.getAllSubscriptionsForUser(authEmail);
        List<UserSubscription> active = subscriptionService.getActiveSubscriptionsForUser(authEmail);

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "userEmail", authEmail,
                "totalSubscriptions", subs.size(),
                "activeSubscriptions", active.size(),
                "subscriptions", subs
        ));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAccess(@RequestParam String email,
                                                          @RequestParam(defaultValue = "core-java") String topicId) {
        boolean hasAccess = subscriptionService.hasActiveAccess(email, topicId);
        return ResponseEntity.ok(Map.of(
                "userEmail", email,
                "topicId", topicId,
                "hasAccess", hasAccess
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserSubscription>> getAllActiveSubscribers() {
        return ResponseEntity.ok(subscriptionService.getAllActiveSubscribers());
    }

    @PostMapping("/create")
    public ResponseEntity<UserSubscription> createSubscription(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.get("email");
        String name = (String) payload.get("fullName");
        String planType = (String) payload.getOrDefault("planType", "TOPIC_ACCESS");
        String topicId = (String) payload.getOrDefault("topicId", "core-java");
        Integer amount = (Integer) payload.getOrDefault("amountPaidRupees", 49);
        Integer durationDays = (Integer) payload.get("durationDays");

        UserSubscription sub = subscriptionService.createSubscription(email, name, planType, topicId, amount, durationDays);
        return ResponseEntity.ok(sub);
    }
}
