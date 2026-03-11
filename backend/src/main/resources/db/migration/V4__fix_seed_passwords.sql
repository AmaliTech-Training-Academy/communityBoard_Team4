-- =============================================================================
-- CommunityBoard  ·  V4: Fix Admin & Seed User Passwords
-- =============================================================================
-- The BCrypt hash stored in V2 did not match the documented plain-text password.
-- This migration replaces both seed account passwords with freshly generated hashes.
--
--  admin@amalitech.com  →  Admin@123
--  user@amalitech.com   →  password123
-- =============================================================================

UPDATE users
SET password = '$2a$10$EEnBdD8kYUiE56ngQfRT..8qeYfnyRHGzNjYl7yvY1.NysSeGrSxW'
WHERE email = 'admin@amalitech.com';

UPDATE users
SET password = '$2a$10$5kTCM6RMC8RbfQkB9IhpmOBbVx8HOzF0bMnExgO5qvWOGeq9c.Hha'
WHERE email = 'user@amalitech.com';
