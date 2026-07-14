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

Milestone 6 — Student Profile (completed and closed; merged to `main`)

**Current Sprint**

Sprint 1

**Current Milestone**

Milestone 6 — Student Profile (complete)

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

## Milestone 4 — Student Database Design (merged to `main`)

### Backend

* Flyway `V4__create_students_and_documents.sql`: `students` + `student_documents`
* CHECKs for gender, status, document_type; UNIQUE admission_number / aadhaar_number
* FK `student_documents.student_id → students(id) ON DELETE RESTRICT`
* Indexes including composite `(deleted, status)`
* JPA entities: `Student`, `StudentDocument` with `@SQLRestriction("deleted = false")`
* Enums: `Gender`, `StudentStatus`, `DocumentType`
* Schema IT: `StudentSchemaIntegrationTest`
* Column `exit_remarks` added; synced into `docs/06_Database_Design.md`

---

## Milestone 5 — Student Registration (completed and closed; merged to `main`)

### Backend

* `StorageService` abstraction: local filesystem (`local`/`dev`/`test`) and GCS HTTP API (`prod`)
* Multipart limits: 10 MB/file, 50 MB/request
* `POST /api/v1/students` (multipart) for ADMIN/STAFF
* DTOs, MapStruct mapper, repositories, file validator (extension + MIME + size + magic bytes), audit fields
* Optional photo → `profile_photo_path`; supporting docs → `student_documents`
* Flyway `V5`: case-insensitive unique index on `LOWER(admission_number)`
* Uniqueness checks include soft-deleted rows; `DataIntegrityViolationException` → 409; multipart size → `ApiErrorResponse` 400
* Cross-field rule: date of birth on or before admission date
* Unit + integration tests for registration and local storage

### Frontend

* Registration form at `/students/new` (reactive forms, sections, validation)
* Past-or-present date validators + DOB ≤ admission date; `max` on date inputs
* Photo + supporting document upload UI with preview and upload progress
* List/form navigation via `app-button` + router (no button nested in anchor)
* `StudentService.create` via `FormData` with `reportProgress`

### Milestone 5 QA bug fixes (BUG-001–009)

* BUG-001–009 resolved; branch `milestone/student-registration` completed and closed

---

## Milestone 6 — Student Profile (completed and closed; merged to `main`)

### Backend

* `StorageService.load` for local + GCS (`?alt=media`)
* `GET /api/v1/students/{id}` — full detail DTO with `hasProfilePhoto` (no storage paths)
* `GET /api/v1/students/{id}/documents` — active documents only
* `GET /api/v1/students/{id}/documents/{documentId}/download` — attachment stream
* `GET /api/v1/students/{id}/photo` — inline image stream
* CORS exposes `Content-Disposition` for cross-origin download filenames
* Unit + integration tests for profile, download, photo, 404, auth

### Frontend

* Profile page at `/students/:id` (sections, photo blob URL, documents + download)
* Post-registration navigate to `/students/{id}`
* `StudentService` getById / listDocuments / fetchPhoto / downloadDocument
* Profile Back control uses button + navigate (no nested button-in-anchor)
* Gender display uses readable labels; photo load failures surface via toast + unavailable state
* Document downloads disable all Download actions while one is in progress
* List page copy reflects M6 profiles (list/search reserved for Milestone 8)
* Component tests for profile page; `fetchPhoto` covered in StudentService specs

### Docs

* `docs/07_API_Design.md` Milestone 6 contracts
* Session context: M6 closed and merged to `main`

### Milestone 6 QA bug fixes (BUG-001–008)

* BUG-001–008 resolved and merged to `main`
  * [#22](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/22) nested Back button
  * [#20](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/20) stale list copy
  * [#21](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/21) Content-Disposition CORS / filename fallback
  * [#26](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/26) silent photo errors
  * [#23](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/23) download busy-state UX
  * [#24](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/24) raw gender enum display
  * [#27](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/27) missing profile page tests
  * [#25](https://github.com/Vishnuvardhanpari/orphanage-student-management/issues/25) missing `fetchPhoto` service test

---

# Technology Decisions

Unchanged from Milestone 1–5. Student registration / profile specifics:

* Create-only multipart API; profile photo is not stored as a `PHOTOGRAPH` document row
* Storage provider selected by `oms.storage.type` (profile-driven DI)
* GCS uses Application Default Credentials + JSON upload API (no full google-cloud-storage SDK)
* Admission number uniqueness is case-insensitive at DB and application layers; soft-deleted rows remain reserved
* Profile photo is never exposed as a storage path in JSON; clients fetch via authenticated photo endpoint (blob URL in UI)
* CORS exposes `Content-Disposition` so browsers can read download filenames cross-origin

---

# Current Objective

Start Milestone 7 — Student Update when ready.

---

# Current Branch

```text
main
```

---

# Known Decisions

* PostgreSQL / Supabase / GCS / Cloud Run / Angular / Tailwind / AG Grid / ECharts / Lucide / CDK / ngx-toastr
* Spring Security + JWT + Google OAuth (GIS ID token)
* Student soft delete; no standalone document module
* No Google self-registration — users must be pre-provisioned (Milestone 3 User Management)
* `@SQLRestriction` hides soft-deleted rows by default; uniqueness and restore queries must bypass explicitly (native/`@Query`)
* Milestone 5 = registration only (completed)
* Milestone 6 = profile view + document list/download + authenticated `GET /students/{id}/photo`; after registration redirect to `/students/{id}` (completed)
* Profile photo is never exposed as a storage path in JSON; clients fetch via authenticated photo endpoint (blob URL in UI)

---

# Pending Milestones

* Milestone 7 — Student Update
* … (see roadmap)

---

# Blockers

None.

**Local note:** Windows service `postgresql-x64-17` may occupy `5432`; OMS Docker Postgres often uses `DB_PORT=5433` in local `.env`. Low Windows paging-file memory can cause Surefire fork OOMs; use `-DforkCount=0` if needed locally.

---

# Next Session Goal

1. Begin Milestone 7 — Student Update (planning → architecture review → implementation)
2. Create / switch to `milestone/student-update` when approved

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
