-- =============================================================
-- CommunityBoard Analytics Views - Flyway Migration V5
-- Creates materialized views consumed by the analytics API.
--
-- These views are also managed by the data-engineering ETL
-- pipeline (01_create_analytics_views.sql).  Creating them here
-- ensures the application starts cleanly even when the ETL
-- container is not running.  When the ETL runs it will call
-- REFRESH MATERIALIZED VIEW CONCURRENTLY to populate live data.
--
-- IMPORTANT NOTE ON ORDERING:
--   PostgreSQL materialized views do NOT guarantee row order.
--   The unique indexes below (BTREE) enable efficient ORDER BY
--   queries.  The backend API always queries with an explicit
--   ORDER BY clause — do not rely on view-internal ordering.
-- =============================================================


-- -------------------------------------------------------------
-- VIEW 1: SUMMARY
-- API endpoint: GET /api/analytics/summary
-- -------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics_summary AS
SELECT
    (SELECT COUNT(*) FROM posts)    AS total_posts,
    (SELECT COUNT(*) FROM comments) AS total_comments;


-- -------------------------------------------------------------
-- VIEW 2: POSTS BY CATEGORY
-- All 4 categories always appear even when count is 0.
-- API endpoint: GET /api/analytics/posts-by-category
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
-- All 7 days always appear even when count is 0.
-- API endpoint: GET /api/analytics/posts-by-day
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


-- -------------------------------------------------------------
-- VIEW 4: TOP 10 CONTRIBUTORS
-- etl_rank  → unique sequential number (required for CONCURRENT refresh)
-- true_rank → leaderboard rank (tied users share the same rank)
-- API endpoint: GET /api/analytics/contributors/top
-- -------------------------------------------------------------
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
-- Required for CONCURRENT refresh (non-blocking view refresh).
-- BTREE type (default) also enables efficient ORDER BY queries.
-- -------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_summary_posts
    ON analytics_summary(total_posts);

CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_category_name
    ON analytics_posts_by_category(category_name);

CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_day_order
    ON analytics_posts_by_day(day_order);

CREATE UNIQUE INDEX IF NOT EXISTS idx_analytics_contributor_etl_rank
    ON analytics_top_contributors(etl_rank);
