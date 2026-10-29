package com.architect.interviewportal.repository;

import com.architect.interviewportal.entity.TopicEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicEntitlementRepository extends JpaRepository<TopicEntitlement, Long> {
    List<TopicEntitlement> findByUserEmail(String userEmail);
    boolean existsByUserEmailAndTopicId(String userEmail, String topicId);
    boolean existsByUserEmailAndIsBundleTrue(String userEmail);
}
