# CommunityBoard — Data Engineering

This directory contains the complete data engineering layer for the CommunityBoard
neighborhood bulletin board application. It is responsible for maintaining
real-time analytics views in PostgreSQL and providing the seed data that
populates the platform during development and testing.

---

## Table of Contents

- [Role of the Data Engineer](#role-of-the-data-engineer)
- [Architecture](#architecture)
- [Directory Structure](#directory-structure)
- [Technology Stack](#technology-stack)
- [Database Schema](#database-schema)
- [Analytics Views](#analytics-views)
- [ETL Pipeline](#etl-pipeline)
  - [Startup Sequence](#startup-sequence)
  - [Backoff and Rate Limiting](#backoff-and-rate-limiting)
  - [LISTEN/NOTIFY Loop](#listennotify-loop)
- [SQL Migration Files](#sql-migration-files)
- [Seed Data](#seed-data)
- [Docker Integration](#docker-integration)
- [Running Locally](#running-locally)
- [Running the Tests](#running-the-tests)
- [Configuration Reference](#configuration-reference)
- [Handoff to Backend](#handoff-to-backend)
- [Documentation](#documentation)

---

## Role of the Data Engineer

The data engineering role on this project owns the layer between the raw
application data and the analytics dashboard. This does not include writing
Spring Boot endpoints or React components. The responsibility boundary is:

```
Application writes data (posts, comments, users)
              |
              v
    [DATA ENGINEERING LAYER]
    - Detects changes via PostgreSQL triggers
    - Refreshes analytics materialized views
    - Provides seed data for development
    - Documents the schema and API contract
              |
              v
Spring Boot reads views, serves JSON to React
```

The backend team receives three artefacts and uses them without modification:
the analytics views (SQL), the trigger definitions (SQL), and the API contract
document describing the exact JSON shape each endpoint must return.

---

## Architecture

The pipeline uses PostgreSQL's native `LISTEN/NOTIFY` mechanism to detect data
changes in real time. This approach was chosen over scheduled polling (too slow,
wastes resources) and over Kafka or Airflow (correct tools at scale, wrong tools
for an MVP with tens of thousands of rows).

```
Application layer
  posts / comments / users tables
          |
          | INSERT / UPDATE / DELETE
          v
  PostgreSQL triggers fire
  (trg_posts_analytics, trg_comments_analytics, trg_users_analytics)
          |
          | pg_notify('analytics_refresh', payload)
          v
  Python ETL service (etl_pipeline.py)
  listening on channel 'analytics_refresh'
          |
          | debounce + rate limit
          v
  REFRESH MATERIALIZED VIEW CONCURRENTLY
  (all four analytics views)
          |
          v
  Spring Boot reads views
  React dashboard renders
```

Key design decisions:

- **Materialized views** are used instead of regular views because they
  pre-compute results and can be refreshed non-blocking with `CONCURRENTLY`.
  Regular views would recompute on every API call, which is wasteful.

- **CONCURRENTLY** refresh means the API never blocks during a refresh.
  Read queries against the views continue to return the previous results until
  the refresh completes.

- **Debouncing** collapses a burst of notifications (for example, a batch
  insert of 20 posts) into a single refresh rather than 20 sequential refreshes.

- **Exponential backoff with jitter** on all retry loops prevents thundering
  herd problems when multiple containers restart simultaneously after an outage.

- **Connection recovery** in the listener loop means the ETL service is
  self-healing. If the PostgreSQL connection drops, it reconnects automatically
  without requiring a container restart.

---

## Directory Structure

```
data-engineering/
|
|-- etl_pipeline.py          Entry point for the ETL service. Runs on
|                            container startup and stays alive as a listener.
|
|-- requirements.txt         Runtime Python dependencies.
|-- requirements-test.txt    Test-only Python dependencies.
|-- Dockerfile               Builds the ETL service container image.
|-- pytest.ini               Pytest configuration.
|
|-- database/
|   |-- sql/
|       |-- 01_create_analytics_views.sql   Creates the four materialized views
|       |                                   and their unique indexes.
|       |-- 02_create_triggers.sql          Creates the trigger function and
|       |                                   three table triggers.
|       |-- 03_seed_data.sql                Inserts 20 users, 60 posts, and
|                                           214 comments. Idempotent.
|
|-- tests/
    |-- conftest.py                  Shared fixtures (no_sleep, mock_connect,
    |                                mock_conn, no_jitter, helpers).
    |-- test_backoff.py              Tests for backoff_delay().
    |-- test_get_connection.py       Tests for get_connection().
    |-- test_wait_for_postgres.py    Tests for wait_for_postgres().
    |-- test_wait_for_tables.py      Tests for wait_for_tables().
    |-- test_run_sql_files.py        Tests for run_sql_files().
    |-- test_refresh_views.py        Tests for refresh_views().
    |-- test_start_listener.py       Tests for start_listener() and
                                     _run_listener().
```

---

## Technology Stack

| Component         | Technology           | Version  |
|-------------------|----------------------|----------|
| Runtime language  | Python               | 3.11     |
| Database driver   | psycopg2-binary      | 2.9.9    |
| Database          | PostgreSQL           | 15       |
| Container runtime | Docker               | Compose v3.8 |
| Test framework    | pytest               | 8.1.1    |
| Test mocking      | pytest-mock          | 3.14.0   |

---

## Database Schema

The application tables are created and owned by the Spring Boot backend via
Hibernate JPA. The ETL service must not create or modify these tables.

### users

| Column     | Type          | Notes                               |
|------------|---------------|-------------------------------------|
| id         | BIGSERIAL PK  |                                     |
| email      | VARCHAR(150)  | Unique. Login identifier            |
| name       | VARCHAR(100)  | Display name                        |
| password   | VARCHAR(255)  | BCrypt hash                         |
| role       | VARCHAR(10)   | `USER` or `ADMIN`                   |
| created_at | TIMESTAMP     | Set on insert                       |

### posts

| Column     | Type          | Notes                               |
|------------|---------------|-------------------------------------|
| id         | BIGSERIAL PK  |                                     |
| title      | VARCHAR(255)  |                                     |
| body       | TEXT          |                                     |
| category   | VARCHAR(20)   | `NEWS`, `EVENT`, `DISCUSSION`, `ALERT` |
| author_id  | BIGINT FK     | References users(id)                |
| created_at | TIMESTAMP     |                                     |
| updated_at | TIMESTAMP     |                                     |

### comments

| Column     | Type          | Notes                               |
|------------|---------------|-------------------------------------|
| id         | BIGSERIAL PK  |                                     |
| content    | TEXT          |                                     |
| post_id    | BIGINT FK     | References posts(id) ON DELETE CASCADE |
| author_id  | BIGINT FK     | References users(id)                |
| created_at | TIMESTAMP     |                                     |

Full column-level documentation including constraints, indexes, and enum values
is in `docs/data_dictionary.md`.

---

## Analytics Views

Four materialized views power the analytics dashboard. All are refreshed
atomically by the ETL service whenever data changes.

### analytics_summary

Single-row view. Returns total post count and total comment count.

```sql
SELECT
    (SELECT COUNT(*) FROM posts)    AS total_posts,
    (SELECT COUNT(*) FROM comments) AS total_comments;
```

### analytics_posts_by_category

One row per category. All four categories always appear, even with zero posts,
achieved by joining against a static `VALUES` table.

```sql
-- Simplified representation
SELECT category_name, COUNT(posts) AS post_count
FROM   (VALUES ('NEWS'), ('EVENT'), ('DISCUSSION'), ('ALERT')) AS all_categories
LEFT JOIN posts ON posts.category = category_name
GROUP BY category_name;
```

### analytics_posts_by_day

One row per day of the week. All seven days always appear. Day order follows
PostgreSQL `EXTRACT(DOW ...)` convention: 0 = Sunday.

```sql
-- Simplified representation
SELECT day_name, day_order, COUNT(posts) AS post_count
FROM   (VALUES (0,'Sun'),(1,'Mon'),...,(6,'Sat')) AS all_days
LEFT JOIN posts ON EXTRACT(DOW FROM posts.created_at) = day_order
GROUP BY day_name, day_order;
```

### analytics_top_contributors

Top ten users by post count. Includes both `etl_rank` (unique, for the
required unique index) and `true_rank` (tie-aware, exposed in the API).

```sql
-- Simplified representation
SELECT ROW_NUMBER() OVER (...) AS etl_rank,
       RANK()       OVER (...) AS true_rank,
       u.name, u.email, COUNT(p.id) AS post_count
FROM users u LEFT JOIN posts p ON p.author_id = u.id
GROUP BY u.id
ORDER BY post_count DESC
LIMIT 10;
```

The API must query with `ORDER BY etl_rank ASC` and expose `true_rank` as the
visible rank field. Full view definitions are in
`database/sql/01_create_analytics_views.sql`.

---

## ETL Pipeline

The entire ETL service lives in a single file: `etl_pipeline.py`. It is a
long-running Python process with five sequential steps.

### Startup Sequence

```
1. wait_for_postgres()
   Poll until PostgreSQL accepts connections.

2. wait_for_tables()
   Poll until the Spring Boot backend has created the application tables
   (users, posts, comments). This is necessary because Hibernate creates
   tables on backend startup, which may be after the ETL container starts.

3. run_sql_files()
   Execute the three SQL migration files in order:
     01_create_analytics_views.sql
     02_create_triggers.sql
     03_seed_data.sql

4. start_listener()
   Establish a persistent connection with autocommit, register LISTEN, and
   perform an initial view refresh.

5. _run_listener() [inner loop]
   Block on select() waiting for notifications. On notification, debounce,
   collect burst, then refresh all four views.
```

### Backoff and Rate Limiting

Every retry loop and the connection recovery loop use exponential backoff with
jitter to avoid thundering herd problems.

```
delay = min(base * 2^attempt, max_delay)
delay += random(0, 0.5 * delay)
```

| Setting                   | Default | Description                                    |
|---------------------------|---------|------------------------------------------------|
| `BACKOFF_BASE_SECS`       | 2       | Starting delay in seconds                      |
| `BACKOFF_MAX_SECS`        | 60      | Maximum delay cap                              |
| `BACKOFF_JITTER_FACTOR`   | 0.5     | Fraction of delay added as random jitter       |
| `POSTGRES_MAX_RETRIES`    | 10      | Attempts to reach PostgreSQL before giving up  |
| `TABLE_MAX_RETRIES`       | 15      | Attempts to find backend tables                |
| `SQL_MAX_RETRIES`         | 3       | Retry attempts per SQL migration file          |
| `REFRESH_MIN_INTERVAL_SECS` | 2     | Minimum gap between view refreshes (rate limit)|
| `REFRESH_MAX_RETRIES`     | 4       | Retry attempts per view on refresh failure     |
| `DEBOUNCE_WINDOW_SECS`    | 1.0     | Seconds to wait after first notification for burst to settle |
| `LISTENER_MAX_RECONNECTS` | 10      | Reconnect attempts after a dropped connection  |

`run_sql_files()` distinguishes between transient errors worth retrying
(`psycopg2.OperationalError`, `DeadlockDetected`) and logic errors that should
fail immediately (`psycopg2.ProgrammingError`).

### LISTEN/NOTIFY Loop

The listener uses Python's `select()` to block until the PostgreSQL socket has
data, with a 60-second timeout as a keepalive. This means the process consumes
no CPU while waiting for notifications.

Debounce logic:

```
notification arrives
    wait DEBOUNCE_WINDOW_SECS (default 1.0s)
    drain any additional notifications that arrived during window
    refresh all views once
```

This means 50 rapid inserts produce one refresh, not 50.

Connection recovery:

```
if OperationalError or InterfaceError is raised from _run_listener():
    reconnect_attempt += 1
    sleep(backoff_delay(reconnect_attempt))
    reconnect
    reset reconnect_attempt to 0 on success
```

The reconnect counter resets to zero on every successful connection, so a
temporary outage does not permanently reduce the retry budget.

---

## SQL Migration Files

The three files in `database/sql/` are executed in numeric order by
`run_sql_files()`. They are idempotent — `IF NOT EXISTS` and `CREATE OR REPLACE`
guards ensure they can be re-run safely without duplicating objects.

### 01_create_analytics_views.sql

Creates the four materialized views and their unique indexes. Views use `VALUES`
tables to guarantee all categories and all days of the week appear even with no
matching posts.

### 02_create_triggers.sql

Creates the `notify_analytics_refresh()` trigger function and attaches it to
the `posts`, `comments`, and `users` tables with `DROP TRIGGER IF EXISTS` guards
to prevent duplicate trigger errors on re-runs.

### 03_seed_data.sql

Inserts 20 users, 60 posts, and 214 comments. The entire block is wrapped in:

```sql
DO $$ BEGIN
IF (SELECT COUNT(*) FROM users) = 0 THEN
    -- all inserts here
END IF;
END $$;
```

This guard makes the seed script safe to re-run. It only inserts if the `users`
table is empty, preventing duplicate data on container restarts.

---

## Seed Data

The seed data is designed to produce meaningful charts on the analytics dashboard
rather than uniform distributions that would make the dashboard look artificial.

| Category   | Posts | Share |
|------------|-------|-------|
| NEWS       | 18    | 30%   |
| EVENT      | 18    | 30%   |
| DISCUSSION | 15    | 25%   |
| ALERT      | 9     | 15%   |

Posts are distributed across 90 days with varied timestamps to produce a
realistic day-of-week distribution. Comments are spread across 30 of the 60
posts, with between 2 and 6 comments per post.

**Test credentials (all users share the same password):**

| Email                      | Password      | Role  |
|----------------------------|---------------|-------|
| john.smith@email.com       | Password123!  | USER  |
| admin@communityboard.com   | Password123!  | ADMIN |

---

## Docker Integration

The ETL service is defined as the `etl` service in the root `docker-compose.yml`.

```yaml
etl:
  build: ./data-engineering
  environment:
    DB_HOST:     postgres
    DB_PORT:     5432
    DB_NAME:     communityboard
    DB_USER:     postgres
    DB_PASSWORD: postgres
  depends_on:
    postgres:
      condition: service_healthy
  restart: always
```

The `restart: always` policy ensures the ETL service automatically recovers
from crashes without manual intervention.

The `postgres` service is configured with a health check:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d communityboard"]
  interval: 5s
  timeout: 5s
  retries: 10
```

This ensures Docker does not start the backend or ETL containers until
PostgreSQL is genuinely ready to accept connections — not just started.

**Important:** The ETL service still implements its own `wait_for_tables()`
retry loop even though Docker health checks are used. The health check confirms
PostgreSQL is running, but the Spring Boot backend may take additional seconds
to complete Hibernate table creation after its container starts.

---

## Running Locally

**Start the full stack:**

```bash
# From the project root
docker compose up --build
```

**First-time setup (wipe existing data):**

```bash
docker compose down -v    # destroys the pgdata volume
docker compose up --build
```

The `-v` flag is required on first setup if the volume already exists from a
previous run. Without it, the SQL migration scripts and seed data will not
re-execute because PostgreSQL only runs `/docker-entrypoint-initdb.d/` scripts
on a fresh volume.

**Verify the ETL service is running:**

```bash
docker compose logs etl --follow
```

Expected output on a successful startup:

```
2026-03-12 10:00:00 [INFO] === CommunityBoard ETL Pipeline Starting ===
2026-03-12 10:00:02 [INFO] PostgreSQL is reachable (attempt 1).
2026-03-12 10:00:04 [INFO] All required tables exist. Proceeding.
2026-03-12 10:00:04 [INFO] Running SQL migration files...
2026-03-12 10:00:05 [INFO] 01_create_analytics_views.sql completed successfully.
2026-03-12 10:00:05 [INFO] 02_create_triggers.sql completed successfully.
2026-03-12 10:00:05 [INFO] 03_seed_data.sql completed successfully.
2026-03-12 10:00:05 [INFO] LISTEN registered on channel 'analytics_refresh'.
2026-03-12 10:00:05 [INFO] Performing initial view refresh on startup...
2026-03-12 10:00:05 [INFO] All views refreshed at 2026-03-12 10:00:05 UTC.
2026-03-12 10:00:05 [INFO] Initial refresh complete. Waiting for changes...
```

**Verify the views are populated:**

Connect to the database on port 5432 (internal) or 5433 (host-mapped) and run:

```sql
SELECT * FROM analytics_summary;
SELECT * FROM analytics_posts_by_category ORDER BY post_count DESC;
SELECT * FROM analytics_posts_by_day      ORDER BY day_order ASC;
SELECT * FROM analytics_top_contributors  ORDER BY etl_rank ASC;
```

**Verify live refresh:**

Insert a post via the API or directly in the database, then re-query the views.
The counts should update within 2–3 seconds.

**pgAdmin connection settings:**

| Field    | Value          |
|----------|----------------|
| Host     | localhost      |
| Port     | 5433           |
| Database | communityboard |
| Username | postgres       |
| Password | postgres       |

---

## Running the Tests

All tests are unit tests. They use mocks exclusively and do not require a
running database or Docker.

**Install test dependencies:**

```bash
cd data-engineering
pip install -r requirements-test.txt
```

**Run all tests:**

```bash
pytest
```

**Run a single test file:**

```bash
pytest tests/test_refresh_views.py
```

**Run with verbose output:**

```bash
pytest -v
```

**Run with coverage:**

```bash
pip install pytest-cov
pytest --cov=etl_pipeline --cov-report=term-missing
```

### Test structure

| File                       | Function under test    | Number of tests |
|----------------------------|------------------------|-----------------|
| `test_backoff.py`          | `backoff_delay()`      | 9               |
| `test_get_connection.py`   | `get_connection()`     | 7               |
| `test_wait_for_postgres.py`| `wait_for_postgres()`  | 8               |
| `test_wait_for_tables.py`  | `wait_for_tables()`    | 8               |
| `test_run_sql_files.py`    | `run_sql_files()`      | 8               |
| `test_refresh_views.py`    | `refresh_views()`      | 11              |
| `test_start_listener.py`   | `start_listener()` and `_run_listener()` | 10 |

All tests use the `no_sleep` fixture (defined in `conftest.py`) which replaces
`time.sleep` with a no-op, making the suite run in seconds rather than minutes.

---

## Configuration Reference

All configuration is provided via environment variables. Defaults are set for
local development.

| Variable      | Default         | Description                          |
|---------------|-----------------|--------------------------------------|
| `DB_HOST`     | `localhost`     | PostgreSQL hostname                  |
| `DB_PORT`     | `5432`          | PostgreSQL port                      |
| `DB_NAME`     | `communityboard`| Target database name                 |
| `DB_USER`     | `postgres`      | Database user                        |
| `DB_PASSWORD` | `postgres`      | Database password                    |

In Docker Compose, these are supplied by the `etl` service's `environment`
block and read from the `.env` file in the project root.

---

## Handoff to Backend

The backend team needs the following from the data engineering layer. All items
are implemented and committed.

| Deliverable                     | File / Location                                      |
|---------------------------------|------------------------------------------------------|
| Analytics views SQL             | `database/sql/01_create_analytics_views.sql`         |
| Trigger definitions SQL         | `database/sql/02_create_triggers.sql`                |
| Seed data SQL                   | `database/sql/03_seed_data.sql`                      |
| API contract (JSON shapes)      | `docs/api_contract.md`                               |
| Data dictionary (schema)        | `docs/data_dictionary.md`                            |

The backend team is responsible for:
- Creating four `@GetMapping` endpoints that query the analytics views
- Applying the `ORDER BY` clauses specified in `docs/api_contract.md`
- Exposing `true_rank` (not `etl_rank`) as the rank field for contributors

The frontend team is responsible for:
- Consuming the four endpoints defined in `docs/api_contract.md`
- Using `dayOrder` for chart axis ordering on the day-of-week chart
- Handling tie ranks correctly in the contributors table

---

## Documentation

| Document              | Location                  | Contents                                      |
|-----------------------|---------------------------|-----------------------------------------------|
| API Contract          | `docs/api_contract.md`    | Endpoint definitions, JSON shapes, SQL queries, error codes |
| Data Dictionary       | `docs/data_dictionary.md` | All tables, columns, views, triggers, indexes, enums |
| This README           | `data-engineering/README.md` | Architecture, setup, testing, configuration |