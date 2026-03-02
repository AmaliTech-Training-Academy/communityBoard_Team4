# CommunityBoard
**AmaliTech Group Project – Full-Stack Teams (Teams 1-5)**

A community notice board where users can post announcements, events, and discussions. Supports categories, comments, and search.

## Tech Stack
- **Backend:** Java 17 + Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, PostgreSQL
- **Frontend:** React 18, React Router, Axios, Chart.js
- **Data Engineering:** Python ETL pipeline, analytics aggregation
- **QA:** REST Assured (API), Selenium WebDriver (UI)
- **DevOps:** Docker, docker-compose, GitHub Actions CI

## Getting Started
```bash
docker-compose up --build
```
- Backend API: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:3000

## Default Users (seeded)
| Email | Password | Role |
|---|---|---|
| admin@amalitech.com | password123 | ADMIN |
| user@amalitech.com | password123 | USER |

## Project Structure
```
backend/          - Spring Boot REST API
frontend/         - React 18 SPA
data-engineering/ - Python ETL & analytics
qa/               - API & UI test suites
devops/           - Docker, CI/CD configs
```

## What's Implemented (~30%)
- [x] User authentication (register/login with JWT)
- [x] Basic post CRUD (create, read, update, delete)
- [x] Category management
- [ ] Comments system (TODO)
- [ ] Search & filtering (TODO)
- [ ] User profiles (TODO)
- [ ] Notifications (TODO)
- [ ] Analytics dashboard (TODO)
