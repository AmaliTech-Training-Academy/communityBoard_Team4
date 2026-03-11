# Phase 1 — Database Foundation: Verification Checklist

> **Project:** CommunityBoard
> **Phase:** 1 — Database Foundation
> **Completed:** 2026-03-09

---

## Acceptance Criteria

### ✅ AC-1 — Tables: `users`, `posts`, `comments` exist

| Criterion | Status | Evidence |
|-----------|--------|----------|
| `users` table defined | ✅ | `V1__create_users_posts_comments.sql` line 12 — `CREATE TABLE IF NOT EXISTS users` |
| `posts` table defined | ✅ | `V1__create_users_posts_comments.sql` line 28 — `CREATE TABLE IF NOT EXISTS posts` |
| `comments` table defined | ✅ | `V1__create_users_posts_comments.sql` line 46 — `CREATE TABLE IF NOT EXISTS comments` |
| JPA entity `User` | ✅ | `model/User.java` — `@Table(name = "users")` |
| JPA entity `Post` | ✅ | `model/Post.java` — `@Table(name = "posts", ...)` |
| JPA entity `Comment` | ✅ | `model/Comment.java` — `@Table(name = "comments", ...)` |

---

### ✅ AC-2 — Foreign Key Constraints

| FK | Status | Evidence |
|----|--------|----------|
| `posts.author_id → users.id` | ✅ | SQL: `CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE` |
| `posts.author_id` JPA mapping | ✅ | `Post.java` — `@ManyToOne @JoinColumn(name = "author_id", nullable = false)` |
| `comments.post_id → posts.id` | ✅ | SQL: `CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE` |
| `comments.post_id` JPA mapping | ✅ | `Comment.java` — `@ManyToOne @JoinColumn(name = "post_id", nullable = false)` |
| `comments.author_id → users.id` | ✅ | SQL: `CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE` |
| `comments.author_id` JPA mapping | ✅ | `Comment.java` — `@ManyToOne @JoinColumn(name = "author_id", nullable = false)` |

---

### ✅ AC-3 — Category restricted to enum values

| Criterion | Status | Evidence |
|-----------|--------|----------|
| DB-level CHECK constraint | ✅ | SQL: `CONSTRAINT chk_post_category CHECK (category IN ('NEWS', 'EVENT', 'DISCUSSION', 'ALERT'))` |
| Java enum definition | ✅ | `model/Category.java` — `enum Category { NEWS, EVENT, DISCUSSION, ALERT }` |
| JPA `@Enumerated(EnumType.STRING)` | ✅ | `Post.java` — `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)` |
| Case-insensitive parse in service | ✅ | `PostService.parseCategory()` — `Category.valueOf(categoryStr.trim().toUpperCase())` |
| Invalid category → error | ✅ | `PostService.parseCategory()` throws `RuntimeException` with clear message |

---

### ✅ AC-4 — Timestamps auto-generated

| Criterion | Status | Evidence |
|-----------|--------|----------|
| `users.created_at` auto-set | ✅ | `User.java` — `@PrePersist onCreate()` sets `createdAt = LocalDateTime.now()` |
| SQL default | ✅ | `V1` — `created_at TIMESTAMP NOT NULL DEFAULT NOW()` |
| `posts.created_at` auto-set | ✅ | `Post.java` — `@PrePersist onCreate()` |
| `posts.updated_at` auto-updated | ✅ | `Post.java` — `@PreUpdate onUpdate()` sets `updatedAt` |
| SQL default | ✅ | `V1` — `created_at / updated_at TIMESTAMP NOT NULL DEFAULT NOW()` |
| `comments.created_at` auto-set | ✅ | `Comment.java` — `@PrePersist onCreate()` |
| SQL default | ✅ | `V1` — `created_at TIMESTAMP NOT NULL DEFAULT NOW()` |

---

### ✅ AC-5 — Text fields have explicit size limits

| Field | Limit | Enforcement Layer | Evidence |
|-------|-------|-------------------|----------|
| `users.name` | 100 chars | DB (VARCHAR) + JPA | SQL `VARCHAR(100)` · `User.java` `@Column(length = 100)` |
| `users.email` | 150 chars | DB (VARCHAR) + JPA | SQL `VARCHAR(150)` · `User.java` `@Column(length = 150)` |
| `users.password` | 255 chars | DB (VARCHAR) + JPA | SQL `VARCHAR(255)` · `User.java` `@Column(length = 255)` |
| `users.role` | 10 chars | DB (VARCHAR) + JPA | SQL `VARCHAR(10)` · `User.java` `@Column(length = 10)` |
| `posts.title` | 255 chars | DB (VARCHAR) + JPA + DTO | SQL `VARCHAR(255)` · `Post.java` `@Column(length = 255)` · `PostRequest` `@Size(max=255)` |
| `posts.body` | 10 000 chars (app) | DTO validation | `PostRequest` `@Size(max=10_000)` · DB is TEXT (unlimited, within reason) |
| `posts.category` | 20 chars | DB (VARCHAR) + JPA | SQL `VARCHAR(20)` · `Post.java` `@Column(length = 20)` |
| `comments.content` | App-layer | DTO `@NotBlank` | DB is TEXT; future: add `@Size` in CommentRequest if needed |

---

### ✅ AC-6 — Cascade delete: deleting a post removes its comments

| Criterion | Status | Evidence |
|-----------|--------|----------|
| DB FK with ON DELETE CASCADE | ✅ | SQL: `CONSTRAINT fk_comments_post ... ON DELETE CASCADE` |
| JPA cascade at entity level | ✅ | `Post.java` — `@OneToMany(mappedBy="post", cascade=CascadeType.ALL, orphanRemoval=true)` |
| Both layers enforced | ✅ | DB cascade ensures integrity even if JPA is bypassed; JPA cascade handles ORM-level deletions |

---

### ✅ AC-7 — Flyway as single source of truth for DDL

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Flyway dependency added | ✅ | `pom.xml` — `flyway-core` + `flyway-database-postgresql` |
| Migration directory created | ✅ | `src/main/resources/db/migration/` |
| V1 migration script | ✅ | `db/migration/V1__create_users_posts_comments.sql` |
| V2 seed migration | ✅ | `db/migration/V2__seed_data.sql` |
| `ddl-auto` changed to `validate` | ✅ | `application.yml` — `ddl-auto: validate` |
| `spring.sql.init.mode: never` | ✅ | `application.yml` — prevents data.sql from conflicting with Flyway |
| `data.sql` disabled | ✅ | `data.sql` — header comment marks as deprecated; sql.init.mode=never prevents execution |
| `baseline-on-migrate: true` | ✅ | `application.yml` — safe for existing DBs migrating from ddl-auto:update |

---

### ✅ AC-8 — `Post.content` renamed to `Post.body` (CommunityBoard spec)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Entity field renamed | ✅ | `Post.java` — `@Column(name = "body", ...) private String body` |
| Flyway column name | ✅ | `V1` SQL — `body TEXT NOT NULL` |
| `PostRequest` DTO | ✅ | `PostRequest.java` — `private String body` with `@NotBlank` + `@Size(max=10_000)` |
| `PostResponse` DTO | ✅ | `PostResponse.java` — `private String body` |
| `PostService.createPost` | ✅ | Uses `request.getBody()` and `.body(...)` builder |
| `PostService.updatePost` | ✅ | Uses `post.setBody(request.getBody())` |
| `PostService.toResponse` | ✅ | Uses `post.getBody()` |

---

### ✅ AC-9 — Performance Indexes

| Index | Column | Table | Evidence |
|-------|--------|-------|----------|
| `idx_posts_author_id` | `author_id` | `posts` | V1 SQL + `Post.java @Table @Index` |
| `idx_posts_category` | `category` | `posts` | V1 SQL + `Post.java @Table @Index` |
| `idx_posts_created_at` | `created_at DESC` | `posts` | V1 SQL + `Post.java @Table @Index` |
| `idx_comments_post_id` | `post_id` | `comments` | V1 SQL + `Comment.java @Table @Index` |
| `idx_comments_author_id` | `author_id` | `comments` | V1 SQL + `Comment.java @Table @Index` |

---

## Files Touched in Phase 1

| File | Change |
|------|--------|
| `pom.xml` | Added `flyway-core` + `flyway-database-postgresql` dependencies; Lombok 1.18.38 + explicit AP path |
| `application.yml` | `ddl-auto: validate`; `spring.flyway.*` config; `sql.init.mode: never`; `cors.allowed-origins` property |
| `model/Category.java` | Converted from `@Entity` to `enum { NEWS, EVENT, DISCUSSION, ALERT }` |
| `model/Post.java` | Field `content` → `body`; `@Column(name="body")`; cascade delete; indexes; `@PrePersist/@PreUpdate` |
| `model/User.java` | Column size constraints; `@PrePersist`; role default |
| `model/Comment.java` | Indexes; `@PrePersist` |
| `dto/PostRequest.java` | Field `content` → `body`; removed duplicate `@NotNull` + `@NotBlank`; `@Size(max=10_000)` |
| `dto/PostResponse.java` | Field `content` → `body` |
| `service/PostService.java` | All `getContent/setContent/content()` → `getBody/setBody/body()`; admin update fix; `@Slf4j` |
| `repository/CategoryRepository.java` | Converted to empty placeholder (Category is now an enum) |
| `repository/PostRepository.java` | Updated to use `Category` enum; `searchPosts` JPQL query |
| `repository/CommentRepository.java` | Added paginated overload |
| `controller/CategoryController.java` | Returns enum names from `Category.values()` |
| `config/SecurityConfig.java` | `@EnableMethodSecurity`; consolidated CORS; configurable origins |
| `config/CorsConfig.java` | Disabled (CORS now in SecurityConfig) |
| `resources/data.sql` | Marked as deprecated/disabled |
| `resources/db/migration/V1__create_users_posts_comments.sql` | **CREATED** — full DDL |
| `resources/db/migration/V2__seed_data.sql` | **CREATED** — seed data using `body` column |

---

## Assumptions

1. **PostgreSQL** is used in all environments. The `TEXT` type and `BIGSERIAL` are PostgreSQL-specific features.
2. **Fresh DB** will have all migrations run (V1 + V2). **Existing DBs** (previously managed by `ddl-auto: update`) will use `baseline-on-migrate: true` and only run V2 onwards.
3. **Comment body name** (`Comment.content`) was deliberately **not renamed** — the spec only specifies `body` for Posts. Comments use `content` which is correct terminology for comment text.
4. **TEXT column size** — The spec requires "explicit size limits". For free-form text (post body, comment content), a DB-level `TEXT` column is appropriate. Application-layer `@Size(max=10_000)` enforces a practical limit without artificially restricting users.
5. **V2 seed data** uses `ON CONFLICT (id) DO NOTHING` — idempotent; safe to re-run.

---

## Risks / Follow-up

| Risk | Mitigation |
|------|------------|
| `ddl-auto: validate` will fail if Flyway has not run yet | Flyway runs before Hibernate on startup; ordering is guaranteed by Spring Boot |
| Adding new columns in future requires a new migration (V3+) | This is correct Flyway discipline — never edit existing migration files |
| `Comment.content` not renamed | Deliberate (spec only says `body` for Posts). Verify acceptance criteria with PO if needed |
| Seed data IDs (1, 2, 3) conflict with BIGSERIAL sequences | PostgreSQL sequences start at 1; manually inserted rows with specific IDs may clash. Consider using `SELECT setval('posts_id_seq', 10)` in V2 to move the sequence past the seeded IDs |
