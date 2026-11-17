package com.architect.interviewportal.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_orders", indexes = {
    @Index(name = "idx_order_id", columnList = "orderId", unique = true),
    @Index(name = "idx_order_email", columnList = "userEmail")
})
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false, length = 150)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String topicId;

    @Column(nullable = false)
    private boolean isBundle;

    @Column(nullable = false)
    private Integer amountRupees;

    @Column(nullable = false)
    private Integer amountPaise;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    private String status = "CREATED";

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(length = 200)
    private String razorpaySignature;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant completedAt;

    public PaymentOrder() {}

    public PaymentOrder(String orderId, String userEmail, String topicId, boolean isBundle, Integer amountRupees) {
        this.orderId = orderId;
        this.userEmail = userEmail.trim().toLowerCase();
        this.topicId = topicId;
        this.isBundle = isBundle;
        this.amountRupees = amountRupees;
        this.amountPaise = amountRupees * 100;
        this.currency = "INR";
        this.status = "CREATED";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail.trim().toLowerCase(); }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public boolean isBundle() { return isBundle; }
    public void setBundle(boolean bundle) { isBundle = bundle; }
    public Integer getAmountRupees() { return amountRupees; }
    public void setAmountRupees(Integer amountRupees) { this.amountRupees = amountRupees; }
    public Integer getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Integer amountPaise) { this.amountPaise = amountPaise; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
