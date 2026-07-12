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

Milestone 4 — Student Database Design (implementation on `milestone/student-database`)

**Current Sprint**

Sprint 1

**Current Milestone**

Milestone 4 — Student Database Design

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

## Milestone 3 — User Management (merged to `main`)

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

### Milestone 3 QA bug fixes (BUG-001–008)

* BUG-001–008 resolved and merged to `main`

---

## Milestone 4 — Student Database Design (in progress on `milestone/student-database`)

### Backend

* Flyway `V4__create_students_and_documents.sql`: `students` + `student_documents`
* CHECKs for gender, status, document_type; UNIQUE admission_number / aadhaar_number
* FK `student_documents.student_id → students(id) ON DELETE RESTRICT`
* Indexes including composite `(deleted, status)`
* JPA entities: `Student`, `StudentDocument` with `@SQLRestriction("deleted = false")`
* Enums: `Gender`, `StudentStatus`, `DocumentType`
* Schema IT: `StudentSchemaIntegrationTest` (H2 + Hibernate; asserts V4 migration on classpath)
* Column `exit_remarks` added (from requirements); synced into `docs/06_Database_Design.md`

### Out of scope (Milestone 5+)

* Student CRUD APIs / DTOs / services / controllers
* Repositories
* Frontend registration / uploads / GCS

---

# Technology Decisions

Unchanged from Milestone 1–3. Student schema specifics:

* Denormalized student row (guardian / education / medical inline)
* Soft delete on students and documents; never physical student delete
* Profile photo path on `students`; supporting docs metadata in `student_documents`
* Gender: `MALE` | `FEMALE` | `OTHER`
* Status: `ACTIVE` | `INACTIVE`

---

# Current Objective

Complete Milestone 4 review/approval, then commit on `milestone/student-database`.

---

# Current Branch

```text
milestone/student-database
```

---

# Known Decisions

* PostgreSQL / Supabase / GCS / Cloud Run / Angular / Tailwind / AG Grid / ECharts / Lucide / CDK / ngx-toastr
* Spring Security + JWT + Google OAuth (GIS ID token)
* Student soft delete; no standalone document module
* No Google self-registration — users must be pre-provisioned (Milestone 3 User Management)
* `@SQLRestriction` hides soft-deleted rows by default; restore queries (later) must bypass explicitly

---

# Pending Milestones

* Milestone 4 — Student Database Design (await approval / commit)
* Milestone 5 — Student Registration
* … (see roadmap)

---

# Blockers

None.

**Local note:** Windows service `postgresql-x64-17` may occupy `5432`; OMS Docker Postgres often uses `DB_PORT=5433` in local `.env`. Low Windows paging-file memory can cause Surefire fork OOMs; use `-DforkCount=0` if needed locally.

---

# Next Session Goal

1. User approval of Milestone 4 schema + entities
2. Commit on `milestone/student-database` when requested
3. Start Milestone 5 — Student Registration (CRUD + form + uploads)

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
