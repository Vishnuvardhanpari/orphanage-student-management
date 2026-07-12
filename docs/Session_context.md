# SESSION_CONTEXT

> This document represents the current development state of the project.
>
> Read this document together with:
>
> * docs/12_AI_Context.md
> * docs/13_DEVELOPMENT_ROADMAP.md
> * All relevant files in the `docs/` directory
>
> Before implementing any feature, review this document to understand the current sprint, completed work, and pending tasks.

---

# Project Status

**Project Name**

Orphanage Management System (OMS)

**Current Phase**

Milestone 2 — Authentication (implementation on `feature/authentication`)

**Current Sprint**

Sprint 1

**Current Milestone**

Milestone 2 — Authentication

---

# Completed

## Project Planning

* Project Vision completed
* Functional Requirements completed
* Feature List completed
* User Roles defined
* Business Rules documented
* Database Design documented
* API Design documented
* UI/UX Guidelines documented
* Deployment Strategy documented
* Coding Standards documented
* Testing Strategy documented
* AI Context documented
* Development Roadmap documented

---

## Milestone 1 — Project Initialization (Phases A–D)

* Root structure, Docker Postgres, Spring Boot foundation, Angular foundation, CORS/Prometheus integration
* See prior session notes for Phase A–D runtime verification details

---

## Milestone 2 — Authentication (implemented on `feature/authentication`)

### Backend

* Flyway migrations: `roles`, `users`, `refresh_tokens`, seeded ADMIN/STAFF roles
* JPA entities/repos for User, Role, RefreshToken
* BCrypt (strength 12), JWT access tokens (HS256), opaque refresh tokens with rotation + reuse detection
* Google ID token verification (GIS → `POST /api/v1/auth/google`); no self-registration
* Account lockout after failed password attempts
* Auth APIs: login, google, refresh, logout, me
* SecurityFilterChain: stateless JWT; Swagger denied in prod
* Bootstrap admin via `AdminBootstrapRunner` when users table is empty
* Unit + integration tests for JWT and auth flows (`JwtServiceTest`, `AuthIntegrationTest`)

### Frontend

* Login page (reactive form + optional Google GIS button)
* AuthService, TokenStorageService (`sessionStorage`)
* Auth interceptor with single-flight refresh on 401
* authGuard, guestGuard, roleGuard (Users = ADMIN)
* Topbar logout; sidebar hides Users for non-admins
* Unit tests for AuthService and authGuard

### Docs

* `docs/07_API_Design.md` updated with refresh/me contracts
* `.env.example` updated with JWT refresh, lockout, bootstrap admin vars

---

# Technology Decisions

Unchanged from Milestone 1. Auth specifics:

* JWT access + opaque refresh (DB-hashed)
* Google login via ID token exchange (not Spring redirect OAuth)
* Refresh tokens elevated into Milestone 2 (were Future/Optional in older docs)

---

# Current Objective

Complete Milestone 2 review, manual verification, and merge `feature/authentication` when approved.

---

# Current Branch

```text
feature/authentication
```

---

# Known Decisions

* PostgreSQL / Supabase / GCS / Cloud Run / Angular / Tailwind / AG Grid / ECharts / Lucide / CDK / ngx-toastr
* Spring Security + JWT + Google OAuth (GIS ID token)
* Student soft delete; no standalone document module
* No Google self-registration — users must be pre-provisioned (Milestone 3 User Management)

---

# Pending Milestones

* Milestone 2 — Authentication (finish review / merge)
* Milestone 3 — User Management
* Milestone 4 — Student Database Design
* … (see roadmap)

---

# Blockers

None.

**Local note:** Windows service `postgresql-x64-17` may occupy `5432`; OMS Docker Postgres often uses `DB_PORT=5433` in local `.env`.

---

# Next Session Goal

1. Manual E2E verify: password login, refresh, logout, guards
2. Configure `GOOGLE_CLIENT_ID` / `environment.googleClientId` for Google login smoke test
3. Merge `feature/authentication` after review
4. Start Milestone 3 — User Management

---

# Instructions for AI Agents

Before making any code changes:

1. Read 12_AI_Context.md.
2. Read 13_DEVELOPMENT_ROADMAP.md.
3. Read this SESSION_CONTEXT.md.
4. Review the current project structure.
5. Explain the implementation plan.
6. Implement only the current task.
7. Do not work on future milestones unless explicitly instructed.
8. Follow the project's coding standards, testing strategy, and architecture.
9. Prefer small, reviewable changes over large rewrites.
10. Keep the project production-ready at every stage.
