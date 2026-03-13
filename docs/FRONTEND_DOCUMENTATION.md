# CommunityBoard — Frontend Documentation

**Engineer:** Illona Addae (illona.addae@amalitech.com)
**Role:** Frontend Developer — React
**Location:** Accra
**Project:** CommunityBoard — Team 4 (Phase 1 Group Project)
**Date:** March 2026

---

## 1. Overview

This document covers all frontend implementation decisions, component architecture, API integration strategy, state management, and testing for the CommunityBoard application. The frontend is built with **React 19** using functional components and modern hooks, styled following the provided Figma design, and integrated against the team's Spring Boot backend.

---

## 2. Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| React | 19.x | UI framework |
| TypeScript | 5.x | Type safety |
| Vite | 7.x | Build tool and dev server |
| React Router DOM | 6.x | Client-side routing |
| Axios | 1.x | HTTP client for API calls |
| Vitest | 4.x | Unit and component testing |
| React Testing Library | 16.x | Component testing utilities |
| ESLint | 8.x | Code quality |
| Docker / Nginx | — | Production containerisation |

---

## 3. Project Structure

```
frontend/src/
├── components/
│   ├── features/        # Feature-specific components (PostCard, etc.)
│   ├── layout/          # Shared layout components (Navbar)
│   └── ui/              # Reusable UI primitives (Badge, Toast)
├── context/
│   ├── AuthContext.tsx  # JWT auth state + login/logout/register
│   └── ToastContext.tsx # Global toast notification state
├── pages/
│   ├── Analytics/       # Analytics dashboard page
│   ├── CreatePost/      # Create & edit post page
│   ├── Login/           # Login page
│   ├── PostDetails/     # Single post view with comments
│   ├── PostFeed/        # Home feed with search & filter
│   └── Register/        # Registration page
├── services/
│   └── api.ts           # Axios instance with JWT interceptor
├── App.tsx              # Route definitions
└── index.tsx            # App entry point
```

---

## 4. Routing

All routes are defined in `App.tsx` using React Router v6:

| Path | Component | Auth Required |
|---|---|---|
| `/login` | `Login` | No — redirects to `/` if already logged in |
| `/register` | `Register` | No — redirects to `/` if already logged in |
| `/` | `PostFeed` | No |
| `/posts/:id` | `PostDetails` | No (read); Yes (comment/edit/delete) |
| `/create-post` | `CreatePost` | Yes |
| `/edit-post/:id` | `CreatePost` | Yes (author or ADMIN only) |
| `/analytics` | `Analytics` | No |

---

## 5. API Integration

### Base Configuration — `src/services/api.ts`

All HTTP requests go through a single Axios instance configured with:
- **Base URL:** `/api` (proxied to `http://backend:8080` via Nginx in Docker)
- **JWT interceptor:** Automatically attaches `Authorization: Bearer <token>` on every request if a token exists in `localStorage`
- **Response interceptor:** Handles `401` by clearing auth state and redirecting to `/login`

```ts
// All components import this single instance
import api from '../../services/api';

// Example usage
const { data } = await api.get('/posts?page=0&size=10');
const { data } = await api.post('/posts', { title, body, category });
```

### Endpoints Integrated

**Authentication**
- `POST /api/auth/register` — Register new account
- `POST /api/auth/login` — Login, stores JWT in localStorage

**Posts**
- `GET /api/posts` — Paginated post feed
- `GET /api/posts/search` — Search/filter by category, keyword, date range
- `GET /api/posts/:id` — Single post detail
- `POST /api/posts` — Create post (authenticated)
- `PUT /api/posts/:id` — Edit post (author or ADMIN)
- `DELETE /api/posts/:id` — Delete post (author or ADMIN)

**Comments**
- `GET /api/posts/:id/comments?sort=createdAt,desc` — Load comments newest-first
- `POST /api/posts/:id/comments` — Add comment
- `PUT /api/comments/:id` — Edit comment
- `DELETE /api/comments/:id` — Delete comment

**Analytics (Percy's ETL endpoints)**
- `GET /api/analytics/summary` — Total posts and total comments
- `GET /api/analytics/posts-by-category` — Post counts per category
- `GET /api/analytics/posts-by-day` — Post counts by day of week (sorted by `dayOrder`)
- `GET /api/analytics/contributors/top` — Top contributors with rank and post count

All 4 analytics endpoints are called in parallel using `Promise.all` for optimal performance.

---

## 6. State Management

### AuthContext (`src/context/AuthContext.tsx`)

Manages authentication state across the entire app using React Context + `useReducer`. Persists to `localStorage` so state survives page refreshes.

**Provides:**
- `user` — `{ id, name, email, role }` or `null` when logged out
- `login(email, password)` — calls `/api/auth/login`, stores token + user
- `register(name, email, password)` — calls `/api/auth/register`, stores token + user
- `logout()` — clears localStorage and resets state

**Usage:**
```tsx
const { user, login, logout } = useAuth();
const isAdmin = user?.role === 'ADMIN';
```

### ToastContext (`src/context/ToastContext.tsx`)

Global non-blocking notification system. All success/error feedback (post created, comment deleted, etc.) goes through here instead of `alert()`.

```tsx
const { showToast } = useToast();
showToast('Post created successfully');
showToast('Failed to delete post', 'error');
```

---

## 7. Components

### Pages

**Login (`src/pages/Login/`)**
- Email + password form with client-side validation
- On success, redirects to `/`
- Shows inline error messages for invalid credentials

**Register (`src/pages/Register/`)**
- Name, email, password form
- On success, user is immediately logged in and redirected to `/`
- Handles `409 Conflict` for duplicate emails

**PostFeed (`src/pages/PostFeed/`)**
- Paginated list of posts using `GET /api/posts` and `GET /api/posts/search`
- Category filter using `Badge` component tabs (All, NEWS, EVENT, DISCUSSION, ALERT)
- Keyword search with debounced input
- Date range filter wired to `startDate`/`endDate` query params
- Create Post button visible only to authenticated users

**PostDetails (`src/pages/PostDetails/`)**
- Full post view with author, category badge, timestamps
- Paginated comments list (newest first via `sort=createdAt,desc`)
- Add/Edit/Delete comment — edit/delete shown only to comment author or ADMIN
- Edit/Delete post — shown only to post author or ADMIN
- Confirmation dialog before destructive actions

**CreatePost (`src/pages/CreatePost/`)**
- Reused for both create (`POST /api/posts`) and edit (`PUT /api/posts/:id`) flows
- Category selector, title, and body fields
- Navigates back to post on success

**Analytics (`src/pages/Analytics/`)**
- Stat cards: Total Posts, Total Comments
- Bar chart: Posts by Category
- Bar chart: Posts by Day of Week (Sunday–Saturday order via `dayOrder`)
- Top 10 Contributors table with dense rank
- All data fetched in parallel from Percy's 4 ETL endpoints
- Loading and error states handled gracefully

### Shared Components

**Navbar (`src/components/layout/Navbar.tsx`)**
- App logo/name, navigation links
- Login/Register links when logged out; user name + Logout when logged in

**Badge (`src/components/ui/Badge.tsx`)**
- Displays category with colour coding (NEWS, EVENT, DISCUSSION, ALERT)
- Doubles as a filter tab when `isFilter={true}`

---

## 8. Design

The UI was implemented following the **Figma design** provided by the team. A starter HTML design (`design/starter-design.html`) was also available as a baseline reference.

Key design decisions:
- Mobile-first responsive layout using CSS flexbox/grid
- Consistent colour coding for post categories across all views
- Breadcrumb navigation on PostDetails and Analytics pages
- Empty states for posts feed, comments, and contributors

---

## 9. Testing

Tests are located alongside their components (`*.test.tsx`).

**Run all tests:**
```bash
cd frontend
npx vitest run
```

**Run with coverage:**
```bash
npx vitest run --coverage
```

### Test Coverage

| File | Tests | Coverage |
|---|---|---|
| `AuthContext.test.tsx` | 6 | Login, register, logout, localStorage persistence |
| `Login.test.tsx` | 4 | Form render, validation, submit |
| `Register.test.tsx` | 6 | Form render, validation, submit |
| `Analytics.test.tsx` | 11 | All 4 endpoints, loading state, error state, empty state, rankings |
| `api.test.ts` | 5 | Axios instance, interceptor setup |

**Total: 32 tests — all passing**

---

## 10. Docker & Deployment

### Dockerfile (`frontend/Dockerfile`)
- **Build stage:** Node 20 Alpine — runs `npm ci && tsc && vite build`
- **Serve stage:** Nginx Alpine — serves the static build output

### Nginx Configuration (`frontend/nginx.conf`)
- Serves static files from `/usr/share/nginx/html`
- Proxies `/api/*` requests to the backend container (`http://backend:8080`)
- `try_files` fallback ensures React Router client-side routes work correctly

### Running Locally
```bash
# Full stack (all services)
docker-compose up --build

# Frontend only (dev server with hot reload)
cd frontend && npm run dev
# Opens at http://localhost:3000
```

---

## 11. MVP Deliverables — Completion Status

| Deliverable | Status | Notes |
|---|---|---|
| Login / Register UI | ✅ Complete | JWT auth, form validation, error handling |
| Post Feed (Post List) | ✅ Complete | Paginated, real API data |
| Post Detail | ✅ Complete | Full post + paginated comments |
| Create / Edit Post | ✅ Complete | Shared component for both flows |
| Delete Post / Comment | ✅ Complete | With confirmation dialog |
| Analytics Dashboard | ✅ Complete | Integrated with Percy's 4 ETL endpoints |
| API Integration | ✅ Complete | All backend endpoints connected |
| Auth State Management | ✅ Complete | AuthContext + JWT + localStorage |
| Search & Filter UI | ✅ Complete | Category, keyword, date range |
| Responsive Design | ✅ Complete | Mobile-first, Figma-based |

---

## 12. Known Issues & Blockers

| Issue | Owner | Status |
|---|---|---|
| Analytics staging data stale (shows old counts) | Percy (Data Engineering) | ETL needs to run against staging DB to refresh materialized views |
| `fix/CB-101-comments-sort-order` PR pending merge to main | Illona | Open PR — awaiting review |

---

## 13. Environment Variables

```env
# frontend/.env (not committed — see .env.example)
VITE_API_BASE_URL=http://localhost:8080
```

In Docker, the Nginx proxy handles routing `/api` to the backend, so no CORS issues in production.

---

## 14. Git Workflow

All frontend changes follow the team's branching strategy:
- Branch from `develop`: `feature/CB-XXX-description` or `fix/CB-XXX-description`
- PR title format: `type(scope): Description [CB-XXX]`
- All PRs go through the CI pipeline (lint, tests, build, coverage) before merge
- Never push directly to `main` or `develop`

---

*Document written by Illona Addae — Frontend Engineer, CommunityBoard Team 4*
