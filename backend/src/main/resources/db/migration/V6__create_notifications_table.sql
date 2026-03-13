-- V6: notifications table for user inbox
CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message      VARCHAR(500) NOT NULL,
    read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, read);
