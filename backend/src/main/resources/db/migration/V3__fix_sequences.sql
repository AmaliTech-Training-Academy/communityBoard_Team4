-- =============================================================================
-- CommunityBoard  ·  V3: Fix Sequences
-- =============================================================================
-- V2 inserted seed rows with explicit IDs (users: 1-2, posts: 1-3, comments: none).
-- PostgreSQL BIGSERIAL sequences were NOT advanced by those explicit inserts,
-- so the next auto-generated ID would collide with the seeded rows.
-- This migration advances each sequence to the current max ID in each table.
-- =============================================================================

SELECT setval('users_id_seq',    (SELECT MAX(id) FROM users),    true);
SELECT setval('posts_id_seq',    (SELECT MAX(id) FROM posts),    true);
SELECT setval('comments_id_seq', COALESCE((SELECT MAX(id) FROM comments), 1), true);
