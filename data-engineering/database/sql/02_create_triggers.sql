-- =============================================================
-- CommunityBoard LISTEN/NOTIFY Triggers
-- 02_create_triggers.sql
-- Fires a NOTIFY signal whenever ANY application table changes
-- The Python ETL listener receives this and refreshes all views
--
-- Tables covered: posts, comments, users
-- Note: categories table no longer exists — category is an ENUM
--       column directly on the posts table
-- =============================================================


-- -------------------------------------------------------------
-- TRIGGER FUNCTION
-- Single reusable function attached to all tables
-- Sends NOTIFY on channel 'analytics_refresh'
-- Payload tells the ETL which table changed and what operation
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION notify_analytics_refresh()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify(
        'analytics_refresh',
        json_build_object(
            'table',     TG_TABLE_NAME,
            'operation', TG_OP,
            'timestamp', NOW()
        )::TEXT
    );
    -- Correctly return OLD on DELETE, NEW on INSERT/UPDATE
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- -------------------------------------------------------------
-- TRIGGER ON POSTS TABLE
-- Fires on INSERT, UPDATE, DELETE
-- Affects: analytics_summary       (total_posts count)
--          analytics_posts_by_category (category breakdown)
--          analytics_posts_by_day      (day of week breakdown)
--          analytics_top_contributors  (post counts per user)
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_posts_analytics ON posts;

CREATE TRIGGER trg_posts_analytics
AFTER INSERT OR UPDATE OR DELETE ON posts
FOR EACH ROW
EXECUTE FUNCTION notify_analytics_refresh();


-- -------------------------------------------------------------
-- TRIGGER ON COMMENTS TABLE
-- Fires on INSERT, UPDATE, DELETE
-- Affects: analytics_summary (total_comments count)
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_comments_analytics ON comments;

CREATE TRIGGER trg_comments_analytics
AFTER INSERT OR UPDATE OR DELETE ON comments
FOR EACH ROW
EXECUTE FUNCTION notify_analytics_refresh();


-- -------------------------------------------------------------
-- TRIGGER ON USERS TABLE
-- Fires on INSERT, UPDATE, DELETE
-- Affects: analytics_top_contributors (contributor name/email)
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_users_analytics ON users;

CREATE TRIGGER trg_users_analytics
AFTER INSERT OR UPDATE OR DELETE ON users
FOR EACH ROW
EXECUTE FUNCTION notify_analytics_refresh();