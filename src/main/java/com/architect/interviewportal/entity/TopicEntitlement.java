package com.architect.interviewportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "topic_entitlements", indexes = {
    @Index(name = "idx_entitlement_user", columnList = "userEmail"),
    @Index(name = "idx_entitlement_user_topic", columnList = "userEmail, topicId")
})
public class TopicEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String topicId;

    @Column(nullable = false)
    private boolean isBundle;

    @Column(nullable = false, updatable = false)
    private Instant grantedAt = Instant.now();

    public TopicEntitlement() {}

    public TopicEntitlement(String userEmail, String topicId, boolean isBundle) {
        this.userEmail = userEmail.trim().toLowerCase();
        this.topicId = topicId;
        this.isBundle = isBundle;
        this.grantedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail.trim().toLowerCase(); }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public boolean isBundle() { return isBundle; }
    public void setBundle(boolean bundle) { isBundle = bundle; }
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
}
