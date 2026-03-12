# Data Dictionary — CommunityBoard

**Version:** 1.0  
**Prepared by:** Ayimbila Nsolemna Percy  
**Database:** PostgreSQL 15  
**Database name:** `communityboard`

---

## Overview

This document describes every table, column, index, constraint, trigger, and
materialized view in the CommunityBoard database. It covers both the application
schema (owned by the Spring Boot backend) and the analytics schema (owned and
maintained by the ETL service).

---

## Table of Contents

1. [Application Tables](#1-application-tables)
   - [users](#11-users)
   - [posts](#12-posts)
   - [comments](#13-comments)
2. [Analytics Materialized Views](#2-analytics-materialized-views)
   - [analytics_summary](#21-analytics_summary)
   - [analytics_posts_by_category](#22-analytics_posts_by_category)
   - [analytics_posts_by_day](#23-analytics_posts_by_day)
   - [analytics_top_contributors](#24-analytics_top_contributors)
3. [Triggers](#3-triggers)
4. [Indexes](#4-indexes)
5. [Enumerations](#5-enumerations)
6. [Seed Data Summary](#6-seed-data-summary)

---

## 1. Application Tables

These tables are created and owned by the Spring Boot backend via Hibernate
JPA. The ETL service must never write to these tables — it reads from them
to populate the analytics views.

---

### 1.1 users

Stores all registered users of the CommunityBoard application.

| Column       | Data Type      | Nullable | Default    | Constraints              | Description                                      |
|--------------|----------------|----------|------------|--------------------------|--------------------------------------------------|
| `id`         | `BIGSERIAL`    | NOT NULL | auto       | PRIMARY KEY              | Auto-incrementing unique identifier              |
| `email`      | `VARCHAR(150)` | NOT NULL |            | UNIQUE                   | User's email address. Used for login             |
| `name`       | `VARCHAR(100)` | NOT NULL |            |                          | User's display name shown in posts and comments  |
| `password`   | `VARCHAR(255)` | NOT NULL |            |                          | BCrypt-hashed password. Never stored in plaintext|
| `role`       | `VARCHAR(10)`  | NOT NULL | `'USER'`   | CHECK IN ('USER','ADMIN')| Access control role. Hibernate `@Enumerated(STRING)` |
| `created_at` | `TIMESTAMP`    | NOT NULL | `NOW()`    |                          | Account creation timestamp (application server time) |

**Notes:**
- The `email` column is the login identifier. It is unique across all users.
- Passwords are hashed with BCrypt (cost factor 10) before storage. The ETL
  seed data uses the hash `$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.`
  which corresponds to the plaintext `Password123!`.
- The `role` column is stored as a VARCHAR by Hibernate `EnumType.STRING`. Valid
  values are `USER` (default) and `ADMIN`.
- `created_at` defaults to the current timestamp and is set on insert only.

---

### 1.2 posts

Stores all bulletin board posts created by users. Each post belongs to exactly
one category and one author.

| Column       | Data Type      | Nullable | Default | Constraints                    | Description                                     |
|--------------|----------------|----------|---------|--------------------------------|-------------------------------------------------|
| `id`         | `BIGSERIAL`    | NOT NULL | auto    | PRIMARY KEY                    | Auto-incrementing unique identifier             |
| `title`      | `VARCHAR(255)` | NOT NULL |         |                                | Post headline displayed in the post list        |
| `body`       | `TEXT`         | NOT NULL |         |                                | Full post content. No length cap enforced in DB |
| `category`   | `VARCHAR(20)`  | NOT NULL |         | CHECK IN ('NEWS','EVENT','DISCUSSION','ALERT') | Post category stored as a string enum    |
| `author_id`  | `BIGINT`       | NOT NULL |         | FOREIGN KEY → `users(id)`     | References the user who created the post        |
| `created_at` | `TIMESTAMP`    | NOT NULL | `NOW()` |                                | Timestamp of post creation                      |
| `updated_at` | `TIMESTAMP`    | NOT NULL | `NOW()` |                                | Timestamp of last edit. Updated on every save   |

**Notes:**
- The `category` column stores values as plain strings rather than a PostgreSQL
  `ENUM` type. This matches the Hibernate `@Enumerated(EnumType.STRING)` mapping
  on the backend. Valid values are `NEWS`, `EVENT`, `DISCUSSION`, and `ALERT`.
- The analytics views group by `category` directly — no join to a categories
  table is required.
- Cascade delete is not configured on `posts`. Deleting a user who has authored
  posts will fail with a foreign key violation unless posts are removed first.
- `body` was renamed from `content` in the final backend implementation. All SQL
  files and the ETL service use `body`.

**Foreign keys:**

| Column      | References     | On Delete |
|-------------|----------------|-----------|
| `author_id` | `users(id)`    | RESTRICT  |

---

### 1.3 comments

Stores comments left by users on posts. Comments are always associated with
exactly one post and one author.

| Column       | Data Type   | Nullable | Default | Constraints                    | Description                                     |
|--------------|-------------|----------|---------|--------------------------------|-------------------------------------------------|
| `id`         | `BIGSERIAL` | NOT NULL | auto    | PRIMARY KEY                    | Auto-incrementing unique identifier             |
| `content`    | `TEXT`      | NOT NULL |         |                                | Comment body text. No length cap enforced in DB |
| `post_id`    | `BIGINT`    | NOT NULL |         | FOREIGN KEY → `posts(id)`     | The post this comment belongs to                |
| `author_id`  | `BIGINT`    | NOT NULL |         | FOREIGN KEY → `users(id)`     | The user who wrote the comment                  |
| `created_at` | `TIMESTAMP` | NOT NULL | `NOW()` |                                | Timestamp of comment creation                   |

**Notes:**
- Comments are cascade-deleted when their parent post is deleted. This is
  enforced at the database level: `ON DELETE CASCADE` on `post_id`.
- There is no `updated_at` on comments. Comments are not editable in the
  current MVP scope.
- The analytics summary view counts all rows in this table to derive
  `total_comments`.

**Foreign keys:**

| Column      | References  | On Delete |
|-------------|-------------|-----------|
| `post_id`   | `posts(id)` | CASCADE   |
| `author_id` | `users(id)` | RESTRICT  |

---

## 2. Analytics Materialized Views

These views are created and refreshed by the ETL service. The Spring Boot
backend reads from them to serve the `/api/analytics/*` endpoints. They must
never be written to directly.

All views use `REFRESH MATERIALIZED VIEW CONCURRENTLY`, which requires a unique
index on each view. Read queries against the views are not blocked during a
refresh.

---

### 2.1 analytics_summary

A single-row view containing platform-wide aggregate counts.

| Column           | Data Type | Description                              |
|------------------|-----------|------------------------------------------|
| `total_posts`    | `BIGINT`  | Total number of rows in the `posts` table |
| `total_comments` | `BIGINT`  | Total number of rows in the `comments` table |

**Unique index:** `idx_analytics_summary_posts` on `(total_posts)`

**Refresh trigger:** Any change to `posts` or `comments`

**API endpoint:** `GET /api/analytics/summary`

---

### 2.2 analytics_posts_by_category

One row per category, always including all four categories even when a category
has zero posts. Achieved using a `VALUES` table left-joined against `posts`.

| Column          | Data Type | Description                                       |
|-----------------|-----------|---------------------------------------------------|
| `category_name` | `TEXT`    | Category name: `NEWS`, `EVENT`, `DISCUSSION`, or `ALERT` |
| `post_count`    | `BIGINT`  | Number of posts in this category                  |

**Unique index:** `idx_analytics_category_name` on `(category_name)`

**Refresh trigger:** Any change to `posts`

**API endpoint:** `GET /api/analytics/posts-by-category`

**Query ordering:** `ORDER BY post_count DESC` must be applied by the API query.
The view definition does not guarantee row order.

---

### 2.3 analytics_posts_by_day

One row per day of the week, always including all seven days even when a day
has zero posts. Achieved using a `VALUES` table left-joined against `posts`.

| Column       | Data Type | Description                                              |
|--------------|-----------|----------------------------------------------------------|
| `day_name`   | `TEXT`    | Three-letter day abbreviation: `Sun`, `Mon`, ..., `Sat`  |
| `day_order`  | `INTEGER` | Numeric sort key: 0=Sun, 1=Mon, 2=Tue, ..., 6=Sat       |
| `post_count` | `BIGINT`  | Number of posts created on this day of the week          |

**Unique index:** `idx_analytics_day_order` on `(day_order)`

**Refresh trigger:** Any change to `posts`

**API endpoint:** `GET /api/analytics/posts-by-day`

**Query ordering:** `ORDER BY day_order ASC` must be applied by the API query.

**Notes:**
- `day_order` follows the PostgreSQL `EXTRACT(DOW FROM timestamp)` convention
  where Sunday = 0, not Monday = 0.
- Post counts are based on the `created_at` timestamp column, not `updated_at`.

---

### 2.4 analytics_top_contributors

The top ten users by number of posts authored. Users with zero posts are
excluded. Tied users share the same `true_rank`.

| Column              | Data Type | Description                                                      |
|---------------------|-----------|------------------------------------------------------------------|
| `etl_rank`          | `BIGINT`  | Unique sequential rank used for the unique index. Uses `ROW_NUMBER()` |
| `true_rank`         | `BIGINT`  | Tie-aware rank. Users with equal post counts share the same rank. Uses `RANK()` |
| `contributor_name`  | `TEXT`    | User's display name from the `users` table                       |
| `contributor_email` | `TEXT`    | User's email address from the `users` table                      |
| `post_count`        | `BIGINT`  | Number of posts authored by this user                            |

**Unique index:** `idx_analytics_contributor_etl_rank` on `(etl_rank)`

**Refresh trigger:** Any change to `posts` or `users`

**API endpoint:** `GET /api/analytics/top-contributors`

**Query ordering:** `ORDER BY etl_rank ASC` must be applied by the API query.
The API exposes `true_rank` (not `etl_rank`) as the visible rank field.

**Notes:**
- The view is limited to 10 rows in the view definition.
- When users have equal post counts, they receive the same `true_rank` and the
  next rank is skipped (standard competition ranking: 1, 2, 2, 4, ...).

---

## 3. Triggers

### notify_analytics_refresh (function)

A PL/pgSQL trigger function that fires a `pg_notify` call on the
`analytics_refresh` channel whenever a tracked table changes. The payload is a
JSON object containing the table name, operation type, and timestamp.

**Payload structure:**

```json
{
  "table":     "posts",
  "operation": "INSERT",
  "timestamp": "2026-03-12T10:00:00.000000"
}
```

### trg_posts_analytics

| Property  | Value                                   |
|-----------|-----------------------------------------|
| Table     | `posts`                                 |
| Events    | `AFTER INSERT OR UPDATE OR DELETE`      |
| Scope     | `FOR EACH ROW`                          |
| Function  | `notify_analytics_refresh()`            |

### trg_comments_analytics

| Property  | Value                                   |
|-----------|-----------------------------------------|
| Table     | `comments`                              |
| Events    | `AFTER INSERT OR UPDATE OR DELETE`      |
| Scope     | `FOR EACH ROW`                          |
| Function  | `notify_analytics_refresh()`            |

### trg_users_analytics

| Property  | Value                                   |
|-----------|-----------------------------------------|
| Table     | `users`                                 |
| Events    | `AFTER INSERT OR UPDATE OR DELETE`      |
| Scope     | `FOR EACH ROW`                          |
| Function  | `notify_analytics_refresh()`            |

**Notes:**
- On `DELETE` operations the trigger returns `OLD` instead of `NEW`. This is
  handled in the trigger function body.
- The NOTIFY signal wakes the Python ETL listener, which then refreshes all
  four materialized views concurrently.

---

## 4. Indexes

### Application table indexes

| Index name               | Table      | Column(s)    | Type   | Purpose                                        |
|--------------------------|------------|--------------|--------|------------------------------------------------|
| `idx_posts_category`     | `posts`    | `category`   | BTREE  | Speeds up category filter queries and the category analytics view |
| `idx_posts_created_at`   | `posts`    | `created_at` | BTREE  | Speeds up date-range queries and day-of-week analytics |
| `idx_posts_author_id`    | `posts`    | `author_id`  | BTREE  | Speeds up join to users for contributor ranking |
| `idx_comments_post_id`   | `comments` | `post_id`    | BTREE  | Speeds up comment count queries per post       |
| `idx_comments_author_id` | `comments` | `author_id`  | BTREE  | Speeds up join to users                        |

### Analytics view unique indexes

These indexes are required for `REFRESH MATERIALIZED VIEW CONCURRENTLY` to
work. Without them, the non-blocking refresh will fail.

| Index name                          | View                             | Column(s)    |
|-------------------------------------|----------------------------------|--------------|
| `idx_analytics_summary_posts`       | `analytics_summary`              | `total_posts` |
| `idx_analytics_category_name`       | `analytics_posts_by_category`    | `category_name` |
| `idx_analytics_day_order`           | `analytics_posts_by_day`         | `day_order`  |
| `idx_analytics_contributor_etl_rank`| `analytics_top_contributors`     | `etl_rank`   |

---

## 5. Enumerations

### Post categories

Categories are stored as `VARCHAR(20)` in the `posts` table. The four valid
values are fixed by the application layer and the seed data.

| Value        | Description                                         |
|--------------|-----------------------------------------------------|
| `NEWS`       | Local neighborhood news and official announcements  |
| `EVENT`      | Upcoming events, gatherings, and activities         |
| `DISCUSSION` | Open conversations and community feedback           |
| `ALERT`      | Urgent safety notices and time-sensitive warnings   |

### User roles

Roles are stored as `VARCHAR(10)` in the `users` table.

| Value   | Description                                              |
|---------|----------------------------------------------------------|
| `USER`  | Standard registered resident. Default on account creation |
| `ADMIN` | Platform administrator with elevated privileges          |

---

## 6. Seed Data Summary

The ETL service loads seed data via `03_seed_data.sql` on first startup if the
`users` table is empty. This guard prevents duplicate inserts on container
restarts.

| Entity    | Count | Notes                                                              |
|-----------|-------|--------------------------------------------------------------------|
| Users     | 20    | 19 standard users + 1 admin. All use password `Password123!`      |
| Posts     | 60    | 18 NEWS, 18 EVENT, 15 DISCUSSION, 9 ALERT. Spread over 90 days    |
| Comments  | 214   | Distributed across 30 of the 60 posts. Between 2 and 6 per post   |

**Test credentials:**

| Email                        | Password      | Role  |
|------------------------------|---------------|-------|
| `john.smith@email.com`       | `Password123!`| USER  |
| `admin@communityboard.com`   | `Password123!`| ADMIN |

All seed users share the same password for testing convenience. Seed data
should be removed or replaced before production deployment.