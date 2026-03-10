-- =============================================================================
-- CommunityBoard  ·  V2: Seed Data
-- =============================================================================
-- Inserts development / demo accounts and sample posts.
-- Uses ON CONFLICT DO NOTHING so re-running is safe (idempotent via Flyway checksums).
-- Passwords are BCrypt-hashed; plain-text equivalent: password123
-- =============================================================================

-- ---------------------------------------------------------------------------
-- USERS  (1 admin, 1 standard user)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, name, email, password, role, created_at)
VALUES
    (1, 'Admin User', 'admin@amalitech.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'ADMIN', NOW()),
    (2, 'Test User', 'user@amalitech.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'USER', NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- POSTS  (uses `body` column — aligned with V1 schema and CommunityBoard spec)
-- ---------------------------------------------------------------------------
INSERT INTO posts (id, title, body, category, author_id, created_at, updated_at)
VALUES
    (1,
     'Welcome to CommunityBoard!',
     'This is our official community platform. Post news, share events, and start discussions.',
     'NEWS', 1, NOW(), NOW()),
    (2,
     'Upcoming Team Building Event',
     'We are organising a cross-location team building event next Friday. Details to follow.',
     'EVENT', 1, NOW(), NOW()),
    (3,
     'Tech Talk: Spring Boot Best Practices',
     'Sharing key lessons from our backend architecture review this quarter.',
     'DISCUSSION', 2, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
