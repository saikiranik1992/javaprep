-- =============================================================================
-- PostgreSQL Database Schema for JavaPrep on Render (Database: javaprep)
-- Tables: user_accounts, user_subscriptions
-- =============================================================================

-- 1. Table: user_accounts
CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL DEFAULT 'Portal User',
    password_hash VARCHAR(250),
    salt VARCHAR(64),
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_user_email ON user_accounts(email);

-- 2. Table: user_subscriptions (Subscriber List, Start Date, End Date)
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(150) NOT NULL,
    full_name VARCHAR(100),
    plan_type VARCHAR(50) NOT NULL DEFAULT 'TOPIC_ACCESS', -- 'TOPIC_ACCESS', 'COMPLETE_BUNDLE', 'MONTHLY', 'ANNUAL', 'LIFETIME'
    topic_id VARCHAR(100),                                -- Specific topic ID (e.g. 'kubernetes') or 'ALL'
    amount_paid_rupees INTEGER NOT NULL DEFAULT 49,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    start_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP WITH TIME ZONE,                    -- NULL for lifetime access; or expiry timestamp
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',         -- 'ACTIVE', 'EXPIRED', 'CANCELLED'
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    order_id VARCHAR(100),
    payment_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Fast lookup indexes
CREATE INDEX IF NOT EXISTS idx_sub_user_email ON user_subscriptions(user_email);
CREATE INDEX IF NOT EXISTS idx_sub_status_end ON user_subscriptions(status, end_date);
CREATE INDEX IF NOT EXISTS idx_sub_user_topic ON user_subscriptions(user_email, topic_id, status);

-- =============================================================================
-- SAMPLE TEST INSERTS & DATE RANGE QUERIES
-- =============================================================================

-- Example 1: Add a 30-Day Monthly Subscriber
INSERT INTO user_subscriptions (user_email, full_name, plan_type, topic_id, amount_paid_rupees, start_date, end_date, status)
VALUES (
    'subscriber1@example.com',
    'Ramesh Kumar',
    'MONTHLY',
    'ALL',
    499,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    'ACTIVE'
);

-- Example 2: Add an Annual Subscriber (365 Days)
INSERT INTO user_subscriptions (user_email, full_name, plan_type, topic_id, amount_paid_rupees, start_date, end_date, status)
VALUES (
    'architect@example.com',
    'Priya Sharma',
    'ANNUAL',
    'ALL',
    1999,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 year',
    'ACTIVE'
);

-- Example 3: Add a Single Topic Lifetime Subscriber
INSERT INTO user_subscriptions (user_email, full_name, plan_type, topic_id, amount_paid_rupees, start_date, end_date, status)
VALUES (
    'dev@example.com',
    'Anand V',
    'TOPIC_ACCESS',
    'kubernetes',
    49,
    CURRENT_TIMESTAMP,
    NULL, -- Lifetime access
    'ACTIVE'
);

-- Query 1: Get all currently ACTIVE subscribers who have not expired
SELECT 
    id, 
    user_email, 
    full_name, 
    plan_type, 
    topic_id, 
    amount_paid_rupees,
    start_date, 
    end_date, 
    status,
    CASE 
        WHEN end_date IS NULL THEN 'Lifetime Access'
        WHEN end_date < CURRENT_TIMESTAMP THEN 'Expired'
        ELSE CONCAT(ROUND(EXTRACT(EPOCH FROM (end_date - CURRENT_TIMESTAMP)) / 86400), ' days remaining')
    END AS access_duration
FROM user_subscriptions
WHERE status = 'ACTIVE' 
  AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)
ORDER BY start_date DESC;

-- Query 2: Check active subscription for a specific user and topic
SELECT * FROM user_subscriptions
WHERE LOWER(user_email) = LOWER('subscriber1@example.com')
  AND status = 'ACTIVE'
  AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)
  AND (topic_id = 'kubernetes' OR topic_id = 'ALL' OR plan_type = 'COMPLETE_BUNDLE');
