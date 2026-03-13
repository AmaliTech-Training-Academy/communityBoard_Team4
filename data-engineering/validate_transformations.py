import re
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
SQL_DIR = BASE_DIR / "database" / "sql"

VIEWS_SQL = SQL_DIR / "01_create_analytics_views.sql"
TRIGGERS_SQL = SQL_DIR / "02_create_triggers.sql"
SEED_SQL = SQL_DIR / "03_seed_data.sql"


def read_text(path: Path) -> str:
    if not path.exists():
        raise FileNotFoundError(f"Missing SQL file: {path}")
    return path.read_text(encoding="utf-8")


def assert_contains(text: str, pattern: str, message: str) -> None:
    if not re.search(pattern, text, re.MULTILINE | re.DOTALL):
        raise AssertionError(message)


def validate_views_sql() -> None:
    sql = read_text(VIEWS_SQL)

    expected_views = [
        "analytics_summary",
        "analytics_posts_by_category",
        "analytics_posts_by_day",
        "analytics_top_contributors",
    ]
    for view_name in expected_views:
        assert_contains(
            sql,
            rf"CREATE MATERIALIZED VIEW IF NOT EXISTS {view_name} AS",
            f"Missing materialized view definition: {view_name}",
        )

    expected_indexes = [
        "idx_analytics_summary_posts",
        "idx_analytics_category_name",
        "idx_analytics_day_order",
        "idx_analytics_contributor_etl_rank",
    ]
    for index_name in expected_indexes:
        assert_contains(
            sql,
            rf"CREATE UNIQUE INDEX IF NOT EXISTS {index_name}",
            f"Missing unique index for concurrent refresh support: {index_name}",
        )

    for category in ["NEWS", "EVENT", "DISCUSSION", "ALERT"]:
        assert_contains(
            sql,
            rf"'{category}'",
            f"Category coverage missing from analytics_posts_by_category: {category}",
        )

    for day_name, day_order in [
        ("Sun", 0),
        ("Mon", 1),
        ("Tue", 2),
        ("Wed", 3),
        ("Thu", 4),
        ("Fri", 5),
        ("Sat", 6),
    ]:
        assert_contains(
            sql,
            rf"\('{day_name}',\s*{day_order}\)",
            f"Day-of-week coverage missing from analytics_posts_by_day: {day_name}",
        )


def validate_triggers_sql() -> None:
    sql = read_text(TRIGGERS_SQL)

    assert_contains(
        sql,
        r"CREATE OR REPLACE FUNCTION notify_analytics_refresh\(\)",
        "Missing notify_analytics_refresh trigger function",
    )
    assert_contains(
        sql,
        r"pg_notify\(\s*'analytics_refresh'",
        "Triggers do not notify the analytics_refresh channel",
    )

    expected_triggers = {
        "posts": "trg_posts_analytics",
        "comments": "trg_comments_analytics",
        "users": "trg_users_analytics",
    }
    for table_name, trigger_name in expected_triggers.items():
        assert_contains(
            sql,
            (
                rf"CREATE TRIGGER {trigger_name}\s+AFTER INSERT OR UPDATE OR DELETE "
                rf"ON {table_name}"
            ),
            f"Missing analytics trigger on {table_name}",
        )


def validate_seed_sql() -> None:
    sql = read_text(SEED_SQL)

    for table_name in ["users", "posts", "comments"]:
        assert_contains(
            sql,
            rf"INSERT INTO {table_name}\s*\(",
            f"Seed data missing INSERT statements for {table_name}",
        )


def main() -> None:
    validate_views_sql()
    validate_triggers_sql()
    validate_seed_sql()
    print("Data quality checks passed.")


if __name__ == "__main__":
    main()
