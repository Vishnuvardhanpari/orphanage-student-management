# Testing Strategy

# Overview

The Orphanage Management System (OMS) shall follow a comprehensive testing strategy to ensure reliability, security, maintainability, and production readiness.

Testing is not a final phase—it is performed throughout the Software Development Life Cycle (SDLC).

The project shall adopt the Testing Pyramid approach.

                E2E Tests
                    ▲
             Integration Tests
                    ▲
              Unit Tests

Unit tests should form the majority of all tests.

---

# Testing Levels

## 1. Unit Testing

Purpose

Validate individual methods and classes in isolation.

Backend

- JUnit 5
- Mockito

Frontend

- Jasmine
- Karma

Coverage

- Service Layer
- Utility Classes
- Validators
- Mappers
- Custom Components

Target Coverage

Minimum 80%

Preferred

90%+

---

# 2. Integration Testing

Purpose

Validate interaction between components.

Backend

Spring Boot Test

MockMvc

Testcontainers

Test

- Controller
- Service
- Repository
- Database Integration

---

# 3. API Testing

Every REST API must be tested.

Use

Postman

or

REST Assured

Test

- Success
- Validation Failure
- Unauthorized
- Forbidden
- Not Found
- Internal Error

---

# 4. UI Testing

Framework

Cypress

Test

- Login
- Student Registration
- Student Update
- File Upload
- Search
- Filters
- Report Generation

---

# Functional Testing

Verify

✔ Student Registration

✔ Student Update

✔ Student Search

✔ Student Soft Delete

✔ Student Restore

✔ Report Export

✔ Document Upload

✔ Authentication

✔ Authorization

---

# File Upload Testing

Test

Allowed

- PDF

- JPG

- JPEG

- PNG

Rejected

- EXE

- ZIP

- BAT

Large File

>10MB

Duplicate File

Replace Existing File

Multiple Upload

Preview

Download

---

# Authentication Testing

## Backend (API)

Username Login — success returns access token, refresh token, and user profile

Username Login — invalid password returns `401` with generic message

Username Login — disabled / locked account returns `403`

Google Login — valid ID token for a pre-provisioned user

Google Login — unknown email returns `401` (no self-registration)

JWT Expiration — expired access token rejected on secured endpoints

Invalid JWT — malformed or tampered token rejected

Expired JWT — secured endpoint returns `401`

Refresh Token — rotation returns a new pair; old refresh token is rejected

Refresh Token — reuse of a revoked token revokes all sessions for that user

Logout — refresh token revoked; subsequent refresh fails

`GET /auth/me` — requires Bearer token; returns current user

---

## Frontend (unit / component)

AuthService — login persists access token, refresh token, and user in `sessionStorage`

AuthService — `isAuthenticated()` returns `true` immediately after successful login (must re-read storage + user state on every call; do **not** rely on a `computed()` that mixes signals with `sessionStorage`)

AuthService — `isAuthenticated()` returns `false` when token or user is missing

AuthService — logout clears session storage and user state

LoginPage — on successful login, calls `router.navigateByUrl('/dashboard')` (or equivalent dashboard path)

LoginPage — on failed login, does **not** navigate away from `/auth/login`

authGuard — allows activation when `isAuthenticated()` is `true`

authGuard — redirects to `/auth/login` when `isAuthenticated()` is `false`

guestGuard — redirects authenticated users away from `/auth/login` to dashboard

roleGuard — Admin can access Users; Staff is redirected away

Auth interceptor — attaches `Authorization: Bearer` on non-auth API calls

Auth interceptor — on `401`, attempts single-flight refresh then retries once

Auth interceptor — on refresh failure, clears session and navigates to login

---

## Frontend (integration / E2E — mandatory for auth)

These scenarios must be covered after any auth change (regression for post-login navigation):

1. **Login → dashboard navigation**  
   Enter valid credentials → success toast (optional) → browser URL becomes `/dashboard` (not stuck on `/auth/login`) → main layout (sidebar/topbar) is visible.

2. **Session present after login**  
   After successful login, `sessionStorage` contains access token, refresh token, and current user **and** the app treats the user as authenticated for the next navigation.

3. **Reload while authenticated**  
   While logged in, reload `/dashboard` → user remains on dashboard (session restored).  
   While logged in, open `/auth/login` → guestGuard redirects to dashboard.

4. **Unauthenticated deep link**  
   Clear session → open `/dashboard` → redirected to `/auth/login`.

5. **Logout**  
   From main layout, logout → session cleared → URL is `/auth/login` → protected routes no longer accessible.

6. **Google login** (when `googleClientId` is configured)  
   Successful Google sign-in navigates to dashboard the same way as password login.

---

## Auth state testing rule (Angular)

When writing auth tests, always assert **both**:

* Persistence (`sessionStorage` / token helper), **and**
* Observable behavior (`isAuthenticated()`, guards, and actual router URL after login)

Do not treat “tokens written to storage” alone as proof of successful login UX. A green persistence test can still miss a broken post-login navigation if auth state used by guards is stale or incorrectly memoized.

---

# Authorization Testing

Admin

Verify

- User Management

- Restore Student

- Audit Logs

Staff

Verify

Cannot

- Create Users

- Restore Student

- View Audit Logs

---

# Database Testing

Verify

Indexes

Foreign Keys

Soft Delete

Flyway Migrations

Constraints

Unique Admission Number

Unique Aadhaar

---

# Search Testing

Search

Admission Number

Student Name

Guardian Name

Phone Number

Partial Match

Case Insensitive

---

# Filter Testing

Status

Gender

Admission Year

Leaving Year

Age

School

Multiple Filters Combined

---

# PDF Export Testing

Single Student

Multiple Students

Filtered Students

Verify

Photo Included

Student Information

Document List

Generated Date

Generated By

Proper Formatting

---

# Security Testing

Follow OWASP Top 10

Verify

Authentication

Authorization

Input Validation

SQL Injection

XSS

CSRF

Broken Authentication

Sensitive Data Exposure

File Upload Security

JWT Security

---

# Performance Testing

Framework

Apache JMeter

Verify

Search Performance

Pagination

Concurrent Users

Report Generation

Upload Performance

Download Performance

---

# Load Testing

Expected

100 Concurrent Users

Future

500+

---

# Accessibility Testing

Keyboard Navigation

Screen Reader Compatibility

Responsive Layout

Color Contrast

---

# Browser Testing

Chrome

Edge

Firefox

Safari

---

# Device Testing

Desktop

Laptop

Tablet

Mobile

---

# Regression Testing

Every bug fix must include

Regression Test

Every new feature

Must not break existing functionality

---

# Continuous Testing

GitHub Actions Pipeline

Workflow: `.github/workflows/ci-tests.yml`

Run

Backend Tests (`mvn verify` + JaCoCo ≥80% line coverage)

↓

Frontend Unit Tests (`npm run test:ci` + Karma ≥80% statements/lines)

↓

Cypress E2E (critical paths against local Postgres + API + `ng serve`)

↓

Build

↓

Deploy (Milestone 15)

Deployment only proceeds if all tests pass.

### Integration test database (Milestone 14 decision)

Backend `@SpringBootTest` / MockMvc suites use **H2 in PostgreSQL mode** (`application-test.yml`).
Testcontainers is deferred; introduce only if H2/Postgres divergence causes production defects.

---

# Manual Testing Checklist

Before Production

□ Login (password) — lands on `/dashboard`, not stuck on `/auth/login`

□ Login — sessionStorage has tokens; reload keeps session

□ Logout — clears session and returns to `/auth/login`

□ Google Login (when configured) — lands on `/dashboard`

□ Auth guards — unauthenticated user cannot open `/dashboard` or `/users`

□ Student Registration

□ Student Edit

□ Student Soft Delete

□ Student Restore

□ Search

□ Filters

□ Upload Documents

□ Preview Documents

□ Download Documents

□ Generate Single PDF

□ Generate Filtered PDF

□ Dashboard

□ User Management

□ Audit Logs

□ Security

□ Performance

---

# Production Verification

Smoke Test

Health Endpoint

```
/actuator/health
```

Database Connectivity

Cloud Storage

Authentication

Report Generation

Application Logs

Monitoring

---

# Goal

The application should be production-ready with:

✔ High reliability

✔ High security

✔ Easy maintenance

✔ Enterprise quality