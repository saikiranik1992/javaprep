package com.architect.interviewportal.repository;

import com.architect.interviewportal.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserEmailOrderByStartDateDesc(String userEmail);

    List<UserSubscription> findByUserEmailAndStatus(String userEmail, String status);

    List<UserSubscription> findByStatusOrderByStartDateDesc(String status);

    @Query("SELECT s FROM UserSubscription s WHERE s.status = 'ACTIVE' ORDER BY s.startDate DESC")
    List<UserSubscription> findAllActiveSubscriptions();

    @Query("SELECT s FROM UserSubscription s WHERE LOWER(s.userEmail) = LOWER(:email) AND s.status = 'ACTIVE' AND (s.endDate IS NULL OR s.endDate > :now)")
    List<UserSubscription> findActiveSubscriptionsForUser(@Param("email") String email, @Param("now") Instant now);

    @Query("SELECT s FROM UserSubscription s WHERE LOWER(s.userEmail) = LOWER(:email) AND s.status = 'ACTIVE' AND (s.topicId = :topicId OR s.topicId = 'ALL' OR s.planType = 'COMPLETE_BUNDLE') AND (s.endDate IS NULL OR s.endDate > :now)")
    List<UserSubscription> findValidSubscriptionsForTopic(@Param("email") String email, @Param("topicId") String topicId, @Param("now") Instant now);
}
