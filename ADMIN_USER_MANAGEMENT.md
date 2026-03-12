# Admin User Management — Frontend Integration Guide

This document covers everything the frontend needs to implement the admin user management feature.
Passwords are **never** exposed or modifiable through any admin endpoint — this is enforced at every layer.

---

## Base URL

```
http://localhost:8080
```

All admin endpoints are under `/api/admin/users`.

---

## Authentication

Every request in this section **must** include an `ADMIN` JWT token in the `Authorization` header:

```
Authorization: Bearer <token>
```

A `USER` token will receive `403 Forbidden`. An absent/invalid token will receive `401 Unauthorized`.

---

## Endpoints

### 1. `GET /api/admin/users` — List all users

Returns every registered user in the system.

**Request:**
```http
GET /api/admin/users
Authorization: Bearer <admin-token>
```

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

**Error responses:**

| Status | When |
|---|---|
| `401` | Missing or invalid token |
| `403` | Token is valid but role is `USER`, not `ADMIN` |

---

### 2. `GET /api/admin/users/{id}` — Get a specific user

**Request:**
```http
GET /api/admin/users/2
Authorization: Bearer <admin-token>
```

**Success — `200 OK`:**
```json
{
  "id": 2,
  "name": "Test User",
  "email": "user@amalitech.com",
  "role": "USER",
  "createdAt": "2026-03-12T10:00:00"
}
```

**Error responses:**

| Status | When |
|---|---|
| `401` | Missing or invalid token |
| `403` | Not an admin |
| `404` | No user exists with that ID |

---

### 3. `PUT /api/admin/users/{id}` — Update a user

All fields are **optional** — only send the fields you want to change. Omitted fields are left unchanged.

**Request:**
```http
PUT /api/admin/users/2
Authorization: Bearer <admin-token>
Content-Type: application/json
```

**Request body:**
```json
{
  "name": "Updated Name",
  "email": "newemail@example.com",
  "role": "ADMIN"
}
```

**Field rules:**

| Field | Required | Type | Validation |
|---|---|---|---|
| `name` | No | string | 2–100 chars. Letters only; spaces, hyphens, apostrophes allowed **between** word parts only. Each word part max 19 chars. E.g. `Jane Smith`, `O'Brien`, `Mary-Jane`. |
| `email` | No | string | Must be a valid email address. Must not already be used by another account. |
| `role` | No | string | Must be exactly `"USER"` or `"ADMIN"` (uppercase). |

> **Password is intentionally excluded.** Admins cannot read or change user passwords. Password management belongs to the user.

**Partial update examples:**

Change role only:
```json
{ "role": "ADMIN" }
```

Change name only:
```json
{ "name": "Kwame Mensah" }
```

Change email and role:
```json
{
  "email": "kwame@amalitech.com",
  "role": "USER"
}
```

**Success — `200 OK`:**
```json
{
  "id": 2,
  "name": "Updated Name",
  "email": "newemail@example.com",
  "role": "ADMIN",
  "createdAt": "2026-03-12T10:00:00"
}
```

**Error responses:**

| Status | When |
|---|---|
| `400` | Validation failure (see error message) or the new email is already registered to another account |
| `401` | Missing or invalid token |
| `403` | Not an admin |
| `404` | No user exists with that ID |

**Example `400` body:**
```json
{
  "status": 400,
  "message": "Email is already in use",
  "timestamp": "2026-03-12T10:30:00"
}
```

---

### 4. `DELETE /api/admin/users/{id}` — Delete a user

Permanently removes the user account.

**Request:**
```http
DELETE /api/admin/users/2
Authorization: Bearer <admin-token>
```

**Success — `204 No Content`** (empty body)

**Error responses:**

| Status | When |
|---|---|
| `401` | Missing or invalid token |
| `403` | Not an admin |
| `404` | No user exists with that ID |

---

## Response Shape — `UserResponse`

Every successful response (except `DELETE`) returns this shape. **`password` is never present.**

```ts
// TypeScript interface
interface UserResponse {
  id: number;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
  createdAt: string; // ISO-8601, e.g. "2026-03-12T10:00:00"
}
```

---

## Error Shape

All errors return this envelope:

```ts
interface ApiError {
  status: number;
  message: string;
  timestamp: string; // "2026-03-12T10:30:00"
}
```

---

## Frontend Integration — Code Examples

### Guard: show admin UI only to admins

```js
const user = JSON.parse(localStorage.getItem('user'));
const isAdmin = user?.role === 'ADMIN';

// In React (conditional render):
{isAdmin && <AdminUserPanel />}
```

### Axios service — admin user management

```js
// services/adminUserService.js

import axios from 'axios';

const BASE = 'http://localhost:8080/api/admin/users';

const authHeader = () => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`,
});

export const getAllUsers = () =>
  axios.get(BASE, { headers: authHeader() });

export const getUserById = (id) =>
  axios.get(`${BASE}/${id}`, { headers: authHeader() });

export const updateUser = (id, payload) =>
  axios.put(`${BASE}/${id}`, payload, { headers: authHeader() });

export const deleteUser = (id) =>
  axios.delete(`${BASE}/${id}`, { headers: authHeader() });
```

### Updating only the role (no other fields needed)

```js
await updateUser(userId, { role: 'ADMIN' });
```

### Handling errors

```js
try {
  await updateUser(id, { email: 'bad@' });
} catch (err) {
  if (err.response?.status === 400) {
    console.error(err.response.data.message); // "Email must be a valid email address"
  } else if (err.response?.status === 404) {
    console.error('User not found');
  } else if (err.response?.status === 403) {
    console.error('Access denied — admin only');
  }
}
```

### Delete with confirmation

```js
const handleDelete = async (userId) => {
  if (!window.confirm('Are you sure you want to delete this user?')) return;
  try {
    await deleteUser(userId);
    setUsers(prev => prev.filter(u => u.id !== userId));
  } catch (err) {
    alert('Failed to delete user: ' + err.response?.data?.message);
  }
};
```

---

## What Was Intentionally Left Out

| Feature | Reason |
|---|---|
| **Create user (POST)** | Handled by `POST /api/auth/register` — admins can register accounts the same way users do. No separate admin-create endpoint. |
| **Change password** | Admins cannot view or change passwords. This is a deliberate security decision. |
| **View password** | Passwords are BCrypt-hashed at rest and `@JsonIgnore` on the `User` entity — they are never serialised into any API response. |

---

## Quick Reference

| Method | Path | Body fields | Returns |
|---|---|---|---|
| `GET` | `/api/admin/users` | — | `UserResponse[]` |
| `GET` | `/api/admin/users/{id}` | — | `UserResponse` |
| `PUT` | `/api/admin/users/{id}` | `name?`, `email?`, `role?` | `UserResponse` |
| `DELETE` | `/api/admin/users/{id}` | — | *(empty)* |
