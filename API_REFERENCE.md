# CommunityBoard API Reference

## Base URL

| Environment | URL |
|---|---|
| Local development | `http://localhost:8080` |
| Docker (compose) | `http://localhost:8080` |

All endpoints are prefixed with `/api`. The full path for every request is `{Base URL}{endpoint path}`.

> **Interactive docs (Swagger UI):** `http://localhost:8080/swagger-ui.html`

---

## Authentication

The API uses **JWT Bearer tokens**. After registering or logging in you receive a `token`. Pass it in every subsequent request that requires authentication:

```
Authorization: Bearer <token>
```

Tokens do **not** expire automatically in the current configuration — store them in `localStorage` or a cookie on the frontend.

---

## Common Response: Errors

All errors follow this shape:

```json
{
  "status": 400,
  "message": "Validation failed: Name must be a valid full name ...",
  "timestamp": "2026-03-12T10:30:00"
}
```

| HTTP Status | Meaning |
|---|---|
| `400` | Validation failed — check the `message` field for details |
| `401` | Missing or invalid Bearer token |
| `403` | Authenticated but not authorised (e.g. editing someone else's post) |
| `404` | Resource not found |
| `405` | Wrong HTTP method for that path |
| `409` | Conflict — e.g. email already registered |
| `500` | Unexpected server error |

---

## 1. Authentication Endpoints

### `POST /api/auth/register` — Register a new account

**Auth required:** No

**Request body:**

```json
{
  "name": "Jane Smith",
  "email": "jane@example.com",
  "password": "mypassword123" // pragma: allowlist secret
}
```

| Field | Type | Rules |
|---|---|---|
| `name` | string | Required. 2–100 chars. Letters only; spaces, hyphens, apostrophes allowed as separators between word parts. Each word part max 19 chars. |
| `email` | string | Required. Must be a valid email (e.g. `user@domain.com`). |
| `password` | string | Required. Minimum 6 characters. |

**Valid name examples:** `Jane Smith`, `Mary-Jane`, `O'Brien`, `Jean-Luc Picard`

**Invalid name examples:** `RVcFNbqPjqiYLlyRHFPm` (word part >19 chars), `123Jane` (digits), `Jane--Smith` (consecutive separators)

**Success — `201 Created`:**

```json
{
  "id": 3,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "jane@example.com",
  "name": "Jane Smith",
  "role": "USER"
}
```

**Error responses:**

| Status | When |
|---|---|
| `400` | Missing fields or validation failure |
| `409` | Email is already registered |

---

### `POST /api/auth/login` — Login

**Auth required:** No

**Request body:**

```json
{
  "email": "jane@example.com",
  "password": "mypassword123" // pragma: allowlist secret
}
```

**Success — `200 OK`:**

```json
{
  "id": 3,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "jane@example.com",
  "name": "Jane Smith",
  "role": "USER"
}
```

> Use the `role` field (`"USER"` or `"ADMIN"`) to conditionally show/hide admin UI.

**Error responses:**

| Status | When |
|---|---|
| `400` | Missing or invalid fields |
| `404` | Email not found or wrong password |

---

## 2. Posts Endpoints

### `GET /api/posts` — List all posts (paginated)

**Auth required:** No

**Query parameters:**

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-based) |
| `size` | `10` | Number of posts per page |

**Example:** `GET /api/posts?page=0&size=10`

**Success — `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "title": "Welcome to CommunityBoard!",
      "body": "This is our official community platform...",
      "category": "NEWS",
      "authorName": "Admin User",
      "authorEmail": "admin@amalitech.com",
      "createdAt": "2026-03-12T10:00:00",
      "updatedAt": "2026-03-12T10:00:00",
      "commentCount": 2
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true
}
```

---

### `GET /api/posts/search` — Search and filter posts

**Auth required:** No

**Query parameters (all optional):**

| Param | Type | Description |
|---|---|---|
| `category` | string | One of `NEWS`, `EVENT`, `DISCUSSION`, `ALERT`. Omit for all. |
| `startDate` | ISO-8601 datetime | Lower bound, e.g. `2026-01-01T00:00:00` |
| `endDate` | ISO-8601 datetime | Upper bound, e.g. `2026-12-31T23:59:59` |
| `keyword` | string | Case-insensitive match against title and body |
| `page` | number | Default `0` |
| `size` | number | Default `10` |

**Example:** `GET /api/posts/search?category=NEWS&keyword=welcome&page=0&size=5`

**Success — `200 OK`:** Same paginated shape as `/api/posts`. Returns empty `content: []` when nothing matches (never `404`).

---

### `GET /api/posts/{id}` — Get a single post

**Auth required:** No

**Example:** `GET /api/posts/1`

**Success — `200 OK`:** Single `PostResponse` object (same fields as in the list above).

**Error responses:**

| Status | When |
|---|---|
| `404` | Post does not exist |

---

### `POST /api/posts` — Create a post

**Auth required:** Yes (`USER` or `ADMIN`)

**Request body:**

```json
{
  "title": "Team Building Event Next Friday",
  "body": "We are organising a cross-location event. Details to follow.",
  "category": "EVENT"
}
```

| Field | Type | Rules |
|---|---|---|
| `title` | string | Required. Max 255 chars. |
| `body` | string | Required. Max 10,000 chars. |
| `category` | string | Required. Must be one of: `NEWS`, `EVENT`, `DISCUSSION`, `ALERT` (case-insensitive). |

**Success — `201 Created`:** Single `PostResponse` object.

**Error responses:**

| Status | When |
|---|---|
| `400` | Missing/invalid fields or unknown category |
| `401` | No Bearer token provided |

---

### `PUT /api/posts/{id}` — Update a post

**Auth required:** Yes (original author **or** `ADMIN`)

**Request body:** Same shape as `POST /api/posts`.

**Success — `200 OK`:** Updated `PostResponse`.

**Error responses:**

| Status | When |
|---|---|
| `400` | Validation failure |
| `401` | Not authenticated |
| `403` | Authenticated but not the author or an admin |
| `404` | Post not found |

---

### `DELETE /api/posts/{id}` — Delete a post

**Auth required:** Yes (original author **or** `ADMIN`)

Deleting a post also deletes **all its comments**.

**Success — `204 No Content`**

**Error responses:**

| Status | When |
|---|---|
| `401` | Not authenticated |
| `403` | Not the author or admin |
| `404` | Post not found |

---

## 3. Comments Endpoints

### `GET /api/posts/{postId}/comments` — List comments on a post

**Auth required:** No

**Query parameters:**

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-based) |
| `size` | `20` | Comments per page |

**Example:** `GET /api/posts/1/comments?page=0&size=20`

**Success — `200 OK`:**

```json
{
  "content": [
    {
      "id": 4,
      "content": "Great post!",
      "authorName": "Jane Smith",
      "authorEmail": "jane@example.com",
      "createdAt": "2026-03-12T11:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**Error responses:**

| Status | When |
|---|---|
| `404` | Post not found |

---

### `POST /api/posts/{postId}/comments` — Add a comment

**Auth required:** Yes (`USER` or `ADMIN`)

**Request body:**

```json
{
  "content": "Great post, thank you!"
}
```

| Field | Type | Rules |
|---|---|---|
| `content` | string | Required. Max 2,000 chars. |

**Success — `201 Created`:** Single `CommentResponse` object.

**Error responses:**

| Status | When |
|---|---|
| `400` | Blank content or exceeds 2000 chars |
| `401` | No Bearer token |
| `404` | Post not found |

---

### `PUT /api/comments/{id}` — Edit a comment

**Auth required:** Yes (comment author **or** `ADMIN`)

**Request body:**

```json
{
  "content": "Updated comment text."
}
```

**Success — `200 OK`:** Updated `CommentResponse`.

**Error responses:**

| Status | When |
|---|---|
| `400` | Validation error |
| `401` | Not authenticated |
| `403` | Not the comment author or admin |
| `404` | Comment not found |

---

### `DELETE /api/comments/{id}` — Delete a comment

**Auth required:** Yes (comment author **or** `ADMIN`)

**Success — `204 No Content`**

**Error responses:**

| Status | When |
|---|---|
| `401` | Not authenticated |
| `403` | Not the comment author or admin |
| `404` | Comment not found |

---

## 4. Categories Endpoint

### `GET /api/categories` — List all valid categories

**Auth required:** No

Use this to populate category dropdowns in the UI.

**Success — `200 OK`:**

```json
["NEWS", "EVENT", "DISCUSSION", "ALERT"]
```

---

## 5. Admin — User Management Endpoints

> **All endpoints in this section require an `ADMIN` JWT token. A `USER` token will receive `403 Forbidden`.**

### `GET /api/admin/users` — List all users

**Auth required:** `ADMIN`

**Success — `200 OK`:**

```json
[
  {
    "id": 1,
    "name": "Admin User",
    "email": "admin@amalitech.com",
    "role": "ADMIN",
    "createdAt": "2026-03-12T10:00:00"
  },
  {
    "id": 2,
    "name": "Test User",
    "email": "user@amalitech.com",
    "role": "USER",
    "createdAt": "2026-03-12T10:00:00"
  }
]
```

> Passwords are **never** returned in any admin response.

---

### `GET /api/admin/users/{id}` — Get a specific user

**Auth required:** `ADMIN`

**Example:** `GET /api/admin/users/2`

**Success — `200 OK`:** Single `UserResponse` object (same fields as above).

**Error responses:**

| Status | When |
|---|---|
| `404` | User not found |

---

### `PUT /api/admin/users/{id}` — Update a user

**Auth required:** `ADMIN`

All fields are **optional** — send only the fields you want to change.

**Request body:**

```json
{
  "name": "Updated Name",
  "email": "newemail@example.com",
  "role": "ADMIN"
}
```

| Field | Type | Rules |
|---|---|---|
| `name` | string | Optional. 2–100 chars. Same name rules as registration. |
| `email` | string | Optional. Must be valid and not already taken. |
| `role` | string | Optional. Must be exactly `"USER"` or `"ADMIN"`. |

> **Note:** Passwords cannot be changed via this endpoint. Password management is handled by the user themselves through the auth flow.

**Success — `200 OK`:** Updated `UserResponse`.

**Error responses:**

| Status | When |
|---|---|
| `400` | Validation error, or new email already in use |
| `404` | User not found |

---

### `DELETE /api/admin/users/{id}` — Delete a user

**Auth required:** `ADMIN`

**Example:** `DELETE /api/admin/users/2`

**Success — `204 No Content`**

**Error responses:**

| Status | When |
|---|---|
| `404` | User not found |

---

## Quick Reference Table

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register — returns JWT |
| `POST` | `/api/auth/login` | No | Login — returns JWT |
| `GET` | `/api/posts` | No | List posts (paginated) |
| `GET` | `/api/posts/search` | No | Search/filter posts |
| `GET` | `/api/posts/{id}` | No | Get single post |
| `POST` | `/api/posts` | User/Admin | Create a post |
| `PUT` | `/api/posts/{id}` | Author/Admin | Update a post |
| `DELETE` | `/api/posts/{id}` | Author/Admin | Delete a post + comments |
| `GET` | `/api/posts/{postId}/comments` | No | List comments on a post |
| `POST` | `/api/posts/{postId}/comments` | User/Admin | Add a comment |
| `PUT` | `/api/comments/{id}` | Author/Admin | Edit a comment |
| `DELETE` | `/api/comments/{id}` | Author/Admin | Delete a comment |
| `GET` | `/api/categories` | No | List valid categories |
| `GET` | `/api/admin/users` | Admin only | List all users |
| `GET` | `/api/admin/users/{id}` | Admin only | Get user by ID |
| `PUT` | `/api/admin/users/{id}` | Admin only | Update user (name/email/role) |
| `DELETE` | `/api/admin/users/{id}` | Admin only | Delete a user |

---

## Frontend Integration Notes

### Storing the token
```js
// After login/register:
localStorage.setItem('token', response.data.token);
localStorage.setItem('user', JSON.stringify({
  id: response.data.id,
  name: response.data.name,
  email: response.data.email,
  role: response.data.role,
}));
```

### Attaching the token to requests (Axios example)
```js
const api = axios.create({ baseURL: 'http://localhost:8080' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### Checking admin status
```js
const user = JSON.parse(localStorage.getItem('user'));
const isAdmin = user?.role === 'ADMIN';
```

### Pagination
Paginated endpoints return a Spring `Page` object. Use these fields:
- `content` — array of items on current page
- `totalElements` — total number of results
- `totalPages` — total pages
- `number` — current page (0-based)
- `first` / `last` — booleans for disabling prev/next buttons

### Date format
All `createdAt` / `updatedAt` fields are ISO-8601 strings:
```
"2026-03-12T10:30:00"
```
Parse with `new Date(post.createdAt)` in JavaScript or use a library like `dayjs`.

### Categories
Always fetch categories dynamically from `GET /api/categories` rather than hard-coding them. Use the array to populate `<select>` dropdowns.
