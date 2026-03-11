-- =============================================================================
-- CommunityBoard  ·  V1: Core Schema
-- =============================================================================
-- Creates: users, posts, comments
-- Enforces: PK, unique constraints, FK constraints (with cascade), CHECK constraints,
--           and performance indexes aligned with expected access patterns.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(10)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE      (email),
    CONSTRAINT chk_user_role  CHECK       (role IN ('USER', 'ADMIN'))
);

-- ---------------------------------------------------------------------------
-- POSTS
-- ---------------------------------------------------------------------------
-- Notes:
--   • body  — TEXT, no hard DB size limit; enforced at application layer (max 10 000 chars).
--   • category — restricted to the four CommunityBoard enum values via CHECK constraint.
--   • author_id — FK to users; ON DELETE CASCADE removes posts when the author is deleted.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts
(
    id         BIGSERIAL    NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    category   VARCHAR(20)  NOT NULL,
    author_id  BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_posts          PRIMARY KEY (id),
    CONSTRAINT fk_posts_author   FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_post_category CHECK (category IN ('NEWS', 'EVENT', 'DISCUSSION', 'ALERT'))
);

-- ---------------------------------------------------------------------------
-- COMMENTS
-- ---------------------------------------------------------------------------
-- Notes:
--   • post_id  — FK to posts; ON DELETE CASCADE removes comments when the post is deleted.
--   • author_id — FK to users; ON DELETE CASCADE removes comments when the author is deleted.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comments
(
    id         BIGSERIAL NOT NULL,
    content    TEXT      NOT NULL,
    post_id    BIGINT    NOT NULL,
    author_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_comments          PRIMARY KEY (id),
    CONSTRAINT fk_comments_post     FOREIGN KEY (post_id)   REFERENCES posts (id)  ON DELETE CASCADE,
    CONSTRAINT fk_comments_author   FOREIGN KEY (author_id) REFERENCES users (id)  ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- INDEXES  (high-selectivity columns used in search/filter/order-by queries)
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_posts_author_id    ON posts    (author_id);
CREATE INDEX IF NOT EXISTS idx_posts_category     ON posts    (category);
CREATE INDEX IF NOT EXISTS idx_posts_created_at   ON posts    (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_post_id   ON comments (post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments (author_id);
