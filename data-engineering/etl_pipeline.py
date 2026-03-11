"""
ETL Pipeline for CommunityBoard Analytics
Extracts data from application DB, transforms into analytics-ready format.
"""


# import pandas as pd
# from sqlalchemy import create_engine, text
# from config import DATABASE_URL

# engine = create_engine(DATABASE_URL)


# def extract_posts():
#     """Extract posts data with author and category info."""
#     query = text(
#         """
#         SELECT p.id, p.title, p.content, p.created_at, p.updated_at,
#                u.name AS author_name, u.email AS author_email,
#                c.name AS category_name
#         FROM posts p
#         JOIN users u ON p.author_id = u.id
#         LEFT JOIN categories c ON p.category_id = c.id
#     """
#     )
#     with engine.connect() as conn:
#         return pd.read_sql(query, conn)


# def extract_comments():
#     """Extract comments with post and author info."""
#     query = text(
#         """
#         SELECT c.id, c.content, c.created_at,
#                c.post_id, u.name AS author_name
#         FROM comments c
#         JOIN users u ON c.author_id = u.id
#     """
#     )
#     with engine.connect() as conn:
#         return pd.read_sql(query, conn)


# def transform_daily_activity(posts_df):
#     """Aggregate posts by date and category."""
#     if posts_df.empty:
#         return pd.DataFrame(columns=["date", "category", "post_count"])
#     posts_df["date"] = pd.to_datetime(posts_df["created_at"]).dt.date
#     daily = (
#         posts_df.groupby(["date", "category_name"])
#         .size()
#         .reset_index(name="post_count")
#     )
#     daily.columns = ["date", "category", "post_count"]
#     return daily


# def transform_user_engagement(posts_df, comments_df):
#     """Calculate engagement metrics per user."""
#     post_counts = (
#         posts_df.groupby("author_email").size().reset_index(name="posts_created")
#     )
#     # TODO: Merge comments and compute engagement score
#     return post_counts


# def load_analytics(df, table_name):
#     """Load transformed data into analytics tables."""
#     df.to_sql(table_name, engine, if_exists="replace", index=False)
#     print(f"Loaded {len(df)} rows into {table_name}")


# def run_pipeline():
#     """Execute the full ETL pipeline."""
#     print("Starting CommunityBoard ETL pipeline...")

#     # Extract
#     posts_df = extract_posts()
#     comments_df = extract_comments()
#     print(f"Extracted {len(posts_df)} posts, {len(comments_df)} comments")

#     # Transform
#     daily_activity = transform_daily_activity(posts_df)

#     # Load
#     load_analytics(daily_activity, "analytics_daily_activity")

#     # TODO: Add more transformations and loads
#     # - User engagement metrics
#     # - Category popularity trends
#     # - Content length analysis

#     print("ETL pipeline complete!")


# if __name__ == "__main__":
#     run_pipeline()



"""
CommunityBoard ETL Pipeline
============================
Entry point for the ETL service. Responsibilities:
  1. Wait for PostgreSQL to be reachable
  2. Wait for the backend to finish creating application tables
  3. Run SQL migration files (views, triggers, seed data)
  4. Start the LISTEN/NOTIFY loop to keep views refreshed in real time
"""

import os
import time
import logging
import psycopg2
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
import select
import json

# ------------------------------------------------------------------ #
# Logging
# ------------------------------------------------------------------ #
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)

# ------------------------------------------------------------------ #
# DB config from environment variables
# ------------------------------------------------------------------ #
DB_CONFIG = {
    "host":     os.getenv("DB_HOST", "localhost"),
    "port":     int(os.getenv("DB_PORT", 5432)),
    "dbname":   os.getenv("DB_NAME", "communityboard"),
    "user":     os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASSWORD", "postgres"),
}

# SQL files to run in order — located next to this script
SQL_DIR = os.path.join(os.path.dirname(__file__), "database", "sql")
SQL_FILES = [
    "01_create_analytics_views.sql",
    "02_create_triggers.sql",
    "03_seed_data.sql",
]

# Tables created by the Spring Boot backend — we wait for these
REQUIRED_TABLES = ["users", "posts", "comments"]


# ------------------------------------------------------------------ #
# Helper: get a database connection
# ------------------------------------------------------------------ #
def get_connection(autocommit: bool = False):
    conn = psycopg2.connect(**DB_CONFIG)
    if autocommit:
        conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
    return conn


# ------------------------------------------------------------------ #
# Step 1: Wait for PostgreSQL to accept connections
# ------------------------------------------------------------------ #
def wait_for_postgres(retries: int = 30, delay: int = 5):
    log.info("Waiting for PostgreSQL to be reachable...")
    for attempt in range(1, retries + 1):
        try:
            conn = get_connection()
            conn.close()
            log.info("PostgreSQL is reachable.")
            return
        except psycopg2.OperationalError:
            log.info(f"  Attempt {attempt}/{retries} — not ready yet, retrying in {delay}s...")
            time.sleep(delay)
    raise RuntimeError("PostgreSQL did not become reachable in time.")


# ------------------------------------------------------------------ #
# Step 2: Wait for backend to create application tables
# ------------------------------------------------------------------ #
def wait_for_tables(retries: int = 30, delay: int = 5):
    log.info(f"Waiting for backend to create tables: {REQUIRED_TABLES}")
    for attempt in range(1, retries + 1):
        try:
            conn = get_connection()
            cur = conn.cursor()
            cur.execute("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_name = ANY(%s)
            """, (REQUIRED_TABLES,))
            count = cur.fetchone()[0]
            cur.close()
            conn.close()
            if count == len(REQUIRED_TABLES):
                log.info("All required tables exist. Proceeding.")
                return
            else:
                log.info(f"  Attempt {attempt}/{retries} — only {count}/{len(REQUIRED_TABLES)} tables found, retrying in {delay}s...")
        except psycopg2.OperationalError as e:
            log.info(f"  Attempt {attempt}/{retries} — DB error: {e}, retrying in {delay}s...")
        time.sleep(delay)
    raise RuntimeError("Backend tables were not created in time.")


# ------------------------------------------------------------------ #
# Step 3: Run SQL migration files
# ------------------------------------------------------------------ #
def run_sql_files():
    log.info("Running SQL migration files...")
    conn = get_connection()
    conn.autocommit = True
    cur = conn.cursor()

    for filename in SQL_FILES:
        filepath = os.path.join(SQL_DIR, filename)
        if not os.path.exists(filepath):
            log.warning(f"  SQL file not found, skipping: {filepath}")
            continue
        log.info(f"  Running {filename}...")
        with open(filepath, "r") as f:
            sql = f.read()
        try:
            cur.execute(sql)
            log.info(f"  {filename} completed successfully.")
        except Exception as e:
            log.error(f"  Error running {filename}: {e}")
            raise

    cur.close()
    conn.close()
    log.info("All SQL migration files completed.")


# ------------------------------------------------------------------ #
# Step 4: Refresh all materialized views
# ------------------------------------------------------------------ #
def refresh_views(conn):
    views = [
        "analytics_summary",
        "analytics_posts_by_category",
        "analytics_posts_by_day",
        "analytics_top_contributors",
    ]
    cur = conn.cursor()
    for view in views:
        try:
            cur.execute(f"REFRESH MATERIALIZED VIEW CONCURRENTLY {view};")
            log.info(f"  Refreshed: {view}")
        except Exception as e:
            log.error(f"  Failed to refresh {view}: {e}")
    conn.commit()
    cur.close()


# ------------------------------------------------------------------ #
# Step 5: LISTEN/NOTIFY loop — stays running forever
# ------------------------------------------------------------------ #
def start_listener():
    log.info("Starting LISTEN/NOTIFY loop on channel 'analytics_refresh'...")
    conn = get_connection(autocommit=True)
    cur = conn.cursor()
    cur.execute("LISTEN analytics_refresh;")
    log.info("Listening for database changes. Views will refresh on every notification.")

    # Do an initial refresh so views are current on startup
    log.info("Performing initial view refresh...")
    refresh_views(conn)
    log.info("Initial refresh complete. Waiting for changes...")

    while True:
        # Wait up to 60 seconds for a notification, then loop back
        if select.select([conn], [], [], 60) == ([], [], []):
            # Timeout — no notification, just keep waiting
            continue

        conn.poll()
        while conn.notifies:
            notify = conn.notifies.pop(0)
            try:
                payload = json.loads(notify.payload)
                log.info(
                    f"Change detected — table: {payload.get('table')}, "
                    f"operation: {payload.get('operation')}, "
                    f"timestamp: {payload.get('timestamp')}"
                )
            except json.JSONDecodeError:
                log.info(f"Notification received on channel: {notify.channel}")

            log.info("Refreshing analytics views...")
            refresh_views(conn)
            log.info("Views refreshed successfully.")


# ------------------------------------------------------------------ #
# Main
# ------------------------------------------------------------------ #
if __name__ == "__main__":
    log.info("=== CommunityBoard ETL Pipeline Starting ===")
    try:
        wait_for_postgres()
        wait_for_tables()
        run_sql_files()
        start_listener()
    except KeyboardInterrupt:
        log.info("ETL pipeline stopped.")
    except Exception as e:
        log.error(f"Fatal error: {e}")
        raise