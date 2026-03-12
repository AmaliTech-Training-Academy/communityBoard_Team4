# DevOps Logging & Monitoring Guide

## Overview

The backend emits **4 structured log files** and exposes **Spring Boot Actuator endpoints** for real-time monitoring. This guide covers how to verify everything is working.

---

## Log Files

All log files are written to the `logs/` directory inside the backend working directory by default. You can override the path by setting the `LOG_PATH` environment variable.

| File | Level | Purpose |
|---|---|---|
| `logs/communityboard.log` | INFO+ | All general application events |
| `logs/communityboard-error.log` | ERROR only | Exceptions and server errors |
| `logs/communityboard-performance.log` | INFO/WARN | One line per HTTP request with timing |
| `logs/communityboard-security.log` | INFO+ | Auth, JWT, login/register events |

### Log Rotation Policy
- Rolls over **daily** and when the file reaches **20 MB**
- Archives are **gzip-compressed** and kept for **30 days**
- Archived files follow the pattern: `communityboard.2026-03-12.0.log.gz`

### Overriding the Log Directory
```bash
# Linux / Docker
export LOG_PATH=/var/log/communityboard

# docker-compose.yml
environment:
  - LOG_PATH=/var/log/communityboard
```

---

## Verifying Log Output

### 1. Performance Log
Make any HTTP request to the app, then check:
```bash
tail -f logs/communityboard-performance.log
```
Expected output:
```
2026-03-12 10:15:42 INFO  [PERF] POST /api/auth/login -> 200 | 143ms | user=anonymous
2026-03-12 10:15:55 INFO  [PERF] GET /api/posts -> 200 | 88ms | user=john@example.com
```
Slow requests (over 1 second) appear as:
```
2026-03-12 10:16:01 WARN  [SLOW REQUEST] [PERF] GET /api/posts -> 200 | 1342ms | user=john@example.com
```

### 2. Security Log
Trigger a login or registration, then check:
```bash
tail -f logs/communityboard-security.log
```
Also check for failed auth attempts (401/403 responses will appear here).

### 3. Error Log
Trigger a bad request (e.g., invalid token), then check:
```bash
tail -f logs/communityboard-error.log
```
This file only writes when something breaks — it should be empty under normal operation.

### 4. Application Log
General startup and operational events:
```bash
tail -f logs/communityboard.log
```
On startup you should see Flyway migration logs like:
```
INFO  o.f.c.i.c.DbValidate - Successfully validated 5 migrations
INFO  o.f.c.i.c.DbMigrate  - Current version of schema "public": 5
```

---

## Actuator Endpoints

Base URL: `http://localhost:8080` (or your deployment host)

### Public Endpoint (no authentication required)

| Endpoint | Method | Description |
|---|---|---|
| `/actuator/health` | GET | App health status and DB connection |

Example:
```bash
curl http://localhost:8080/actuator/health
```
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

### Protected Endpoints (require ADMIN JWT token)

All other actuator endpoints require an `Authorization: Bearer <token>` header from an ADMIN-role account.

#### Get a token first:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "yourpassword"}'  # pragma: allowlist secret
```
Copy the `token` value from the response and use it in the requests below.

---

#### `/actuator/metrics` — JVM and HTTP statistics
```bash
curl http://localhost:8080/actuator/metrics \
  -H "Authorization: Bearer <token>"
```
To view a specific metric (e.g., HTTP request durations):
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests \
  -H "Authorization: Bearer <token>"
```

---

#### `/actuator/loggers` — View and change log levels at runtime
View all loggers:
```bash
curl http://localhost:8080/actuator/loggers \
  -H "Authorization: Bearer <token>"
```
View a specific logger:
```bash
curl http://localhost:8080/actuator/loggers/com.amalitech.communityboard \
  -H "Authorization: Bearer <token>"
```
Change a log level without restarting (e.g., enable DEBUG temporarily):
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.amalitech.communityboard \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```
Reset it back:
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.amalitech.communityboard \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "INFO"}'
```

---

#### `/actuator/flyway` — Migration history
```bash
curl http://localhost:8080/actuator/flyway \
  -H "Authorization: Bearer <token>"
```
Shows all Flyway migrations with their version, description, status, and timestamp.

---

#### `/actuator/env` — Active configuration
```bash
curl http://localhost:8080/actuator/env \
  -H "Authorization: Bearer <token>"
```
Shows all active Spring properties and environment variables (passwords are masked automatically).

---

#### `/actuator/threaddump` — Thread states
```bash
curl http://localhost:8080/actuator/threaddump \
  -H "Authorization: Bearer <token>"
```
Use this to diagnose hangs or deadlocks — look for threads stuck in `BLOCKED` or `WAITING` state.

---

#### `/actuator/heapdump` — Memory heap dump
```bash
curl http://localhost:8080/actuator/heapdump \
  -H "Authorization: Bearer <token>" \
  --output heapdump.hprof
```
Open the `.hprof` file with [Eclipse Memory Analyzer (MAT)](https://eclipse.dev/mat/) or IntelliJ to investigate memory leaks.

---

## Quick Verification Checklist

Use this checklist after every deployment to confirm logging is healthy:

- [ ] `GET /actuator/health` returns `{"status": "UP"}`
- [ ] `logs/communityboard.log` exists and shows Flyway migration lines on first run
- [ ] `logs/communityboard-performance.log` records a new line after making any API request
- [ ] `logs/communityboard-security.log` records a line after a login attempt
- [ ] `logs/communityboard-error.log` is either empty or contains only expected errors
- [ ] `GET /actuator/metrics` returns data (with ADMIN token)
- [ ] `GET /actuator/flyway` shows all 5 migrations with status `SUCCESS`
- [ ] Slow requests (simulate with a heavy query) appear as `[SLOW REQUEST]` in the performance log

---

## Docker / Production Notes

When running in Docker, mount a volume for logs so they persist across container restarts:

```yaml
# docker-compose.yml
services:
  backend:
    volumes:
      - ./logs:/app/logs
    environment:
      - LOG_PATH=/app/logs
```

The health endpoint is safe to expose publicly and is suitable for use as a **Docker HEALTHCHECK** or **load balancer health probe**:
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```
