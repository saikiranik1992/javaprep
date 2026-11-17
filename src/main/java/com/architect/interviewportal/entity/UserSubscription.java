package com.architect.interviewportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_subscriptions", indexes = {
    @Index(name = "idx_sub_user_email", columnList = "userEmail"),
    @Index(name = "idx_sub_status_end", columnList = "status, endDate"),
    @Index(name = "idx_sub_user_topic", columnList = "userEmail, topicId, status")
})
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String userEmail;

    @Column(length = 100)
    private String fullName;

    @Column(nullable = false, length = 50)
    private String planType = "TOPIC_ACCESS"; // TOPIC_ACCESS, COMPLETE_BUNDLE, MONTHLY, ANNUAL, LIFETIME

    @Column(length = 100)
    private String topicId; // specific topic id or "ALL" for bundle

    @Column(nullable = false)
    private Integer amountPaidRupees = 49;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false)
    private Instant startDate = Instant.now();

    @Column
    private Instant endDate; // null means lifetime access; otherwise expiry timestamp

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, CANCELLED

    private boolean autoRenew = false;

    @Column(length = 100)
    private String orderId;

    @Column(length = 100)
    private String paymentId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public UserSubscription() {}

    public UserSubscription(String userEmail, String fullName, String planType, String topicId, Integer amountPaidRupees, Instant startDate, Instant endDate) {
        this.userEmail = userEmail.trim().toLowerCase();
        this.fullName = fullName;
        this.planType = planType;
        this.topicId = topicId;
        this.amountPaidRupees = amountPaidRupees;
        this.startDate = startDate != null ? startDate : Instant.now();
        this.endDate = endDate;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isCurrentlyActive() {
        if (!"ACTIVE".equalsIgnoreCase(this.status)) {
            return false;
        }
        Instant now = Instant.now();
        if (this.startDate != null && now.isBefore(this.startDate)) {
            return false;
        }
        if (this.endDate != null && now.isAfter(this.endDate)) {
            return false;
        }
        return true;
    }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail.trim().toLowerCase(); }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public Integer getAmountPaidRupees() { return amountPaidRupees; }
    public void setAmountPaidRupees(Integer amountPaidRupees) { this.amountPaidRupees = amountPaidRupees; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
