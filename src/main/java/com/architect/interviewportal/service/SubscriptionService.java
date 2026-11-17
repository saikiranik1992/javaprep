package com.architect.interviewportal.service;

import com.architect.interviewportal.entity.UserSubscription;
import com.architect.interviewportal.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final UserSubscriptionRepository subscriptionRepository;

    public SubscriptionService(UserSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public UserSubscription createSubscription(String userEmail, String fullName, String planType, String topicId, Integer amountRupees, Integer durationDays) {
        Instant now = Instant.now();
        Instant endDate = (durationDays != null && durationDays > 0) ? now.plus(durationDays, ChronoUnit.DAYS) : null;

        UserSubscription sub = new UserSubscription(userEmail, fullName, planType, topicId, amountRupees, now, endDate);
        UserSubscription saved = subscriptionRepository.save(sub);
        log.info("Created subscription #{} for {} (Plan: {}, Topic: {}, Expires: {})", saved.getId(), userEmail, planType, topicId, endDate);
        return saved;
    }

    public boolean hasActiveAccess(String userEmail, String topicId) {
        if (userEmail == null || userEmail.isBlank()) return false;
        Instant now = Instant.now();
        List<UserSubscription> valid = subscriptionRepository.findValidSubscriptionsForTopic(userEmail.trim(), topicId, now);
        return !valid.isEmpty();
    }

    public List<UserSubscription> getActiveSubscriptionsForUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) return List.of();
        return subscriptionRepository.findActiveSubscriptionsForUser(userEmail.trim(), Instant.now());
    }

    public List<UserSubscription> getAllSubscriptionsForUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) return List.of();
        return subscriptionRepository.findByUserEmailOrderByStartDateDesc(userEmail.trim().toLowerCase());
    }

    public List<UserSubscription> getAllActiveSubscribers() {
        return subscriptionRepository.findByStatusOrderByStartDateDesc("ACTIVE");
    }

    @Transactional
    public UserSubscription cancelSubscription(Long subscriptionId) {
        UserSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        sub.setStatus("CANCELLED");
        sub.setUpdatedAt(Instant.now());
        log.info("Cancelled subscription #{}", subscriptionId);
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public UserSubscription extendSubscription(Long subscriptionId, int additionalDays) {
        UserSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        Instant currentEnd = sub.getEndDate() != null ? sub.getEndDate() : Instant.now();
        Instant newEnd = currentEnd.plus(additionalDays, ChronoUnit.DAYS);
        sub.setEndDate(newEnd);
        sub.setStatus("ACTIVE");
        sub.setUpdatedAt(Instant.now());
        log.info("Extended subscription #{} to {}", subscriptionId, newEnd);
        return subscriptionRepository.save(sub);
    }
}
