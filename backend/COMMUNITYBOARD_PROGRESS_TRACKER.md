# CommunityBoard Backend — Progress Tracker

> **Project:** CommunityBoard
> **Team:** Team 4
> **Stack:** Java 17 · Spring Boot 3.2 · Spring Security (JWT) · PostgreSQL · Swagger/OpenAPI
> **Architecture:** Controller → Service → Repository → Database

---

## Phase 1 — Database Foundation

**Status:** ✅ Completed + ✅ Hardened (Flyway)

> **Last hardened:** 2026-03-09. Flyway migrations added, `ddl-auto: validate`, `Post.content` renamed to `Post.body`.

### Objectives
- Define and enforce the core database schema: `users`, `posts`, `comments`
- Replace the `Category` JPA entity with a strongly-typed Java enum (NEWS, EVENT, DISCUSSION, ALERT)
- Implement cascade delete: deleting a Post automatically deletes all of its Comments
- Add column-level size constraints to enforce data integrity
- Add database indexes on high-query columns for performance
- Fix CORS configuration conflict (consolidated into SecurityConfig)
- Introduce Flyway as the single source of truth for DDL (`ddl-auto: validate`)

### Completed Tasks
- [x] **Category enum** — `model/Category.java` converted from `@Entity` to `enum { NEWS, EVENT, DISCUSSION, ALERT }`
- [x] **Post entity** — replaced `@ManyToOne Category` with `@Enumerated(EnumType.STRING) Category`
- [x] **Post.body** — field renamed from `content` → `body` (`@Column(name="body")`); updated in PostRequest, PostResponse, PostService
- [x] **Cascade delete** — `Post.comments` annotated with `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` so deleting a Post removes all its Comments
- [x] **Column constraints** — `users.name` (100), `users.email` (150), `posts.title` (255), `category` (20 chars), `role` (10 chars)
- [x] **PostRequest validation** — `body` with `@NotBlank` + `@Size(max=10_000)` at DTO layer
- [x] **Timestamps** — `@PrePersist` / `@PreUpdate` JPA lifecycle callbacks ensure timestamps are always set
- [x] **Database indexes** — Added indexes on `posts.category`, `posts.created_at`, `posts.author_id`, `comments.post_id`, `comments.author_id` for query performance
- [x] **PostRequest DTO** — `categoryId` (Long) replaced with `category` (String) validated against enum
- [x] **PostResponse DTO** — body field, category as string; no categoryId
- [x] **PostService** — removed `CategoryRepository` dependency; category parsed via `Category.valueOf()`; all body getter/setter/builder use `.body()`
- [x] **PostRepository** — updated to use `Category` enum in queries; `searchPosts` JPQL query added
- [x] **CommentRepository** — added paginated `findByPostIdOrderByCreatedAtAsc(Long, Pageable)` overload
- [x] **CategoryController** — now returns enum values as strings; no DB call needed
- [x] **CategoryRepository** — converted to empty placeholder (no longer a Spring component)
- [x] **CORS fix** — `CorsConfig` (WebMvcConfigurer) disabled; single authoritative CORS config in `SecurityConfig.corsConfigurationSource()`; allowed origins made configurable via `cors.allowed-origins` property
- [x] **Logging** — `@Slf4j` added to PostService; key operations logged (create, update, delete)
- [x] **Flyway dependency** — `flyway-core` (9.x, Spring Boot managed) added to `pom.xml`
- [x] **V1 migration** — `db/migration/V1__create_users_posts_comments.sql`: full DDL with CHECK constraints (role, category), FK constraints, indexes
- [x] **V2 migration** — `db/migration/V2__seed_data.sql`: idempotent seed data using `body` column; `ON CONFLICT DO NOTHING`
- [x] **ddl-auto: validate** — Hibernate validates schema; Flyway owns DDL; prevents drift
- [x] **sql.init.mode: never** — `data.sql` disabled; Flyway is sole DDL+DML authority
- [x] **PHASE_1_VERIFICATION.md** — full acceptance-criteria checklist created
- [x] **BUILD SUCCESS** — `mvn clean compile` verified; only expected deprecation warnings (JwtService API — Phase 2 fix)

### Entities After Phase 1

| Entity  | Table      | Key Fields |
|---------|------------|------------|
| User    | `users`    | id, email UNIQUE (150), name (100), password BCrypt (255), role (USER/ADMIN), created_at |
| Post    | `posts`    | id, title (255), **body** TEXT, category enum/VARCHAR(20), author_id FK, created_at, updated_at |
| Comment | `comments` | id, content TEXT, post_id FK CASCADE, author_id FK, created_at |

### Database Relationships
```
User  ──(1:N)──► Post     (author_id FK)
User  ──(1:N)──► Comment  (author_id FK)
Post  ──(1:N)──► Comment  (post_id FK, CASCADE DELETE)
```

### Verification Checklist
- [x] `users`, `posts`, `comments` tables defined in `V1__create_users_posts_comments.sql`
- [x] Category CHECK constraint: only NEWS, EVENT, DISCUSSION, ALERT allowed at DB level
- [x] Role CHECK constraint: only USER, ADMIN allowed at DB level
- [x] Foreign key constraints with ON DELETE CASCADE enforced in DB
- [x] Deleting a post cascades to delete all associated comments (both DB and JPA layers)
- [x] Timestamps auto-generated via `@PrePersist` / `@PreUpdate`
- [x] Text fields have appropriate length limits (VARCHAR or `@Size` at DTO layer)
- [x] Flyway is sole migration authority; `ddl-auto: validate` enforced
- [x] `mvn clean compile` → BUILD SUCCESS ✅

---

## Phase 2 — Authentication & Authorization

**Status:** ✅ Completed

### Objectives
- Implement user registration with BCrypt password hashing
- Implement login with JWT token generation
- Enforce role-based access control (USER / ADMIN)
- Add Swagger documentation to auth endpoints

### Completed Tasks
- [x] **Registration endpoint** — `POST /api/auth/register` (name, email, password → JWT token)
- [x] **Login endpoint** — `POST /api/auth/login` (email, password → JWT token)
- [x] **BCrypt** — passwords hashed via `BCryptPasswordEncoder` (configured as a Spring bean)
- [x] **JWT** — tokens generated via JJWT 0.12.3; secret + expiration configurable in `application.yml`
- [x] **JwtAuthFilter** — validates Bearer token, loads user from DB, sets `SecurityContext`
- [x] **RBAC** — Spring Security rules: `ROLE_USER` and `ROLE_ADMIN`; admin overrides enforced in services
- [x] **Input validation** — `@NotBlank`, `@Email`, `@Size` on RegisterRequest/AuthRequest
- [x] **Duplicate email** — throws exception if email already registered
- [x] **Password never returned** — AuthResponse contains only token, name, email, role
- [x] **@EnableMethodSecurity** — enabled in SecurityConfig for method-level `@PreAuthorize` support
- [x] **Swagger annotations** — `@Operation`, `@ApiResponse` on AuthController

### Endpoints Implemented
| Method | Endpoint             | Auth | Description |
|--------|----------------------|------|-------------|
| POST   | `/api/auth/register` | None | Register new USER account |
| POST   | `/api/auth/login`    | None | Login and receive JWT token |

### Verification Checklist
- [x] Registration requires name, email, password
- [x] Duplicate email returns clear error message
- [x] Passwords hashed using BCrypt (never stored in plain text)
- [x] Password never in API response
- [x] Successful registration returns name, email, role
- [x] Login returns valid JWT token
- [x] Requests without token to protected endpoints → 401
- [x] Requests with insufficient role → 403

---

## Phase 3 — Post Management

**Status:** ✅ Completed

### Objectives
- Full CRUD API for posts
- Category validation against enum
- Ownership + admin authorization for update/delete
- Cascade delete of comments when post is deleted

### Endpoints Implemented
| Method | Endpoint           | Auth     | Description |
|--------|--------------------|----------|-------------|
| POST   | `/api/posts`       | Required | Create a new post |
| GET    | `/api/posts`       | None     | Get all posts (paginated) |
| GET    | `/api/posts/{id}`  | None     | Get single post with comment count |
| PUT    | `/api/posts/{id}`  | Required | Update post (author or admin) |
| DELETE | `/api/posts/{id}`  | Required | Delete post + all its comments |

### Verification Checklist
- [x] Post creation requires title, content, and valid category
- [x] Invalid category returns validation error
- [x] Single post response includes: author name, category, timestamps, comment count
- [x] Non-existent post returns 404
- [x] Post author OR admin can update
- [x] Post author OR admin can delete
- [x] Deleting a post deletes all associated comments (cascade)

---

## Phase 4 — Comment System

**Status:** ✅ Completed

### Objectives
- Create, list (paginated), and delete comments
- Enforce authentication and ownership for create/delete

### Endpoints Implemented
| Method | Endpoint                          | Auth     | Description |
|--------|-----------------------------------|----------|-------------|
| POST   | `/api/posts/{postId}/comments`    | Required | Add comment to a post |
| GET    | `/api/posts/{postId}/comments`    | None     | Get paginated comments for a post |
| DELETE | `/api/comments/{id}`              | Required | Delete comment (author or admin) |

### Verification Checklist
- [x] Comment creation requires valid JWT token
- [x] Commenting on non-existent post returns 404
- [x] Comments returned paginated (with totalElements, totalPages, pageNumber, pageSize)
- [x] Response includes author name and timestamp
- [x] Comment author OR admin can delete a comment
- [x] Deleting a post deletes all its comments

---

## Phase 5 — Search and Filtering

**Status:** ✅ Completed

### Objectives
- Implement search endpoint with keyword, category, and date range filters
- Paginated results with total count and page info

### Endpoints Implemented
| Method | Endpoint              | Auth | Description |
|--------|-----------------------|------|-------------|
| GET    | `/api/posts/search`   | None | Filter/search posts with pagination |

**Query Parameters:**
- `keyword` — case-insensitive search in title and content
- `category` — filter by category (NEWS / EVENT / DISCUSSION / ALERT)
- `startDate` — inclusive start date (ISO 8601)
- `endDate` — inclusive end date (ISO 8601)
- `page` — page number (default 0)
- `size` — page size (default 10)

### Verification Checklist
- [x] Posts filterable by category
- [x] Keyword search is case-insensitive
- [x] Keyword searches title AND content
- [x] Date filters are inclusive
- [x] Empty results return 200 with empty list
- [x] Results paginated with totalElements, totalPages, pageNumber, pageSize

---

## Phase 6 — Error Handling and Validation

**Status:** ✅ Completed

### Objectives
- Global exception handler using `@RestControllerAdvice`
- Consistent API error response format
- Input validation with clear messages

### Error Response Format
```json
{
  "status": 404,
  "message": "Post not found with id: 42",
  "timestamp": "2026-03-09T10:00:00"
}
```

### Completed Tasks
- [x] **GlobalExceptionHandler** — `@RestControllerAdvice` handles: `ResourceNotFoundException`, `UnauthorizedException`, `ValidationException`, `MethodArgumentNotValidException`
- [x] **ResourceNotFoundException** — custom exception mapped to 404
- [x] **UnauthorizedException** — custom exception mapped to 403
- [x] **Validation errors** — field-level messages returned as structured response
- [x] **PostService / CommentService** — refactored to throw typed exceptions instead of raw RuntimeException
- [x] **ApiErrorResponse DTO** — standard error envelope returned on all errors

### Verification Checklist
- [x] Missing required fields return 400 with clear field-level messages
- [x] Duplicate email returns 409 Conflict
- [x] Resource not found returns 404 with descriptive message
- [x] Unauthorized action returns 403 Forbidden
- [x] All error responses follow standard format

---

## Summary

| Phase | Status |
|-------|--------|
| Phase 1 — Database Foundation      | ✅ Complete |
| Phase 2 — Authentication           | ✅ Complete |
| Phase 3 — Post Management          | ✅ Complete |
| Phase 4 — Comment System           | ✅ Complete |
| Phase 5 — Search and Filtering     | ✅ Complete |
| Phase 6 — Error Handling           | ✅ Complete |
| Swagger Documentation              | ✅ Complete |
| Defense Documentation              | ✅ Complete |
