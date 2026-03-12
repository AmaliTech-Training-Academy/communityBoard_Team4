# CB-213 Technical Documentation — Concurrency Safety

## Overview

CB-213 adds concurrency safety to the CommunityBoard backend via optimistic locking on the `Post` entity and explicit `@Transactional` boundaries on all service methods.

---

## 1. Optimistic Locking — `@Version` on `Post`

### What it does

JPA optimistic locking prevents **lost updates** without holding database locks. A `version` column is added to the `posts` table. Every `UPDATE` increments the version automatically. If two concurrent transactions both read `version = 0` and both attempt a write, the second write fails because the DB row now has `version = 1`.

### Implementation

```java
// Post.java
@Version
@Column(nullable = false)
private Long version;
```

- **Initial value:** `0` (set by JPA on first insert)
- **Incremented by:** JPA on every `UPDATE` statement
- **Failure mode:** `ObjectOptimisticLockingFailureException` → mapped to HTTP 409

---

## 2. Exception Handling — HTTP 409 Conflict

### `ConflictException`

```java
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
```

Throw this from service code for domain-level conflicts (e.g., duplicate resource that should be a 409, not a 422).

### `GlobalExceptionHandler` additions

```java
@ExceptionHandler(ConflictException.class)
public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
}

@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ApiErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    return build(HttpStatus.CONFLICT,
            "This resource was updated by another request. Please reload and try again.");
}
```

Both return `HTTP 409 Conflict` with a structured `ApiErrorResponse` body.

---

## 3. Transactional Boundaries

### PostService

| Method | Annotation |
|---|---|
| `getAllPosts()` | `@Transactional(readOnly = true)` |
| `searchPosts()` | `@Transactional(readOnly = true)` |
| `getPostById()` | `@Transactional(readOnly = true)` |
| `createPost()` | `@Transactional` |
| `updatePost()` | `@Transactional` |
| `deletePost()` | `@Transactional` |

### CommentService

| Method | Annotation |
|---|---|
| `getCommentsByPost()` | `@Transactional(readOnly = true)` |
| `createComment()` | `@Transactional` |
| `updateComment()` | `@Transactional` |
| `deleteComment()` | `@Transactional` |

**Why `readOnly=true`?** Hibernate skips dirty-checking on read-only transactions, reducing memory overhead and preventing accidental flushes.

---

## 4. Concurrency Tests — `PostConcurrencyTest`

| Test | What it verifies |
|---|---|
| `newPost_hasVersionZero` | `@Version` initialises to `0` on first persist |
| `successfulUpdate_incrementsVersion` | Version increments `0 → 1 → 2` on sequential writes |
| `concurrentUpdate_throwsOptimisticLockException` | Saving a stale entity throws `ObjectOptimisticLockingFailureException` |
| `twoThreadsConcurrentUpdate_onlyOneSucceeds` | Two simultaneous writes → exactly 1 success + 1 failure |
| `savedPost_isRetrievableById` | Basic persistence smoke-test |
| `deletePost_removesFromRepository` | Delete cascade smoke-test |

All tests use `@SpringBootTest` with H2 in-memory DB. `@DirtiesContext(AFTER_EACH_TEST_METHOD)` ensures a clean DB per test.

---

## 5. Security Notes

- The version field is **read-only to clients** — it is never included in `PostRequest` DTO, so clients cannot manipulate it.
- HTTP 409 responses do not expose internal entity state or Hibernate details — only a user-friendly message.
- `@Transactional` boundaries ensure that partial writes are rolled back on any exception.
