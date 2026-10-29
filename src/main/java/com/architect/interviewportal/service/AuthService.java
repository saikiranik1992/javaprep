package com.architect.interviewportal.service;

import com.architect.interviewportal.entity.UserAccount;
import com.architect.interviewportal.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;

    public AuthService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserAccount register(String fullName, String email, String rawPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalStateException("An account with this email already exists. Please sign in.");
        }

        String salt = generateSalt();
        String hash = hashPassword(rawPassword, salt);
        String cleanName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : cleanEmail.split("@")[0];

        UserAccount user = new UserAccount(cleanEmail, cleanName, hash, salt);
        return userRepository.save(user);
    }

    @Transactional
    public UserAccount login(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String cleanEmail = email.trim().toLowerCase();
        UserAccount user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isActive()) {
            throw new IllegalStateException("This account has been deactivated");
        }

        if (user.getPasswordHash() != null && user.getSalt() != null) {
            String computedHash = hashPassword(rawPassword, user.getSalt());
            if (!computedHash.equals(user.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid email or password");
            }
        }

        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
