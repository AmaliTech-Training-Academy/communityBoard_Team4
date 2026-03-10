# CommunityBoard — Frontend Integration Guide

> **For:** Frontend Developer  
> **Backend stack:** Java 17 · Spring Boot 3.2.0 · Spring Security (JWT) · Spring Data JPA · PostgreSQL  
> **Frontend stack:** React (Functional Components + Hooks)  
> **Last updated:** March 2026

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Backend Architecture Overview](#2-backend-architecture-overview)
3. [Authentication Flow](#3-authentication-flow)
4. [API Base URL](#4-api-base-url)
5. [Authentication Endpoints](#5-authentication-endpoints)
6. [Post Management Endpoints](#6-post-management-endpoints)
7. [Comment Endpoints](#7-comment-endpoints)
8. [Search and Filtering](#8-search-and-filtering)
9. [Role-Based Access Rules](#9-role-based-access-rules)
10. [Error Handling](#10-error-handling)
11. [Frontend Workflow Examples](#11-frontend-workflow-examples)
12. [React Code Examples](#12-react-code-examples)
13. [Development Tips](#13-development-tips)
14. [Swagger Documentation Reference](#14-swagger-documentation-reference)

---

## 1. Project Overview

**CommunityBoard** is a community engagement platform that allows residents and members of a community to share news, announce events, start discussions, and raise alerts — all in one place.

### Core Features

| Feature | Description |
|---|---|
| **User Authentication** | Register and log in with email/password. JWT token protects all write operations. |
| **Post Management** | Create, read, update, and delete posts with a category (NEWS, EVENT, DISCUSSION, ALERT). |
| **Comments** | Authenticated users can comment on any post. Comments are paginated. |
| **Search & Filtering** | Filter posts by category, keyword, and date range. |
| **Role-Based Access** | Regular users manage their own content. Admins can manage everyone's content. |

---

## 2. Backend Architecture Overview

The backend follows a standard **layered architecture**:

```
Frontend (React)
      ↓  HTTP Request
  Controller          ← Handles HTTP, validates input, returns HTTP response
      ↓
   Service            ← Business logic (auth checks, data transformation)
      ↓
  Repository          ← Database queries via Spring Data JPA
      ↓
  PostgreSQL          ← Stores users, posts, and comments
```

### What each layer does

- **Controller** — Receives HTTP requests from the frontend and returns JSON responses. Validates incoming data (returns `400` if fields are missing or invalid).
- **Service** — Implements all business rules, e.g. "only the author or an admin can delete a post."
- **Repository** — Uses Spring Data JPA to query PostgreSQL — think of it as the database access layer.
- **Database** — PostgreSQL stores all persistent data in three main tables: `users`, `posts`, `comments`.

---

## 3. Authentication Flow

CommunityBoard uses **JWT (JSON Web Token)** for authentication. There is no session or cookie — the token is sent with every request.

### Step-by-step flow

```
1. User fills in the Register form
         ↓
2. Frontend sends POST /api/auth/register
         ↓
3. Backend creates the user and returns a JWT token
         ↓
4. Frontend stores the token (localStorage or React Context)
         ↓
5. For every protected request, frontend adds the token in the header:
   Authorization: Bearer <token>
         ↓
6. Backend validates the token and processes the request
```

### Request Header Format

Every **protected** API call must include this header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0...
```

> **Important:** Only the raw token goes in the header — no quotes, just `Bearer <token>`.

### Where to store the token

| Option | Recommended use |
|---|---|
| `localStorage` | Simple projects. Persists across browser sessions. |
| `React Context + memory` | More secure. Token is lost on page refresh (user must log in again). |
| `React Context + localStorage` | **Recommended** — combine both for persistence and global access. |

---

## 4. API Base URL

```
http://localhost:8080/api
```

All endpoints below are relative to this base URL. In production, replace `localhost:8080` with the deployed server address.

---

## 5. Authentication Endpoints

These endpoints are **public** — no token required.

### Register a New User

```
POST /api/auth/register
```

**Request body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Validation rules:**
- `name` — required, max 100 characters
- `email` — required, must be a valid email format
- `password` — required, minimum 6 characters

**Success response — `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "USER"
}
```

**Save the `token` immediately after registration** — the user is already logged in.

---

### Login

```
POST /api/auth/login
```

**Request body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Success response — `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "USER"
}
```

> The `role` field will be either `"USER"` or `"ADMIN"`. Store it in context so the UI can show/hide admin controls.

---

### Pre-seeded Test Accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@amalitech.com` | `Admin@123` |
| User | `user@amalitech.com` | `password123` |

---

## 6. Post Management Endpoints

### Get All Posts (paginated)

```
GET /api/posts?page=0&size=10
```

**Public — no token required.**

**Query parameters:**

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-based) |
| `size` | `10` | Number of posts per page |

**Response — `200 OK`:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Community Cleanup Event",
      "body": "Join us this Saturday at 9am at the park.",
      "category": "EVENT",
      "authorName": "John Doe",
      "authorEmail": "john@example.com",
      "createdAt": "2026-03-10T12:30:00",
      "updatedAt": "2026-03-10T12:30:00",
      "commentCount": 4
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false
}
```

---

### Get a Single Post

```
GET /api/posts/{id}
```

**Public — no token required.**

**Response — `200 OK`:**
```json
{
  "id": 1,
  "title": "Community Cleanup Event",
  "body": "Join us this Saturday at 9am at the park.",
  "category": "EVENT",
  "authorName": "John Doe",
  "authorEmail": "john@example.com",
  "createdAt": "2026-03-10T12:30:00",
  "updatedAt": "2026-03-10T12:30:00",
  "commentCount": 4
}
```

---

### Create a Post

```
POST /api/posts
```

**Requires authentication (Bearer token).**

**Request body:**
```json
{
  "title": "Community Cleanup Event",
  "body": "Join us this Saturday at 9am at the park.",
  "category": "EVENT"
}
```

**Validation rules:**
- `title` — required, max 200 characters
- `body` — required
- `category` — required, must be one of: `NEWS`, `EVENT`, `DISCUSSION`, `ALERT`

**Response — `201 Created`:** Returns the created post object (same structure as Get Single Post).

---

### Update a Post

```
PUT /api/posts/{id}
```

**Requires authentication. Only the post author or an ADMIN can update.**

**Request body:** Same as Create a Post.

**Response — `200 OK`:** Returns the updated post object.

---

### Delete a Post

```
DELETE /api/posts/{id}
```

**Requires authentication. Only the post author or an ADMIN can delete.**

**Response — `204 No Content`** (empty body on success).

---

## 7. Comment Endpoints

### Get Comments for a Post (paginated)

```
GET /api/posts/{postId}/comments?page=0&size=10
```

**Public — no token required.** Comments are returned oldest first.

**Response — `200 OK`:**
```json
{
  "content": [
    {
      "id": 1,
      "content": "Great initiative! I will definitely be there.",
      "authorName": "Jane Smith",
      "createdAt": "2026-03-10T14:00:00"
    },
    {
      "id": 2,
      "content": "Looking forward to it!",
      "authorName": "John Doe",
      "createdAt": "2026-03-10T15:30:00"
    }
  ],
  "totalElements": 12,
  "totalPages": 2,
  "number": 0,
  "size": 10
}
```

---

### Add a Comment

```
POST /api/posts/{postId}/comments
```

**Requires authentication.**

**Request body:**
```json
{
  "content": "Great initiative! I will definitely be there."
}
```

**Validation rules:**
- `content` — required, max 2000 characters

**Response — `201 Created`:** Returns the created comment object.

---

### Delete a Comment

```
DELETE /api/posts/{postId}/comments/{commentId}
```

**Requires authentication. Only the comment author or an ADMIN can delete.**

**Response — `204 No Content`** (empty body on success).

---

## 8. Search and Filtering

Use the **dedicated search endpoint** — not the regular `/api/posts`:

```
GET /api/posts/search
```

**Public — no token required.** All parameters are optional and can be combined freely.

### Query Parameters

| Param | Type | Example | Description |
|---|---|---|---|
| `category` | string | `NEWS` | Filter by category. Must be: `NEWS`, `EVENT`, `DISCUSSION`, or `ALERT`. Omit to include all. |
| `keyword` | string | `cleanup` | Case-insensitive search against post title and body. |
| `startDate` | ISO-8601 datetime | `2026-01-01T00:00:00` | Only return posts created on or after this date. |
| `endDate` | ISO-8601 datetime | `2026-03-31T23:59:59` | Only return posts created on or before this date. |
| `page` | integer | `0` | Page number (0-based). Default: `0`. |
| `size` | integer | `10` | Results per page. Default: `10`. |

### Example Calls

```
# Category filter only
GET /api/posts/search?category=EVENT&page=0&size=10

# Keyword search
GET /api/posts/search?keyword=cleanup

# Combined: category + keyword + pagination
GET /api/posts/search?category=NEWS&keyword=meeting&page=0&size=10

# Date range filter
GET /api/posts/search?startDate=2026-01-01T00:00:00&endDate=2026-03-31T23:59:59

# Everything together
GET /api/posts/search?category=EVENT&keyword=park&startDate=2026-01-01T00:00:00&endDate=2026-12-31T23:59:59&page=0&size=10
```

**Response:** Same paginated format as `GET /api/posts`.

---

## 9. Role-Based Access Rules

### Permissions Table

| Action | USER | ADMIN |
|---|---|---|
| View posts and comments | ✅ | ✅ |
| Register / Login | ✅ | ✅ |
| Create a post | ✅ | ✅ |
| Edit **own** post | ✅ | ✅ |
| Delete **own** post | ✅ | ✅ |
| Edit **any** post | ❌ | ✅ |
| Delete **any** post | ❌ | ✅ |
| Add a comment | ✅ | ✅ |
| Delete **own** comment | ✅ | ✅ |
| Delete **any** comment | ❌ | ✅ |

### How the frontend should handle this

1. After login, store the `role` field from the response (`"USER"` or `"ADMIN"`).
2. Show **Edit / Delete** buttons on a post only if:
   - `loggedInUser.email === post.authorEmail` (own post), OR
   - `loggedInUser.role === "ADMIN"`
3. If the backend returns `403 Forbidden`, show a message like: *"You don't have permission to do this."*
4. If the backend returns `401 Unauthorized`, redirect the user to the Login page.

---

## 10. Error Handling

All errors follow the same JSON structure:

```json
{
  "status": 404,
  "message": "Post not found",
  "timestamp": "2026-03-10T12:30:00"
}
```

### Common Error Codes

| HTTP Status | Meaning | What the frontend should do |
|---|---|---|
| `400 Bad Request` | Validation failed (missing or invalid fields) | Show `message` field to the user near the form |
| `401 Unauthorized` | No token or invalid/expired token | Redirect to Login page |
| `403 Forbidden` | Authenticated but not allowed (wrong user/role) | Show "Permission denied" message |
| `404 Not Found` | Post, comment, or user does not exist | Show "Not found" page or message |
| `409 Conflict` | Email already registered | Show "Email already in use" on register form |
| `500 Internal Server Error` | Unexpected backend error | Show generic "Something went wrong" message |

### Validation error example (400)

When multiple fields fail validation, the message joins them:

```json
{
  "status": 400,
  "message": "Title is required; Category must be one of NEWS, EVENT, DISCUSSION, ALERT",
  "timestamp": "2026-03-10T12:30:00"
}
```

---

## 11. Frontend Workflow Examples

### Login Page → `/api/auth/login`
1. User enters email + password
2. `POST /api/auth/login` with credentials
3. On success: save `token` and `role` in context/localStorage, redirect to Home
4. On `404`: show "Invalid email or password"

### Register Page → `/api/auth/register`
1. User fills in name, email, password
2. `POST /api/auth/register`
3. On `201`: save token (user is auto-logged in), redirect to Home
4. On `409`: show "This email is already registered"
5. On `400`: show the validation message on the relevant field

### Home Feed → `GET /api/posts`
1. On page load, call `GET /api/posts?page=0&size=10`
2. Display the `content` array as post cards
3. Use `totalPages` to render pagination controls
4. No token required

### Search & Filter → `GET /api/posts/search`
1. When user changes the category dropdown, keyword input, or date picker
2. Call `GET /api/posts/search?category=...&keyword=...&page=0&size=10`
3. Update the post list with the results

### Post Details Page → `GET /api/posts/{id}`
1. Call `GET /api/posts/{id}` to load the post
2. Call `GET /api/posts/{id}/comments?page=0&size=10` to load comments
3. Show Edit/Delete buttons only if the current user is the author or ADMIN

### Create Post Page → `POST /api/posts`
1. User fills in title, body, category
2. Call `POST /api/posts` with `Authorization: Bearer <token>` header
3. On `201`: redirect to the new post's detail page
4. On `401`: redirect to Login
5. On `400`: show validation errors

### Comments Section → `POST /api/posts/{postId}/comments`
1. User types a comment and clicks Submit
2. Call `POST /api/posts/{postId}/comments` with token header
3. On `201`: refresh the comments list (or optimistically append the new comment)

---

## 12. React Code Examples

### Setting up an Axios instance with the JWT token

```javascript
// src/services/api.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Attach the token automatically to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 globally — redirect to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

### Auth Context

```javascript
// src/context/AuthContext.js
import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token');
    const role  = localStorage.getItem('role');
    const name  = localStorage.getItem('name');
    return token ? { token, role, name } : null;
  });

  const login = (data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('role',  data.role);
    localStorage.setItem('name',  data.name);
    setUser(data);
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
```

---

### Login Page

```javascript
// src/pages/Login.js
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm]     = useState({ email: '', password: '' });
  const [error, setError]   = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', form);
      login(data);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input type="email"    placeholder="Email"    onChange={e => setForm({...form, email: e.target.value})} />
      <input type="password" placeholder="Password" onChange={e => setForm({...form, password: e.target.value})} />
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <button type="submit" disabled={loading}>
        {loading ? 'Logging in...' : 'Login'}
      </button>
    </form>
  );
}
```

---

### Fetching Posts (Home Feed)

```javascript
// src/pages/Home.js
import { useState, useEffect } from 'react';
import api from '../services/api';

export default function Home() {
  const [posts, setPosts]     = useState([]);
  const [page, setPage]       = useState(0);
  const [totalPages, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const { data } = await api.get(`/posts?page=${page}&size=10`);
        setPosts(data.content);
        setTotal(data.totalPages);
      } catch (err) {
        console.error('Failed to load posts', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPosts();
  }, [page]);

  return (
    <div>
      {loading && <p>Loading...</p>}
      {posts.map(post => (
        <div key={post.id}>
          <h3>{post.title}</h3>
          <span>{post.category}</span>
          <p>{post.body}</p>
          <small>By {post.authorName} · {post.commentCount} comments</small>
        </div>
      ))}
      {/* Pagination */}
      {Array.from({ length: totalPages }, (_, i) => (
        <button key={i} onClick={() => setPage(i)} disabled={page === i}>
          {i + 1}
        </button>
      ))}
    </div>
  );
}
```

---

### Creating a Post

```javascript
// src/pages/CreatePost.js
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const CATEGORIES = ['NEWS', 'EVENT', 'DISCUSSION', 'ALERT'];

export default function CreatePost() {
  const navigate = useNavigate();
  const [form, setForm]   = useState({ title: '', body: '', category: 'NEWS' });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await api.post('/posts', form);
      navigate(`/posts/${data.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create post');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input    value={form.title}    onChange={e => setForm({...form, title: e.target.value})}    placeholder="Title" />
      <textarea value={form.body}     onChange={e => setForm({...form, body: e.target.value})}     placeholder="Body" />
      <select   value={form.category} onChange={e => setForm({...form, category: e.target.value})}>
        {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
      </select>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <button type="submit">Create Post</button>
    </form>
  );
}
```

---

### Loading Comments

```javascript
// Inside a PostDetail component
const [comments, setComments]     = useState([]);
const [commentPage, setCommentPage] = useState(0);
const [totalCommentPages, setTotal] = useState(0);

useEffect(() => {
  const fetchComments = async () => {
    const { data } = await api.get(`/posts/${postId}/comments?page=${commentPage}&size=10`);
    setComments(data.content);
    setTotal(data.totalPages);
  };
  fetchComments();
}, [postId, commentPage]);
```

---

## 13. Development Tips

### 1. Use the Axios interceptor for token injection
Never manually add `Authorization` headers to every call. Set it once in the Axios instance (see Section 12). Any request that needs the token will get it automatically.

### 2. Store role in context
After login, store `role` alongside the token. Use it to conditionally render Edit/Delete buttons:
```javascript
const { user } = useAuth();
const canEdit = user && (user.email === post.authorEmail || user.role === 'ADMIN');
```

### 3. Always handle loading and error states
Every API call should have three states: loading, success, error. Use `useState` for each:
```javascript
const [loading, setLoading] = useState(false);
const [error, setError]     = useState(null);
const [data, setData]       = useState(null);
```

### 4. Redirect on 401
If you receive a `401` response anywhere in the app, the token has expired or is invalid. Clear localStorage and redirect the user to `/login`. The Axios interceptor handles this globally (see Section 12).

### 5. Search debouncing
When implementing the keyword search, add a debounce (e.g. 300ms delay) to avoid firing an API call on every keystroke:
```javascript
useEffect(() => {
  const timer = setTimeout(() => {
    fetchSearchResults();
  }, 300);
  return () => clearTimeout(timer);
}, [keyword, category]);
```

### 6. Date format for the API
When sending `startDate` and `endDate` to the search endpoint, use ISO-8601 format:
```javascript
const startDate = new Date(pickerValue).toISOString().slice(0, 19); // "2026-01-01T00:00:00"
```

### 7. Category values are fixed
The four valid category values are: `NEWS`, `EVENT`, `DISCUSSION`, `ALERT` — always uppercase. Hardcode these in the frontend as a constant array.

### 8. Check the Figma design
Make sure your component structure and styles match the provided Figma starter design in the `design/` folder.

---

## 14. Swagger Documentation Reference

The backend exposes a full interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

### What you can do there
- See every endpoint with its URL, method, and description
- See exact request body formats and required fields
- Try API calls directly in the browser (no Postman needed)
- Authenticate: click the **Authorize** button → paste your JWT token → all subsequent requests will include it

### JSON API docs (machine-readable)
```
http://localhost:8080/api-docs
```

> **Tip:** Before starting integration of a new endpoint, open Swagger, test the endpoint manually, and confirm the exact request/response shape before writing frontend code.

---

## Quick Reference Card

```
BASE URL:  http://localhost:8080/api

AUTH:
  POST  /auth/register           Public  → 201 + token
  POST  /auth/login              Public  → 200 + token

POSTS:
  GET   /posts                   Public  → paginated list
  GET   /posts/{id}              Public  → single post
  GET   /posts/search            Public  → filtered paginated list
  POST  /posts                   🔒 Auth → create post (201)
  PUT   /posts/{id}              🔒 Auth (author/admin) → update post
  DELETE /posts/{id}             🔒 Auth (author/admin) → 204

COMMENTS:
  GET   /posts/{postId}/comments            Public  → paginated comments
  POST  /posts/{postId}/comments            🔒 Auth → create comment (201)
  DELETE /posts/{postId}/comments/{id}      🔒 Auth (author/admin) → 204

HEADER FOR PROTECTED CALLS:
  Authorization: Bearer <your_jwt_token>

ERROR FORMAT:
  { "status": 4xx, "message": "...", "timestamp": "..." }
```
