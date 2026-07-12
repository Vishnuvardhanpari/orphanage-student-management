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

Milestone 3 — User Management (implementation on `user_management`)

**Current Sprint**

Sprint 1

**Current Milestone**

Milestone 3 — User Management

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

---

## Milestone 2 — Authentication (merged to `main`)

### Backend

* Flyway migrations: `roles`, `users`, `refresh_tokens`, seeded ADMIN/STAFF roles
* JPA entities/repos for User, Role, RefreshToken
* BCrypt (strength 12), JWT access tokens (HS256), opaque refresh tokens with rotation + reuse detection
* Google ID token verification (GIS → `POST /api/v1/auth/google`); no self-registration
* Account lockout after failed password attempts
* Auth APIs: login, google, refresh, logout, me
* SecurityFilterChain: stateless JWT; Swagger denied in prod
* Bootstrap admin via `AdminBootstrapRunner` when users table is empty
* Unit + integration tests for JWT and auth flows

### Frontend

* Login page (reactive form + optional Google GIS button)
* AuthService, TokenStorageService (`sessionStorage`)
* Auth interceptor with single-flight refresh on 401
* authGuard, guestGuard, roleGuard (Users = ADMIN)
* Topbar logout; sidebar hides Users for non-admins
* Unit tests for AuthService and authGuard

---

## Milestone 3 — User Management (implemented on `user_management`)

### Backend

* `UserController` at `/api/v1/users` (ADMIN only via SecurityConfig + `@PreAuthorize`)
* List (page/search/filter), get, create, update, disable, enable, reset-password
* Soft disable only (`DELETE` aliases disable); revoke refresh tokens on disable/reset
* Safety: cannot disable/demote self; cannot disable/demote last enabled ADMIN
* Create supports LOCAL (password) and GOOGLE (no password) provisioning
* Reset password upgrades GOOGLE → LOCAL_GOOGLE
* Unit tests (`UserServiceTest`) + integration tests (`UserManagementIntegrationTest`)

### Frontend

* User list (AG Grid + server-side paging/filters/**sorting**)
* User form (create/edit)
* User details (enable/disable, reset password via CDK dialogs)
* User list actions: Angular AG Grid cell renderer with a11y labels; self-disable hidden on list
* Reset password dialog: confirm-password match validation
* UserService + models; routes under `/users` with `roleGuard`

### Docs

* `docs/07_API_Design.md` Users section expanded with full contracts
* Enable Staff documented in Features, Business Rules, UI UX, and AI Context
* This Session_context updated for M3

### Milestone 3 QA bug fixes (BUG-001–008)

* BUG-001: `disable()` uses `saveAndFlush` before refresh-token revoke so `enabled=false` persists
* BUG-002: Users list hides Disable for the current admin (matches detail page)
* BUG-003: ITs assert DB persistence + login rejection while disabled; `DisabledUserLoginIT` restored
* BUG-004: AG Grid `sortChanged` drives Spring `sort` query param; client compare disabled
* BUG-005: Enable Staff added to Features / Business Rules / UI UX / AI Context
* BUG-006: List returns stable `PageResponse` DTO (no raw `PageImpl` JSON)
* BUG-007: Reset password confirm field + match validator
* BUG-008: Actions column uses Angular cell renderer with `aria-label`s

---

# Technology Decisions

Unchanged from Milestone 1–2. User management specifics:

* Soft disable (no physical user delete)
* Single role per user (`ADMIN` | `STAFF`)
* Pre-provision only — no open self-registration

---

# Current Objective

Milestone 3 QA bugs BUG-001–008 fixed on `user_management`. Await user approval, then commit/merge.

---

# Current Branch

```text
user_management
```

---

# Known Decisions

* PostgreSQL / Supabase / GCS / Cloud Run / Angular / Tailwind / AG Grid / ECharts / Lucide / CDK / ngx-toastr
* Spring Security + JWT + Google OAuth (GIS ID token)
* Student soft delete; no standalone document module
* No Google self-registration — users must be pre-provisioned (Milestone 3 User Management)

---

# Pending Milestones

* Milestone 3 — User Management (finish review / merge)
* Milestone 4 — Student Database Design
* … (see roadmap)

---

# Blockers

None.

**Local note:** Windows service `postgresql-x64-17` may occupy `5432`; OMS Docker Postgres often uses `DB_PORT=5433` in local `.env`.

---

# Next Session Goal

1. User approval of Milestone 3 bug fixes; commit when requested
2. Manual E2E smoke: login, disable/enable persistence, list sort, reset password confirm, self-disable hidden
3. Merge `user_management` after approval
4. Start Milestone 4 — Student Database Design

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
