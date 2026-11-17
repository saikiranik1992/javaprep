package com.architect.interviewportal.service;

import com.architect.interviewportal.entity.TopicEntitlement;
import com.architect.interviewportal.entity.UserAccount;
import com.architect.interviewportal.repository.TopicEntitlementRepository;
import com.architect.interviewportal.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EntitlementService {

    public static final List<String> ALL_TOPIC_IDS = List.of(
            "core-java", "java-versions", "java-threads", "java-memory-model",
            "jvm-gc", "design-patterns", "coding-interviews", "spring-boot",
            "spring-security", "hibernate-jpa", "junit-mockito", "microservices",
            "system-design", "spring-kafka", "spring-redis", "sql-notes",
            "docker", "kubernetes", "aws-cloud", "git-workflows"
    );

    private final TopicEntitlementRepository entitlementRepository;
    private final UserAccountRepository userRepository;

    public EntitlementService(TopicEntitlementRepository entitlementRepository, UserAccountRepository userRepository) {
        this.entitlementRepository = entitlementRepository;
        this.userRepository = userRepository;
    }

    public boolean isTopicUnlocked(String email, String topicId) {
        if (email == null || email.isBlank()) return false;
        String cleanEmail = email.trim().toLowerCase();
        if (entitlementRepository.existsByUserEmailAndIsBundleTrue(cleanEmail)) return true;
        return entitlementRepository.existsByUserEmailAndTopicId(cleanEmail, topicId);
    }

    public boolean isBundleUnlocked(String email) {
        if (email == null || email.isBlank()) return false;
        return entitlementRepository.existsByUserEmailAndIsBundleTrue(email.trim().toLowerCase());
    }

    public List<String> getUnlockedTopicIds(String email) {
        if (email == null || email.isBlank()) return List.of();
        String cleanEmail = email.trim().toLowerCase();
        if (entitlementRepository.existsByUserEmailAndIsBundleTrue(cleanEmail)) return ALL_TOPIC_IDS;
        return entitlementRepository.findByUserEmail(cleanEmail)
                .stream()
                .map(TopicEntitlement::getTopicId)
                .filter(ALL_TOPIC_IDS::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void grantTopic(String email, String topicId) {
        if (email == null || email.isBlank()) return;
        String cleanEmail = email.trim().toLowerCase();
        ensureUserExists(cleanEmail);

        if (!entitlementRepository.existsByUserEmailAndTopicId(cleanEmail, topicId)) {
            entitlementRepository.save(new TopicEntitlement(cleanEmail, topicId, false));
        }
    }

    @Transactional
    public void grantBundle(String email) {
        if (email == null || email.isBlank()) return;
        String cleanEmail = email.trim().toLowerCase();
        ensureUserExists(cleanEmail);

        if (!entitlementRepository.existsByUserEmailAndIsBundleTrue(cleanEmail)) {
            entitlementRepository.save(new TopicEntitlement(cleanEmail, "ALL_BUNDLE", true));
        }
        Set<String> existing = entitlementRepository.findByUserEmail(cleanEmail)
                .stream().map(TopicEntitlement::getTopicId).collect(Collectors.toSet());
        for (String tid : ALL_TOPIC_IDS) {
            if (!existing.contains(tid)) {
                entitlementRepository.save(new TopicEntitlement(cleanEmail, tid, true));
            }
        }
    }

    private void ensureUserExists(String email) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(new UserAccount(email));
        }
    }
}
