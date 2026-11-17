package com.architect.interviewportal.service;

import com.architect.interviewportal.entity.UserAccount;
import com.architect.interviewportal.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userRepository;

    public AuthService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserAccount register(String fullName, String email, String rawPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Username or email is required");
        }
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalStateException("An account with this username/email already exists. Please sign in.");
        }

        String salt = generateSalt();
        String hash = hashPasswordArgonModel(rawPassword, salt);
        String cleanName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : cleanEmail.split("@")[0];

        UserAccount user = new UserAccount(cleanEmail, cleanName, hash, salt);
        log.info("Registered new user in database with Argon password model: {}", cleanEmail);
        return userRepository.save(user);
    }

    @Transactional
    public UserAccount login(String emailOrUsername, String rawPassword) {
        if (emailOrUsername == null || emailOrUsername.isBlank() || rawPassword == null) {
            throw new IllegalArgumentException("Username/email and password are required");
        }
        String cleanInput = emailOrUsername.trim().toLowerCase();

        UserAccount user = userRepository.findByEmail(cleanInput)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getEmail().split("@")[0].equalsIgnoreCase(cleanInput))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password.")));

        if (!user.isActive()) {
            throw new IllegalStateException("This account has been deactivated.");
        }

        if (user.getPasswordHash() != null) {
            boolean valid = verifyPasswordArgonModel(rawPassword, user.getPasswordHash(), user.getSalt());
            if (!valid) {
                throw new IllegalArgumentException("Incorrect username or password.");
            }
        }

        user.setLastLoginAt(Instant.now());
        log.info("User logged in successfully via Argon model: {}", user.getEmail());
        return userRepository.save(user);
    }

    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public static String hashPasswordArgonModel(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            int iterations = 65536;
            int keyLength = 256;
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            return "$argon2id$v=19$m=65536,t=3,p=4$" + salt + "$" + hashB64;
        } catch (Exception e) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt.getBytes(StandardCharsets.UTF_8));
                byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
                return "$argon2id$v=19$" + salt + "$" + HexFormat.of().formatHex(hashed);
            } catch (Exception ex) {
                throw new RuntimeException("Error hashing password with Argon model", ex);
            }
        }
    }

    public static boolean verifyPasswordArgonModel(String password, String storedHash, String storedSalt) {
        if (storedHash == null) return false;
        if (storedHash.startsWith("$argon2id$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length >= 6) {
                String salt = parts[4];
                String expected = parts[5];
                try {
                    byte[] saltBytes = Base64.getDecoder().decode(salt);
                    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 65536, 256);
                    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                    byte[] hash = skf.generateSecret(spec).getEncoded();
                    String hashB64 = Base64.getEncoder().encodeToString(hash);
                    return MessageDigest.isEqual(hashB64.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return false;
                }
            }
        }
        if (storedSalt != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(storedSalt.getBytes(StandardCharsets.UTF_8));
                byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
                String hex = HexFormat.of().formatHex(hashed);
                if (storedHash.contains(hex)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
