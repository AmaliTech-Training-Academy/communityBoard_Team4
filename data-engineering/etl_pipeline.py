"""
CommunityBoard ETL Pipeline
============================
Entry point for the ETL service. Responsibilities:
  1. Wait for PostgreSQL to be reachable
  2. Wait for the backend to finish creating application tables
  3. Run SQL migration files (views, triggers, seed data)
  4. Start the LISTEN/NOTIFY loop to keep views refreshed in real time

Backoff & Rate Limiting Strategy:
  - Exponential backoff: delay = min(base * 2^attempt, max_delay)
  - Jitter: delay += random(0, 0.5 * delay) to prevent thundering herd
  - Rate limiting: minimum gap between view refreshes
  - Debouncing: burst notifications collapsed into a single refresh
  - Connection recovery: listener reconnects with backoff on drop
"""

import argparse
import json
import logging
import os
import random
import select
import time
from datetime import datetime
from typing import Any

try:
    import psycopg2
    from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
except ModuleNotFoundError:
    psycopg2 = None
    ISOLATION_LEVEL_AUTOCOMMIT = None


ConnectionType = Any

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
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 5432)),
    "dbname": os.getenv("DB_NAME", "communityboard"),
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASSWORD", "postgres"),
}

# SQL files to run in order — located relative to this script
SQL_DIR = os.path.join(os.path.dirname(__file__), "database", "sql")
SQL_FILES = [
    "01_create_analytics_views.sql",
    "02_create_triggers.sql",
    "03_seed_data.sql",
]

# Tables created by Spring Boot — ETL waits for all of these
REQUIRED_TABLES = ["users", "posts", "comments"]

# ------------------------------------------------------------------ #
# Backoff & Rate Limit Configuration
# ------------------------------------------------------------------ #

# Exponential backoff settings
BACKOFF_BASE_SECS = 2  # starting delay in seconds
BACKOFF_MAX_SECS = 60  # cap on delay — never wait longer than this
BACKOFF_JITTER_FACTOR = 0.5  # jitter = random(0, factor * delay)

# Postgres / table wait settings
POSTGRES_MAX_RETRIES = 10  # attempts before giving up on postgres
TABLE_MAX_RETRIES = 15  # attempts before giving up on tables
SQL_MAX_RETRIES = 3  # attempts to re-run a failed SQL file

# View refresh rate limiting — prevents refresh storms
REFRESH_MIN_INTERVAL_SECS = 2  # minimum seconds between view refreshes
REFRESH_MAX_RETRIES = 4  # retry attempts if a view refresh fails
REFRESH_BACKOFF_BASE = 1  # starting retry delay for refresh failures

# Debounce — collect burst notifications then refresh once
DEBOUNCE_WINDOW_SECS = 1.0  # seconds to wait after last notification

# Listener connection recovery
LISTENER_MAX_RECONNECTS = 10  # reconnect attempts after a connection drop


def validate_pipeline_assets() -> None:
    """
    Validate ETL configuration and SQL assets without opening DB connections.

    This provides a CI-safe preflight check for the ETL pipeline.
    """
    missing_files = [
        os.path.join(SQL_DIR, filename)
        for filename in SQL_FILES
        if not os.path.exists(os.path.join(SQL_DIR, filename))
    ]

    if missing_files:
        raise FileNotFoundError("Missing ETL SQL file(s): " + ", ".join(missing_files))

    for key, value in DB_CONFIG.items():
        if value in (None, ""):
            raise ValueError(f"Missing ETL DB config value for: {key}")

    if not REQUIRED_TABLES:
        raise ValueError("REQUIRED_TABLES cannot be empty")

    log.info("ETL preflight passed. SQL files: %s", ", ".join(SQL_FILES))
    log.info("Required tables: %s", ", ".join(REQUIRED_TABLES))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="CommunityBoard ETL pipeline")
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="Validate ETL configuration and SQL assets, then exit.",
    )
    return parser.parse_args()


# ------------------------------------------------------------------ #
# Backoff helper
# ------------------------------------------------------------------ #
def backoff_delay(
    attempt: int,
    base: float = BACKOFF_BASE_SECS,
    max_delay: float = BACKOFF_MAX_SECS,
    jitter_factor: float = BACKOFF_JITTER_FACTOR,
) -> float:
    """
    Calculate exponential backoff delay with jitter, then sleep.

    Formula:
        delay = min(base * 2^attempt, max_delay)
        delay += random(0, jitter_factor * delay)

    Jitter prevents the thundering herd problem when multiple
    ETL instances restart simultaneously after a DB outage.

    Returns the actual total delay applied (in seconds).
    """
    delay = min(base * (2**attempt), max_delay)
    jitter = random.uniform(0, jitter_factor * delay)
    total = delay + jitter
    log.debug("Backoff delay: %.2fs (attempt %d, base=%.1fs)", total, attempt, base)
    time.sleep(total)
    return total


# ------------------------------------------------------------------ #
# Helper: get a database connection (with retry + backoff)
# ------------------------------------------------------------------ #
def get_connection(
    autocommit: bool = False,
    retries: int = 3,
) -> ConnectionType:
    """
    Open a PostgreSQL connection with retry + exponential backoff.

    Retries on transient OperationalError (e.g. brief network blip).
    Raises immediately on non-retryable errors (e.g. auth failure).
    """
    if psycopg2 is None:
        raise RuntimeError(
            "psycopg2 is required to open database connections. "
            "Install data-engineering/requirements.txt first."
        )

    last_exc: Exception = RuntimeError("No connection attempts made.")
    for attempt in range(retries):
        try:
            conn = psycopg2.connect(**DB_CONFIG)
            if autocommit:
                conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
            return conn
        except psycopg2.OperationalError as exc:
            last_exc = exc
            if attempt == retries - 1:
                break
            delay = backoff_delay(attempt, base=1, max_delay=10)
            log.warning(
                "Connection attempt %d/%d failed: %s. Retrying in %.1fs...",
                attempt + 1,
                retries,
                exc,
                delay,
            )
    raise RuntimeError(f"Could not establish database connection: {last_exc}")


# ------------------------------------------------------------------ #
# Step 1: Wait for PostgreSQL to accept connections
# ------------------------------------------------------------------ #
def wait_for_postgres(max_retries: int = POSTGRES_MAX_RETRIES) -> None:
    """
    Poll PostgreSQL until it accepts connections.

    Uses exponential backoff with jitter so early retries are fast
    but later retries do not hammer a struggling server.

    Approximate backoff sequence (with jitter):
        2s, 4s, 8s, 16s, 32s, 60s, 60s, 60s, 60s, 60s
    """
    if psycopg2 is None:
        raise RuntimeError(
            "psycopg2 is required to run the ETL pipeline. "
            "Install data-engineering/requirements.txt first."
        )

    log.info("Waiting for PostgreSQL to be reachable...")
    last_exc: Exception = RuntimeError("No attempts made.")

    for attempt in range(max_retries):
        try:
            conn = psycopg2.connect(**DB_CONFIG)
            conn.close()
            log.info("PostgreSQL is reachable (attempt %d).", attempt + 1)
            return
        except psycopg2.OperationalError as exc:
            last_exc = exc
            delay = backoff_delay(attempt)
            log.info(
                "  Attempt %d/%d — not ready yet. Retrying in %.1fs...",
                attempt + 1,
                max_retries,
                delay,
            )

    raise RuntimeError(
        f"PostgreSQL did not become reachable after {max_retries} "
        f"attempts. Last error: {last_exc}"
    )


# ------------------------------------------------------------------ #
# Step 2: Wait for backend to create application tables
# ------------------------------------------------------------------ #
def wait_for_tables(max_retries: int = TABLE_MAX_RETRIES) -> None:
    """
    Poll until all REQUIRED_TABLES exist in the public schema.

    Spring Boot / Hibernate creates tables on startup. This waits for
    that to complete before the ETL creates views that reference them.
    Uses exponential backoff with jitter.
    """
    log.info("Waiting for backend to create tables: %s", REQUIRED_TABLES)
    last_count = 0

    for attempt in range(max_retries):
        try:
            conn = get_connection()
            cur = conn.cursor()
            cur.execute(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_name = ANY(%s)
                """,
                (REQUIRED_TABLES,),
            )
            last_count = cur.fetchone()[0]
            cur.close()
            conn.close()

            if last_count == len(REQUIRED_TABLES):
                log.info("All required tables exist. Proceeding.")
                return

            delay = backoff_delay(attempt)
            log.info(
                "  Attempt %d/%d — %d/%d tables found. Retrying in %.1fs...",
                attempt + 1,
                max_retries,
                last_count,
                len(REQUIRED_TABLES),
                delay,
            )

        except psycopg2.OperationalError as exc:
            delay = backoff_delay(attempt)
            log.warning(
                "  Attempt %d/%d — DB error: %s. Retrying in %.1fs...",
                attempt + 1,
                max_retries,
                exc,
                delay,
            )

    raise RuntimeError(
        f"Backend tables not created after {max_retries} attempts. "
        f"Found {last_count}/{len(REQUIRED_TABLES)} of {REQUIRED_TABLES}"
    )


# ------------------------------------------------------------------ #
# Step 3: Run SQL migration files
# ------------------------------------------------------------------ #
def run_sql_files(max_retries: int = SQL_MAX_RETRIES) -> None:
    """
    Execute SQL migration files in order.

    Each file is retried independently up to max_retries times with
    exponential backoff on transient errors (deadlock, temp connection
    loss). Non-retryable errors (syntax errors) fail immediately.

    Transient:     psycopg2.OperationalError, DeadlockDetected
    Non-retryable: psycopg2.ProgrammingError
    """
    log.info("Running SQL migration files...")

    TRANSIENT = (psycopg2.OperationalError, psycopg2.errors.DeadlockDetected)

    for filename in SQL_FILES:
        filepath = os.path.join(SQL_DIR, filename)

        if not os.path.exists(filepath):
            log.warning("  SQL file not found, skipping: %s", filepath)
            continue

        log.info("  Running %s...", filename)
        with open(filepath, "r") as fh:
            sql = fh.read()

        for attempt in range(max_retries):
            conn = None
            try:
                conn = get_connection()
                conn.autocommit = True
                cur = conn.cursor()
                cur.execute(sql)
                cur.close()
                conn.close()
                log.info("  %s completed successfully.", filename)
                break  # move to next file

            except psycopg2.ProgrammingError as exc:
                # Syntax / logic error — retrying will not help
                if conn:
                    conn.close()
                log.error("  Non-retryable error in %s: %s", filename, exc)
                raise

            except TRANSIENT as exc:
                if conn:
                    conn.close()
                if attempt == max_retries - 1:
                    log.error(
                        "  %s failed after %d attempts: %s",
                        filename,
                        max_retries,
                        exc,
                    )
                    raise
                delay = backoff_delay(attempt, base=2, max_delay=30)
                log.warning(
                    "  Transient error in %s (attempt %d/%d): %s. "
                    "Retrying in %.1fs...",
                    filename,
                    attempt + 1,
                    max_retries,
                    exc,
                    delay,
                )

            except Exception as exc:
                if conn:
                    conn.close()
                log.error("  Unexpected error in %s: %s", filename, exc)
                raise

    log.info("All SQL migration files completed.")


# ------------------------------------------------------------------ #
# Step 4: Refresh all materialized views
# ------------------------------------------------------------------ #

# Module-level rate-limit tracker
_last_refresh_time: float = 0.0


def refresh_views(
    conn: ConnectionType,
    max_retries: int = REFRESH_MAX_RETRIES,
) -> None:
    """
    Refresh all analytics materialized views.

    Rate limiting: enforces a minimum gap (REFRESH_MIN_INTERVAL_SECS)
    between refreshes to prevent refresh storms on bulk inserts.

    Retry with backoff: each view is retried independently so one
    failing view does not block the others.
    """
    global _last_refresh_time

    # ---- Rate limiting ------------------------------------------ #
    now = time.monotonic()
    elapsed = now - _last_refresh_time
    if elapsed < REFRESH_MIN_INTERVAL_SECS:
        wait = REFRESH_MIN_INTERVAL_SECS - elapsed
        log.info(
            "Rate limit: last refresh %.2fs ago (min %ds). " "Waiting %.2fs...",
            elapsed,
            REFRESH_MIN_INTERVAL_SECS,
            wait,
        )
        time.sleep(wait)

    views = [
        "analytics_summary",
        "analytics_posts_by_category",
        "analytics_posts_by_day",
        "analytics_top_contributors",
    ]

    cur = conn.cursor()
    any_failed = False

    for view in views:
        success = False
        for attempt in range(max_retries):
            try:
                cur.execute(f"REFRESH MATERIALIZED VIEW CONCURRENTLY {view};")
                log.info("  Refreshed: %s", view)
                success = True
                break
            except psycopg2.OperationalError as exc:
                if attempt == max_retries - 1:
                    log.error(
                        "  Failed to refresh %s after %d attempts: %s",
                        view,
                        max_retries,
                        exc,
                    )
                    any_failed = True
                else:
                    delay = backoff_delay(
                        attempt,
                        base=REFRESH_BACKOFF_BASE,
                        max_delay=15,
                    )
                    log.warning(
                        "  Refresh of %s failed (attempt %d/%d): %s. "
                        "Retrying in %.1fs...",
                        view,
                        attempt + 1,
                        max_retries,
                        exc,
                        delay,
                    )
            except Exception as exc:
                log.error("  Unexpected error refreshing %s: %s", view, exc)
                any_failed = True
                break

        if not success and not any_failed:
            any_failed = True

    try:
        conn.commit()
    except Exception as exc:
        log.error("  Failed to commit after view refresh: %s", exc)

    cur.close()
    _last_refresh_time = time.monotonic()

    if any_failed:
        log.warning(
            "One or more views failed to refresh. " "Will retry on next notification."
        )
    else:
        log.info(
            "All views refreshed at %s UTC.",
            datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S"),
        )


# ------------------------------------------------------------------ #
# Step 5: Inner LISTEN/NOTIFY loop
# ------------------------------------------------------------------ #
def _run_listener(conn: ConnectionType) -> None:
    """
    Inner listener loop. Blocks until the connection drops.

    Debouncing: after the first notification arrives, waits
    DEBOUNCE_WINDOW_SECS for the burst to settle before refreshing.
    This collapses 10 rapid inserts into a single refresh.
    """
    cur = conn.cursor()
    cur.execute("LISTEN analytics_refresh;")
    log.info("LISTEN registered on channel 'analytics_refresh'.")

    log.info("Performing initial view refresh on startup...")
    refresh_views(conn)
    log.info("Initial refresh complete. Waiting for changes...")

    pending: list = []

    while True:
        # Block up to 60s waiting for socket activity (keepalive)
        ready = select.select([conn], [], [], 60)
        if ready == ([], [], []):
            continue  # keepalive timeout — loop back

        conn.poll()

        while conn.notifies:
            notify = conn.notifies.pop(0)
            try:
                payload = json.loads(notify.payload)
                log.info(
                    "Notification received — table: %s, operation: %s, at: %s",
                    payload.get("table"),
                    payload.get("operation"),
                    payload.get("timestamp"),
                )
            except json.JSONDecodeError:
                log.info(
                    "Notification on channel '%s' (no structured payload).",
                    notify.channel,
                )
            pending.append(notify)

        if not pending:
            continue

        # ---- Debounce ------------------------------------------- #
        log.info(
            "Debouncing: %d notification(s) received. "
            "Waiting %.1fs for burst to settle...",
            len(pending),
            DEBOUNCE_WINDOW_SECS,
        )
        time.sleep(DEBOUNCE_WINDOW_SECS)

        # Drain any notifications that arrived during the debounce window
        conn.poll()
        while conn.notifies:
            pending.append(conn.notifies.pop(0))
            log.info("  Additional notification collected during debounce.")

        log.info(
            "Debounce complete. Refreshing views for %d batched notification(s)...",
            len(pending),
        )
        pending.clear()

        refresh_views(conn)
        log.info("Views refreshed successfully.")


# ------------------------------------------------------------------ #
# Step 5: Outer listener with connection recovery
# ------------------------------------------------------------------ #
def start_listener(max_reconnects: int = LISTENER_MAX_RECONNECTS) -> None:
    """
    Start the LISTEN/NOTIFY loop with automatic connection recovery.

    If PostgreSQL drops the connection (network issue, server restart),
    this function reconnects with exponential backoff and resumes
    listening. The reconnect counter resets on every successful connect
    so temporary outages do not permanently reduce the retry budget.
    """
    log.info("Starting LISTEN/NOTIFY listener with connection recovery...")
    reconnect_attempt = 0

    while reconnect_attempt <= max_reconnects:
        conn = None
        try:
            log.info(
                "Establishing listener connection (attempt %d/%d)...",
                reconnect_attempt,
                max_reconnects,
            )
            conn = get_connection(autocommit=True)
            reconnect_attempt = 0  # reset on successful connection
            _run_listener(conn)  # blocks until connection drops

        except (psycopg2.OperationalError, psycopg2.InterfaceError) as exc:
            reconnect_attempt += 1
            if conn:
                try:
                    conn.close()
                except Exception:
                    pass

            if reconnect_attempt > max_reconnects:
                raise RuntimeError(
                    f"Listener failed to reconnect after {max_reconnects} "
                    f"attempts. Last error: {exc}"
                )

            delay = backoff_delay(reconnect_attempt, base=2, max_delay=60)
            log.warning(
                "Listener connection lost: %s. "
                "Reconnecting in %.1fs (attempt %d/%d)...",
                exc,
                delay,
                reconnect_attempt,
                max_reconnects,
            )

        except Exception as exc:
            if conn:
                try:
                    conn.close()
                except Exception:
                    pass
            log.error("Unexpected listener error: %s", exc)
            raise


# ------------------------------------------------------------------ #
# Main
# ------------------------------------------------------------------ #
if __name__ == "__main__":
    log.info("=== CommunityBoard ETL Pipeline Starting ===")
    try:
        args = parse_args()
        if args.validate_only:
            validate_pipeline_assets()
        else:
            wait_for_postgres()
            wait_for_tables()
            run_sql_files()
            start_listener()
    except KeyboardInterrupt:
        log.info("ETL pipeline stopped by user.")
    except Exception as exc:
        log.error("Fatal error: %s", exc)
        raise
