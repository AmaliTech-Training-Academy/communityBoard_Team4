"""
ETL Pipeline for CommunityBoard Analytics
Extracts data from application DB, transforms into analytics-ready format.
"""
import pandas as pd
from sqlalchemy import create_engine, text
from config import DATABASE_URL

engine = create_engine(DATABASE_URL)

def extract_posts():
    """Extract posts data with author and category info."""
    query = text("""
        SELECT p.id, p.title, p.content, p.created_at, p.updated_at,
               u.name AS author_name, u.email AS author_email,
               c.name AS category_name
        FROM posts p
        JOIN users u ON p.author_id = u.id
        LEFT JOIN categories c ON p.category_id = c.id
    """)
    with engine.connect() as conn:
        return pd.read_sql(query, conn)

def extract_comments():
    """Extract comments with post and author info."""
    query = text("""
        SELECT c.id, c.content, c.created_at,
               c.post_id, u.name AS author_name
        FROM comments c
        JOIN users u ON c.author_id = u.id
    """)
    with engine.connect() as conn:
        return pd.read_sql(query, conn)

def transform_daily_activity(posts_df):
    """Aggregate posts by date and category."""
    if posts_df.empty:
        return pd.DataFrame(columns=["date", "category", "post_count"])
    posts_df["date"] = pd.to_datetime(posts_df["created_at"]).dt.date
    daily = posts_df.groupby(["date", "category_name"]).size().reset_index(name="post_count")
    daily.columns = ["date", "category", "post_count"]
    return daily

def transform_user_engagement(posts_df, comments_df):
    """Calculate engagement metrics per user."""
    post_counts = posts_df.groupby("author_email").size().reset_index(name="posts_created")
    comment_counts = comments_df.groupby("author_name").size().reset_index(name="comments_made")
    # TODO: Merge and compute engagement score
    return post_counts

def load_analytics(df, table_name):
    """Load transformed data into analytics tables."""
    df.to_sql(table_name, engine, if_exists="replace", index=False)
    print(f"Loaded {len(df)} rows into {table_name}")

def run_pipeline():
    """Execute the full ETL pipeline."""
    print("Starting CommunityBoard ETL pipeline...")

    # Extract
    posts_df = extract_posts()
    comments_df = extract_comments()
    print(f"Extracted {len(posts_df)} posts, {len(comments_df)} comments")

    # Transform
    daily_activity = transform_daily_activity(posts_df)

    # Load
    load_analytics(daily_activity, "analytics_daily_activity")

    # TODO: Add more transformations and loads
    # - User engagement metrics
    # - Category popularity trends
    # - Content length analysis

    print("ETL pipeline complete!")

if __name__ == "__main__":
    run_pipeline()
