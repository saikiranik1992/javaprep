package com.architect.interviewportal.controller;

import com.architect.interviewportal.entity.UserAccount;
import com.architect.interviewportal.service.AuthService;
import com.architect.interviewportal.service.EntitlementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EntitlementService entitlementService;

    public AuthController(AuthService authService, EntitlementService entitlementService) {
        this.authService = authService;
        this.entitlementService = entitlementService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> payload, HttpSession session) {
        String fullName = payload.get("fullName");
        String email = payload.get("email");
        String password = payload.get("password");

        try {
            UserAccount user = authService.register(fullName, email, password);
            String token = "tok_" + UUID.randomUUID().toString().replace("-", "");
            session.setAttribute("AUTH_USER_EMAIL", user.getEmail());

            List<String> unlocked = entitlementService.getUnlockedTopicIds(user.getEmail());
            boolean isBundle = entitlementService.isBundleUnlocked(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account registered successfully",
                    "user", Map.of(
                            "id", user.getId(),
                            "fullName", user.getFullName(),
                            "email", user.getEmail()
                    ),
                    "token", token,
                    "unlockedTopics", unlocked,
                    "isBundleUnlocked", isBundle
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String password = payload.get("password");

        try {
            UserAccount user = authService.login(email, password);
            String token = "tok_" + UUID.randomUUID().toString().replace("-", "");
            session.setAttribute("AUTH_USER_EMAIL", user.getEmail());

            List<String> unlocked = entitlementService.getUnlockedTopicIds(user.getEmail());
            boolean isBundle = entitlementService.isBundleUnlocked(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logged in successfully",
                    "user", Map.of(
                            "id", user.getId(),
                            "fullName", user.getFullName(),
                            "email", user.getEmail()
                    ),
                    "token", token,
                    "unlockedTopics", unlocked,
                    "isBundleUnlocked", isBundle
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestParam(name = "email", required = false) String emailParam,
            HttpSession session
    ) {
        String sessionEmail = (String) session.getAttribute("AUTH_USER_EMAIL");
        String email = (emailParam != null && !emailParam.isBlank()) ? emailParam : sessionEmail;

        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        return authService.findByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "authenticated", true,
                        "user", Map.of(
                                "id", user.getId(),
                                "fullName", user.getFullName(),
                                "email", user.getEmail()
                        ),
                        "unlockedTopics", entitlementService.getUnlockedTopicIds(user.getEmail()),
                        "isBundleUnlocked", entitlementService.isBundleUnlocked(user.getEmail())
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of("authenticated", false)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }
}
