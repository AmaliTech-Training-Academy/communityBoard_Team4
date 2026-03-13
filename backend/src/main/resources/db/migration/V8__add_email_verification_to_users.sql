-- V8: email verification fields on users
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN,
    ADD COLUMN email_verification_token VARCHAR(255),
    ADD COLUMN email_verification_expires_at TIMESTAMP;

UPDATE users
SET email_verified = TRUE
WHERE email_verified IS NULL;

ALTER TABLE users
    ALTER COLUMN email_verified SET NOT NULL,
    ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE INDEX idx_users_email_verification_token ON users (email_verification_token);
