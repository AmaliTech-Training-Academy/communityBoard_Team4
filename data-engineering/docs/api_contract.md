# API Contract — CommunityBoard Analytics

**Version:** 1.0  
**Prepared by:** Data Engineering  
**Audience:** Backend (Spring Boot) and Frontend (React) teams  
**Base URL:** `http://localhost:8080`

---

## Overview

This document defines the four analytics endpoints that the Spring Boot backend
must expose. All endpoints read directly from PostgreSQL materialized views that
are maintained in real time by the ETL service. No business logic is required in
the controller layer — each endpoint is a straightforward projection of its
corresponding view.

The ETL service guarantees the views are refreshed within seconds of any insert,
update, or delete on the `posts`, `comments`, or `users` tables via a
PostgreSQL LISTEN/NOTIFY trigger pipeline.

---

## General Conventions

| Property        | Value                                    |
|-----------------|------------------------------------------|
| Protocol        | HTTP/1.1                                 |
| Data format     | JSON (`application/json`)                |
| Authentication  | JWT Bearer token (same as all endpoints) |
| Error format    | Spring Boot default (`timestamp`, `status`, `error`, `message`, `path`) |
| Ordering        | Defined per endpoint — must be applied in the query, not the client |

All successful responses return HTTP `200 OK`. Empty result sets return an empty
array `[]` rather than `404`.

---

## Endpoints

---

### GET /api/analytics/summary

Returns the total count of posts and comments across the entire platform.
Powers the two summary cards at the top of the analytics dashboard.

**Authentication:** Required (any authenticated user)

**Query parameters:** None

**Response body:**

```json
{
  "totalPosts": 60,
  "totalComments": 214
}
```

**Field definitions:**

| Field           | Type    | Source view column | Description                        |
|-----------------|---------|--------------------|------------------------------------|
| `totalPosts`    | integer | `total_posts`      | Count of all rows in `posts`       |
| `totalComments` | integer | `total_comments`   | Count of all rows in `comments`    |

**Backing view:** `analytics_summary`

**Suggested Spring Boot query:**

```sql
SELECT total_posts, total_comments FROM analytics_summary;
```

**Notes:**
- This view always contains exactly one row.
- Both values are non-negative. A value of `0` is valid when no data exists.

---

### GET /api/analytics/posts-by-category

Returns the post count for each of the four categories. Powers the bar chart
labelled "Posts by Category" on the analytics dashboard.

**Authentication:** Required (any authenticated user)

**Query parameters:** None

**Response body:**

```json
[
  { "categoryName": "NEWS",       "postCount": 18 },
  { "categoryName": "EVENT",      "postCount": 18 },
  { "categoryName": "DISCUSSION", "postCount": 15 },
  { "categoryName": "ALERT",      "postCount": 9  }
]
```

**Field definitions:**

| Field      | Type    | Source view column | Description                                     |
|------------|---------|--------------------|--------------------------------------------------|
| `categoryName` | string  | `category_name`    | One of: `NEWS`, `EVENT`, `DISCUSSION`, `ALERT`  |
| `postCount`    | integer | `post_count`       | Number of posts in this category                 |

**Backing view:** `analytics_posts_by_category`

**Suggested Spring Boot query:**

```sql
SELECT category_name AS category, post_count AS count
FROM   analytics_posts_by_category
ORDER  BY post_count DESC;
```

**Notes:**
- All four categories are always present in the response, even if a category
  has zero posts. The view uses a `VALUES` table to guarantee this.
- The frontend must not assume a fixed order. Always apply `ORDER BY` in
  the query and present the data in the order returned.

---

### GET /api/analytics/posts-by-day

Returns the post count grouped by day of the week. Powers the bar chart
labelled "Posts by Day of Week" on the analytics dashboard.

**Authentication:** Required (any authenticated user)

**Query parameters:** None

**Response body:**

```json
[
  { "dayName": "Sun", "dayOrder": 0, "postCount": 8  },
  { "dayName": "Mon", "dayOrder": 1, "postCount": 12 },
  { "dayName": "Tue", "dayOrder": 2, "postCount": 9  },
  { "dayName": "Wed", "dayOrder": 3, "postCount": 11 },
  { "dayName": "Thu", "dayOrder": 4, "postCount": 7  },
  { "dayName": "Fri", "dayOrder": 5, "postCount": 6  },
  { "dayName": "Sat", "dayOrder": 6, "postCount": 7  }
]
```

**Field definitions:**

| Field      | Type    | Source view column | Description                                      |
|------------|---------|--------------------|--------------------------------------------------|
| `dayName`      | string  | `day_name`         | Three-letter day abbreviation (Sun through Sat)  |
| `dayOrder` | integer | `day_order`        | Numeric sort key: 0=Sun, 1=Mon, ..., 6=Sat      |
| `postCount`    | integer | `post_count`       | Number of posts created on this day of the week  |

**Backing view:** `analytics_posts_by_day`

**Suggested Spring Boot query:**

```sql
SELECT day_name  AS day,
       day_order AS "dayOrder",
       post_count AS count
FROM   analytics_posts_by_day
ORDER  BY day_order ASC;
```

**Notes:**
- All seven days are always present, including days with zero posts. The view
  uses a `VALUES` table to guarantee this.
- The frontend must use `dayOrder` for chart axis ordering, not alphabetical
  sort of the `day` string.
- `day_order` follows PostgreSQL `EXTRACT(DOW ...)` convention: Sunday = 0.

---

### GET /api/analytics/contributors/top

Returns the top ten users ranked by number of posts authored. Powers the
"Top Contributors" table on the analytics dashboard.

**Authentication:** Required (any authenticated user)

**Query parameters:** None

**Response body:**

```json
[
  { "trueRank": 1, "etlRank": 1, "contributorName": "John Smith",          "contributorEmail": "john.smith@email.com",          "postCount": 7 },
  { "trueRank": 2, "etlRank": 2, "contributorName": "Brooklyn Simmons",    "contributorEmail": "brooklyn.simmons@email.com",    "postCount": 5 },
  { "trueRank": 2, "etlRank": 3, "contributorName": "Kristin Watson",      "contributorEmail": "kristin.watson@email.com",      "postCount": 5 },
  { "trueRank": 4, "etlRank": 4, "contributorName": "Courtney Henry",      "contributorEmail": "courtney.henry@email.com",      "postCount": 4 },
  { "trueRank": 5, "etlRank": 5, "contributorName": "Leslie Alexander",    "contributorEmail": "leslie.alexander@email.com",    "postCount": 3 }
]
```

**Field definitions:**

| Field       | Type    | Source view column   | Description                                      |
|-------------|---------|----------------------|--------------------------------------------------|
| `trueRank`  | integer | `true_rank`          | Dense rank; tied users share the same rank value |
| `name`      | string  | `contributor_name`   | User's display name from the `users` table       |
| `email`     | string  | `contributor_email`  | User's email address                             |
| `postCount` | integer | `post_count`         | Total number of posts authored                   |
| `etlRank`   | integer | `etl_rank`           | rank; tied users share the same rank value |

**Backing view:** `analytics_top_contributors`

**Suggested Spring Boot query:**

```sql
SELECT true_rank        AS rank,
       contributor_name AS name,
       contributor_email AS email,
       post_count       AS "postCount"
FROM   analytics_top_contributors
ORDER  BY etl_rank ASC;
```

**Notes:**
- The view exposes two rank columns: `etl_rank` (unique, used for the unique
  index required by `REFRESH MATERIALIZED VIEW CONCURRENTLY`) and `true_rank`
  (handles ties with `RANK()` — users with equal post counts receive the same
  rank). The backend must expose `true_rank` as the `rank` field.
- The response is capped at ten rows by the view definition.
- The `email` field should be used for display in the contributor table only.
  It must not be used as an identifier in any write operations.

---

## View Refresh Timing

All four endpoints read from materialized views. The views are refreshed by the
ETL service via the following trigger chain:

```
INSERT / UPDATE / DELETE on posts, comments, or users
    → PostgreSQL trigger fires
    → NOTIFY sent on channel 'analytics_refresh'
    → ETL listener wakes up
    → Debounce window (1 second) collects burst notifications
    → REFRESH MATERIALIZED VIEW CONCURRENTLY for all four views
    → Views are current
```

Expected refresh latency after a data change: **under 5 seconds** under normal
load. During startup, an initial refresh is performed before the listener begins
accepting notifications.

The `CONCURRENTLY` flag means view refreshes are non-blocking — read queries
against the views continue to return results during a refresh.

---

## Error Responses

All errors follow the standard Spring Boot error format:

```json
{
  "timestamp": "2026-03-12T10:00:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/analytics/summary"
}
```

| Status | Condition                          |
|--------|------------------------------------|
| 200    | Success                            |
| 401    | Missing or invalid JWT token       |
| 403    | Authenticated but insufficient role|
| 500    | Database or server error           |

---

## Handoff Checklist

The following artefacts accompany this contract:

| Artefact                               | Location                                    |
|----------------------------------------|---------------------------------------------|
| Analytics materialized views (SQL)     | `data-engineering/database/sql/01_create_analytics_views.sql` |
| LISTEN/NOTIFY triggers (SQL)           | `data-engineering/database/sql/02_create_triggers.sql`        |
| Seed data for endpoint testing (SQL)   | `data-engineering/database/sql/03_seed_data.sql`              |
| ETL listener service                   | `data-engineering/etl_pipeline.py`                            |
| Data dictionary                        | `docs/data_dictionary.md`                                     |