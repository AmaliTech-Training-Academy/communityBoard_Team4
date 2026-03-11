# CB-212: Async Processing — Technical Documentation

## Overview

CB-212 introduces asynchronous analytics event tracking for post operations. When a user views, creates, or deletes a post, a fire-and-forget analytics event is dispatched to a dedicated thread pool. The HTTP request thread returns immediately without waiting for analytics to complete.

---

## Architecture

```
HTTP Request Thread                   analyticsExecutor Thread Pool
──────────────────                    ─────────────────────────────
                                       ┌─────────────────────────┐
  PostController                       │  Thread: async-analytics-1│
       │                               │  Thread: async-analytics-2│
       ▼                               │  (up to 10 threads)        │
  PostService                          └─────────────────────────┘
       │  ← DB work (synchronous)              ▲
       │                                       │  @Async dispatch
       │  ── return response ──►  caller       │
       │                                       │
       └──── postAnalyticsService.record*() ───┘
              (returns Future<Void> immediately)
```

---

## Configuration (`AppConfig.java`)

```java
@Bean(name = "analyticsExecutor")
public Executor analyticsExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);       // Always-alive threads
    executor.setMaxPoolSize(10);       // Max under burst load
    executor.setQueueCapacity(100);    // Queue before spawning beyond core
    executor.setThreadNamePrefix("async-analytics-");
    executor.initialize();
    return executor;
}
```

### Thread pool sizing rationale

| Parameter | Value | Reasoning |
|---|---|---|
| `corePoolSize` | 2 | Low baseline — analytics is low-throughput |
| `maxPoolSize` | 10 | Handles traffic spikes without unbounded threads |
| `queueCapacity` | 100 | Absorbs bursts; tasks queue before new threads spawn |
| Thread prefix | `async-analytics-` | Identifiable in logs and thread dumps |

---

## `PostAnalyticsService`

All methods are `void` and annotated with `@Async("analyticsExecutor")`. Spring wraps the bean in a proxy at startup — every call is intercepted and submitted to the executor instead of running inline.

```java
@Async("analyticsExecutor")
public void recordPostView(Long postId, String viewerEmail)

@Async("analyticsExecutor")
public void recordPostCreated(Long postId, String authorEmail, String category)

@Async("analyticsExecutor")
public void recordPostDeleted(Long postId, String deletedBy)
```

**Current analytics sink:** `@Slf4j` structured logs in the format:
```
[ANALYTICS] post_view | postId=42 viewer=alice@test.com thread=async-analytics-1
```

---

## Call Sites in `PostService`

| Method | Analytics call |
|---|---|
| `getPostById(id, viewerEmail)` | `recordPostView(id, viewerEmail)` |
| `createPost(request, author)` | `recordPostCreated(saved.getId(), author.getEmail(), category.name())` |
| `deletePost(id, author)` | `recordPostDeleted(id, author.getEmail())` |

`getPostById` signature was extended with `viewerEmail`. `PostController` extracts the email from `@AuthenticationPrincipal User viewer`, defaulting to `"anonymous"` for unauthenticated requests.

---

## Testing Strategy (`PostAsyncTest`)

Uses `@SpringBootTest` (full context) with `@SpyBean PostAnalyticsService` so the real `@Async` proxy chain is active. `@MockBean` intercepts the DB repositories.

Mockito's `verify(..., timeout(N))` is used instead of `Thread.sleep()` to wait for async dispatch with a deterministic upper bound.

| Test | What it proves |
|---|---|
| `getPostById_triggersAsyncViewEvent` | View event fires after `getPostById` |
| `getPostById_anonymousViewer_...` | `"anonymous"` is passed correctly for unauthenticated callers |
| `createPost_triggersAsyncCreatedEvent` | Create event fires with correct postId, email, category |
| `deletePost_triggersAsyncDeletedEvent` | Delete event fires with correct postId and deleter email |
| `recordPostView_runsOnAnalyticsExecutorThread` | Async dispatch confirmed via `timeout()` |
| `getPostById_doesNotBlockCallingThread` | Service returns in < 100ms (analytics is fire-and-forget) |

---

## Trade-offs

| Concern | Decision |
|---|---|
| **Reliability** | Events are best-effort — a server crash loses queued analytics. Acceptable for current scale. |
| **Error handling** | `@Async` swallows exceptions by default. Failures are logged at WARN level by Spring's `SimpleAsyncUncaughtExceptionHandler`. |
| **Observability** | Thread prefix `async-analytics-` makes it easy to filter analytics threads in logs. |
| **In-memory only** | Thread pool is per-instance. No distribution across nodes. |

---

## Future Upgrade Path

1. **Persistent events** — Replace log statements with writes to an `analytics_events` table or a message queue (Kafka, AWS SQS) for durability and cross-service consumption.
2. **Custom exception handler** — Implement `AsyncUncaughtExceptionHandler` to alert on analytics failures without crashing the request thread.
3. **Distributed tracing** — Propagate the MDC/trace context into the async thread for end-to-end correlation (Spring's `TaskDecorator` API).
4. **Rate limiting** — Add a `RateLimitingTaskDecorator` to the executor if analytics volume becomes a concern.
