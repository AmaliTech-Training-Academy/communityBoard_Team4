-- =============================================================
-- CommunityBoard Analytics Views
-- 01_create_analytics_views.sql
-- Creates materialized views for the analytics dashboard
-- These views are refreshed by the ETL service via NOTIFY triggers
--
-- Schema note:
--   posts.category is a STRING ENUM (NEWS, EVENT, DISCUSSION, ALERT)
--   There is NO separate categories table — category lives on posts directly
--   posts.body holds the post content (not posts.content)
--
-- IMPORTANT NOTE ON ORDERING:
--   PostgreSQL materialized views do NOT guarantee row order on SELECT
--   even if ORDER BY is used inside the view definition.
--   Correct ordering is enforced by:
--     1. BTREE indexes on the sort columns (efficient ordered reads)
--     2. The backend API must query with explicit ORDER BY
--   Each view has a comment showing the ORDER BY the API must use.
-- =============================================================


-- -------------------------------------------------------------
-- VIEW 1: SUMMARY
-- Returns total post count and total comment count
-- API endpoint: GET /api/analytics/summary
-- Query:  SELECT * FROM analytics_summary;
-- -------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics_summary AS
SELECT
    (SELECT COUNT(*) FROM posts)    AS total_posts,
    (SELECT COUNT(*) FROM comments) AS total_comments;


-- -------------------------------------------------------------
-- VIEW 2: POSTS BY CATEGORY
-- Returns post count grouped by category enum value
-- All 4 categories always appear even if count is 0
-- API endpoint: GET /api/analytics/posts-by-category
-- Query:  SELECT * FROM analytics_posts_by_category ORDER BY post_count DESC;
-- -------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics_posts_by_category AS
SELECT
    category_name,
    COUNT(p.id) AS post_count
FROM (
    VALUES ('NEWS'), ('EVENT'), ('DISCUSSION'), ('ALERT')
) AS all_categories(category_name)
LEFT JOIN posts p ON p.category::TEXT = all_categories.category_name
GROUP BY category_name;


-- -------------------------------------------------------------
-- VIEW 3: POSTS BY DAY OF WEEK
-- Returns post count grouped by day of week (Sun=0 to Sat=6)
-- All 7 days always appear even if count is 0
-- API endpoint: GET /api/analytics/posts-by-day
-- Query:  SELECT * FROM analytics_posts_by_day ORDER BY day_order ASC;
-- -------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics_posts_by_day AS
SELECT
    day_name,
    day_order,
    COUNT(p.id) AS post_count
FROM (
    VALUES
        ('Sun', 0),
        ('Mon', 1),
        ('Tue', 2),
        ('Wed', 3),
        ('Thu', 4),
        ('Fri', 5),
        ('Sat', 6)
) AS all_days(day_name, day_order)
LEFT JOIN posts p
    ON EXTRACT(DOW FROM p.created_at)::INT = all_days.day_order
GROUP BY day_name, day_order;


-- =============================================================
-- VIEW 4: TOP 10 CONTRIBUTORS
-- Returns top 10 users ranked by number of posts authored
-- etl_rank  → unique sequential number (used for unique index)
-- true_rank → leaderboard rank (tied users get the same rank)
-- API endpoint: GET /api/analytics/top-contributors
-- Query:  SELECT * FROM analytics_top_contributors ORDER BY etl_rank ASC;
-- =============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics_top_contributors AS
WITH ranked_contributors AS (
    SELECT
        ROW_NUMBER() OVER (ORDER BY COUNT(p.id) DESC, u.id ASC) AS etl_rank,
        RANK()       OVER (ORDER BY COUNT(p.id) DESC)           AS true_rank,
        u.id                                                     AS user_id,
        u.name                                                   AS contributor_name,
        u.email                                                  AS contributor_email,
        COUNT(p.id)                                              AS post_count
    FROM users u
    LEFT JOIN posts p ON p.author_id = u.id
    GROUP BY u.id, u.name, u.email
)
SELECT *
FROM ranked_contributors
LIMIT 10;


-- -------------------------------------------------------------
-- UNIQUE INDEXES
-- Required for CONCURRENT refresh (non-blocking view refresh)
-- BTREE type (default) also enables efficient ORDER BY queries
-- -------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_summary_posts
    ON analytics_summary(total_posts);

-- Supports: ORDER BY post_count DESC
CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_category_name
    ON analytics_posts_by_category(category_name);

-- Supports: ORDER BY day_order ASC
CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_day_order
    ON analytics_posts_by_day(day_order);

-- Supports: ORDER BY etl_rank ASC  ← this is what fixes the ordering
CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_contributor_etl_rank
    ON analytics_top_contributors(etl_rank);