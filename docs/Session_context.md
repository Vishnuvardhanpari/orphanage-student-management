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

Milestone 7 — Student Update (completed and closed; merged to `main` with QA bug fixes BUG-001–007)

**Current Sprint**

Sprint 1

**Current Milestone**

Milestone 7 — Student Update

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

---

## Milestone 7 — Student Update (completed and closed; merged to `main`)

### Backend

* `UpdateStudentRequest` (no `admissionNumber`); MapStruct `updateFromDto`
* Aadhaar uniqueness excluding current student, including soft-deleted rows → `409`
* `PUT /api/v1/students/{id}` — JSON field update → `StudentDetailResponse`
* `PUT /api/v1/students/{id}/photo` — replace profile photo → `204`
* `POST /api/v1/students/{id}/documents` — add supporting docs → `201` list
* `PUT /api/v1/students/{id}/documents/{documentId}` — replace one document → `200`
* `DELETE /api/v1/students/{id}/photo` — remove profile photo → `204` (QA BUG-007)
* `DELETE /api/v1/students/{id}/documents/{documentId}` — logical document delete → `204` (QA BUG-007)
* Store-new-then-delete-old for photo/document replace; compensation on store failure
* Admission number immutable; status/exit/soft-delete not editable
* Unit tests (`StudentServiceTest`) + integration (`StudentUpdateIntegrationTest`)

### Frontend

* Edit route `/students/:id/edit` (registered before `:id`)
* `StudentFormPage` create | edit mode: prefill, read-only admission number, separate field save vs photo/doc actions
* Profile page **Edit** CTA
* `StudentService` update / replacePhoto / addDocuments / replaceDocument
* Form + service + profile Edit navigation specs

### Docs

* `docs/07_API_Design.md` Milestone 7 contracts (four endpoints)

### Milestone 7 QA bug fixes (BUG-001–007, issues #29–#35)

* BUG-001: `StudentFileUpload` photo preview now syncs with the parent `photo` input (effect clears stale preview after successful replace)
* BUG-002: Save changes with pending photo/documents opens a confirm dialog ("Save and discard") before discarding un-uploaded files
* BUG-003: duplicate error toasts removed — the global `errorInterceptor` owns HTTP error toasts; blob endpoints (`fetchPhoto`, `downloadDocument`) opt out via new `SKIP_ERROR_TOAST` `HttpContextToken` and keep their contextual toast
* BUG-004: replace-document path validates type/size client-side via shared `student-file-validation.ts` helpers (also used by add path)
* BUG-005: separate `photoUploading` / `docsUploading` signals; button labels no longer cross-trigger; `mediaBusy` is now a computed over all media operations
* BUG-006: replace row includes a document-type select; selected type is sent on `PUT .../documents/{documentId}`
* BUG-007: new endpoints `DELETE /students/{id}/photo` (clears path + storage) and `DELETE /students/{id}/documents/{documentId}` (soft delete, storage retained); edit page has Remove photo / Delete document actions with confirm dialogs
* `ConfirmDialog` moved from `features/user/components` to `shared/components/confirm-dialog` for reuse
* Integration tests now purge student tables with native SQL (bypasses `@SQLRestriction`) so soft-deleted rows cannot leak between suites

---

# Technology Decisions

Unchanged from Milestone 1–6. Student update specifics:

* Four write endpoints (fields JSON; photo/docs multipart) rather than one fat multipart PUT
* Full PUT for editable fields (not PATCH); admission number omitted from update DTO
* Profile photo remains on `profile_photo_path` only (never a `PHOTOGRAPH` document row)
* Field save and media uploads are independent so a failed file upload does not roll back text changes
* Document logical DELETE and photo removal shipped with Milestone 7 QA fixes (BUG-007); document storage objects are retained on soft delete

---

# Current Objective

Begin Milestone 8 — Student Search (list, pagination, filters).

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
* Milestone 6 = profile view + document list/download + authenticated `GET /students/{id}/photo` (completed)
* Milestone 7 = update fields + replace photo + add/replace documents (implemented, pending merge)
* Profile photo is never exposed as a storage path in JSON; clients fetch via authenticated photo endpoint (blob URL in UI)

---

# Pending Milestones

* Milestone 8 — Student Search
* … (see roadmap)

---

# Blockers

None.

**Local note:** Windows service `postgresql-x64-17` may occupy `5432`; OMS Docker Postgres often uses `DB_PORT=5433` in local `.env`. Low Windows paging-file memory can cause Surefire fork OOMs; use `-DforkCount=0` if needed locally.

---

# Next Session Goal

1. Begin Milestone 8 — Student Search (list page, server-side pagination/filtering)

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
