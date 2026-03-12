# CB-211 Technical Documentation: Posts Caching

**Epic:** CB-210 Backend Performance Optimization
**Story:** CB-211 Implement Application Caching
**Author:** Backend Team
**Date:** 2026-03-11

---

## 1. Overview

This document describes the caching strategy implemented for the `PostService` layer as part of the CB-211 user story. The goal is to reduce repeated database queries for frequently accessed post data, resulting in faster API responses and lower database load.

---

## 2. Technology Used

| Component | Technology | Reason |
|-----------|-----------|--------|
| Cache abstraction | `spring-boot-starter-cache` | Standard Spring Cache API — provider-agnostic (can swap to Redis later) |
| Cache provider | `ConcurrentMapCacheManager` | In-memory, zero-dependency, thread-safe — suitable for single-instance deployments |
| Annotations | `@Cacheable`, `@CacheEvict`, `@Caching` | Declarative; applied at service method level, no boilerplate |

---

## 3. Architecture

```
HTTP Request
     │
     ▼
PostController
     │
     ▼
PostService ─── @Cacheable ──► Cache Hit? ──YES──► Return cached value
     │                                                     │
     │                         NO                         │
     ▼                                                     │
PostRepository (DB)                                       │
     │                                                     │
     └───────────── Store in cache ◄──────────────────────┘
```

When a mutation occurs (create / update / delete), `@CacheEvict` removes the stale entries so subsequent reads fetch fresh data from the database.

---

## 4. Configuration

### 4.1 Dependency (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

Spring Boot's `spring-boot-starter-cache` brings in the Spring Cache abstraction. No external cache server is required — `ConcurrentMapCacheManager` is used as the in-memory provider.

### 4.2 `AppConfig.java`

```java
@Configuration
@EnableCaching                          // activates Spring's caching proxy infrastructure
public class AppConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("posts", "post");
    }
}
```

`@EnableCaching` tells Spring to post-process beans annotated with `@Cacheable`/`@CacheEvict` etc., wrapping them in a caching proxy (uses Spring AOP).

Two named cache regions are declared:

| Cache name | Stores | Evicted when |
|-----------|--------|-------------|
| `"posts"` | Paginated `Page<PostResponse>` results | Any `createPost`, `updatePost`, or `deletePost` call |
| `"post"`  | Single `PostResponse` by ID | The specific post is updated or deleted |

---

## 5. Cache Annotations Applied

### 5.1 `@Cacheable` — Read-through caching

**`getAllPosts(int page, int size)`**

```java
@Cacheable(value = "posts", key = "'page-' + #page + '-size-' + #size")
public Page<PostResponse> getAllPosts(int page, int size) { ... }
```

- Cache key: `page-0-size-10`, `page-1-size-10`, `page-0-size-20`, etc.
- Each unique `(page, size)` combination is cached separately.
- On first call: DB is queried; result stored in cache.
- On subsequent calls with the same params: DB is **not** queried; result returned from memory.

**`getPostById(Long id)`**

```java
@Cacheable(value = "post", key = "#id")
public PostResponse getPostById(Long id) { ... }
```

- Cache key: the post's ID (e.g. `1`, `42`).
- Repeated calls to `GET /api/posts/42` skip the DB after the first hit.

> **Note:** `searchPosts()` is intentionally **not** cached — its parameter space (keyword, category, date range, page, size) is too large and varied for effective caching.

---

### 5.2 `@CacheEvict` — Invalidation on create

**`createPost(PostRequest, User)`**

```java
@CacheEvict(value = "posts", allEntries = true)
public PostResponse createPost(PostRequest request, User author) { ... }
```

- Evicts **all entries** in the `"posts"` cache (all pages are now stale).
- `"post"` cache is not affected — no existing single-post entry changes on create.

---

### 5.3 `@Caching` — Multiple evictions on update / delete

**`updatePost(Long id, PostRequest, User)`**

```java
@Caching(evict = {
    @CacheEvict(value = "post",  key = "#id"),
    @CacheEvict(value = "posts", allEntries = true)
})
public PostResponse updatePost(Long id, PostRequest request, User author) { ... }
```

- Removes the specific post entry from `"post"` cache.
- Clears all paginated pages from `"posts"` cache (since any page might contain this post).

**`deletePost(Long id, User)`**

```java
@Caching(evict = {
    @CacheEvict(value = "post",  key = "#id"),
    @CacheEvict(value = "posts", allEntries = true)
})
public void deletePost(Long id, User author) { ... }
```

- Same eviction strategy as `updatePost`.

---

## 6. Cache Lifecycle Diagram

```
getAllPosts(0, 10)   ──► [cache miss]  ── DB query ──► stored as key "page-0-size-10"
getAllPosts(0, 10)   ──► [cache HIT ]  ── no DB ──────► returned from memory
createPost(...)     ──► @CacheEvict(posts, allEntries=true) → "page-0-size-10" removed
getAllPosts(0, 10)   ──► [cache miss]  ── DB query ──► re-cached

getPostById(5)      ──► [cache miss]  ── DB query ──► stored as key "5"
getPostById(5)      ──► [cache HIT ]  ── no DB ──────► returned from memory
updatePost(5, ...)  ──► @CacheEvict(post, key=5) + @CacheEvict(posts, allEntries=true)
getPostById(5)      ──► [cache miss]  ── DB query ──► re-cached
```

---

## 7. Trade-offs and Limitations

| Concern | Detail |
|---------|--------|
| **In-memory only** | `ConcurrentMapCacheManager` lives in the JVM heap. Cache is lost on restart or in multi-instance deployments (use Redis for distributed caching). |
| **No TTL (time-to-live)** | Entries persist until explicitly evicted. Stale data can only occur if eviction is misconfigured. |
| **Self-invocation** | Spring's caching proxy is AOP-based. Calling a `@Cacheable` method from *within the same bean* bypasses the cache. All cached methods are only called from controllers — this is safe. |
| **`searchPosts` not cached** | Intentional. The parameter space is too broad for effective cache key design. |

---

## 8. Future Improvements

If the application scales to multiple instances, `ConcurrentMapCacheManager` should be replaced with **Redis**:

```xml
<!-- Replace spring-boot-starter-cache with: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
// AppConfig.java — update to:
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10));
    return RedisCacheManager.builder(factory)
        .cacheDefaults(config).build();
}
```

This requires no changes to `PostService` — the cache annotations remain identical.

---

## 9. Unit Test Coverage

| Test | Assertion |
|------|-----------|
| `getAllPosts_cacheHitOnSecondCall` | `findAllByOrderByCreatedAtDesc` called exactly 1 time for 2 identical requests |
| `getAllPosts_differentPageParams_bothHitRepository` | Different page numbers produce 2 DB hits (different cache keys) |
| `getPostById_cacheHitOnSecondCall` | `findById` called exactly 1 time for 2 identical requests |
| `createPost_evictsPostsCache` | After `createPost`, next `getAllPosts` hits DB again (total 2 DB hits) |
| `updatePost_evictsPostAndPostsCache` | After `updatePost`, next `getPostById` hits DB again (3 total: getById → updatePost lookup → getById again) |
| `deletePost_evictsPostAndPostsCache` | After `deletePost`, next `getPostById` hits DB again (3 total: getById → deletePost lookup → getById again) |

**All 6 tests pass: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`**

---

## 10. Related Files

| File | Role |
|------|------|
| [config/AppConfig.java](../src/main/java/com/amalitech/communityboard/config/AppConfig.java) | Declares `@EnableCaching` and `CacheManager` bean |
| [service/PostService.java](../src/main/java/com/amalitech/communityboard/service/PostService.java) | Contains all `@Cacheable` / `@CacheEvict` / `@Caching` annotations |
| [test/.../PostCacheTest.java](../src/test/java/com/amalitech/communityboard/service/PostCacheTest.java) | Unit tests for cache behaviour |
| [CB-211-DONE.md](./CB-211-DONE.md) | Progress tracker for this story |
