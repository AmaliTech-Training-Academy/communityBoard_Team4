-- V7: category subscriptions for new-post email notifications
CREATE TABLE user_category_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category   VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_category_subscription UNIQUE (user_id, category)
);

CREATE INDEX idx_user_category_subscriptions_category ON user_category_subscriptions (category);
CREATE INDEX idx_user_category_subscriptions_user ON user_category_subscriptions (user_id);
