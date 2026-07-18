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

Milestone 12 — Audit Logging (next)

**Current Sprint**

Sprint 1

**Current Milestone**

Milestone 12 — Audit Logging

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

* BUG-001–007 resolved and merged to `main`

---

## Milestone 8 — Student Search (completed and closed; merged to `main`)

### Backend

* `GET /api/v1/students` — single list/search endpoint (no separate `/students/search`)
* Query params: `search`, `gender`, `status`, `admissionYear`, `school`, `ageMin`, `ageMax`, plus `page`/`size`/`sort` (default `admissionDate,desc`, size 20)
* Global `search` matches name, admission number, Aadhaar, guardian, and phone
* `ageMin`/`ageMax` translated server-side into a `date_of_birth` range (age never persisted); `400` on negative values or `ageMin > ageMax`
* JPA Specifications (`StudentSpecifications`) for dynamic filtering; soft-deleted rows excluded by default
* `StudentSummaryResponse` list DTO; shared `PageResponse` moved to `common/dto` (old `user/dto/PageResponse` removed)
* Flyway `V6__add_students_search_indexes.sql`: index on `students(date_of_birth)` for the age-range filter
* Unit tests (`StudentServiceTest`) + integration tests (`StudentSearchIntegrationTest`)

### Frontend

* Student list page at `/students` — AG Grid with server-side pagination, sorting, and filters
* Filter panel: global search, gender, status, admission year, school, age range (min/max)
* Age column sorts via `dateOfBirth` with inverted direction; server-side sort mapping per column
* Empty states for "no students yet" vs "no matching students"; actions cell (View / Edit)

### Milestone 8 QA bug fixes (issues #37–#43)

* #37–#43 resolved and merged to `main`

---

## Milestone 9 — Student Soft Delete & Restore (completed and closed; merged to `main` via PR #54)

### Backend

* `DELETE /api/v1/students/{id}` — soft delete (`deleted`, `deletedBy`, `deletedDate`, `status=INACTIVE`); ADMIN/STAFF; `204`; already deleted → `404`. Optionally accepts a JSON body (`exitDate`, `exitReason`, `exitRemarks`) to record exit details in the same request (QA BUG-005)
* `PATCH /api/v1/students/{id}/restore` — ADMIN only; clear delete flags + `ACTIVE`; `200` detail; not deleted → `409`
* `GET /api/v1/students/inactive` — ADMIN/STAFF; paginated soft-deleted rows (native query bypasses `@SQLRestriction`); default sort `deletedDate,desc`; response rows include `deletedDate` (QA BUG-007)
* Sort whitelist for `/students` and `/students/inactive` includes `schoolName` and `standard` (QA BUG-002)
* Profile/photo/documents **reads** include soft-deleted students (archived profile)
* Mutations still require an active student → archived → `404`
* No Flyway / schema changes
* Unit tests + `StudentSoftDeleteIntegrationTest` (authz, inactive list, restore, mutations blocked, optional exit details, exit-date validation)

### Frontend

* Active list: Archive action opens `ArchiveStudentDialog` (optional exit date/reason/remarks) instead of a plain confirm; link to Archived students; Status filter removed from the active list (QA BUG-003, its value duplicated the page itself)
* `/students/inactive` archived list (ADMIN + STAFF); Restore action ADMIN-only; grid shows the `Deleted on` column (QA BUG-007)
* `/students/inactive/:id` read-only archived profile (photo + document download); Restore for ADMIN
* Active profile: Archive action via `ArchiveStudentDialog`; Edit hidden when archived
* Archived/active UI mode (`archived()`) is derived from the loaded student's `status`, not from route data (QA BUG-001); route data is only a loading-state hint
* `app-button` supports a `routerLink` input so navigation actions render a single `<a>` instead of nesting a `<button>` inside an anchor (QA BUG-004); the projected label is captured once via an `<ng-template>` and replayed into whichever branch renders via `NgTemplateOutlet`, so it survives Angular's static content-projection resolution across the `@if`/`@else` branches (QA BUG-UI-001)
* Sidebar: Archived Students; Students nav exact-match so inactive does not highlight both
* `StudentService.softDelete(id, exitDetails?)` / `restore` / `listInactive`
* Dedicated spec coverage added for the archived list page, the archive/restore dialogs, and the `Button` routerLink variant (QA BUG-006)

### Docs

* `docs/07_API_Design.md` Milestone 9 contracts expanded, including the optional soft-delete request body and `schoolName`/`standard` sort support
* Session context: M9 closed and merged to `main`

### Milestone 9 QA bug fixes (BUG-001–007, issues #45–#51)

* BUG-001 — Archived UI mode is now derived from `student.status` instead of static route data
* BUG-002 — `schoolName`/`standard` added to both list endpoints' sort whitelists
* BUG-003 — Status filter removed from the active student list (redundant with the list itself)
* BUG-004 — `app-button` gained a `routerLink` input; student pages no longer nest a `<button>` inside an `<a>`
* BUG-005 — `DELETE /students/{id}` now accepts an optional exit-details body; `ArchiveStudentDialog` captures it in the UI
* BUG-006 — Added missing specs for the archived list page, archive/restore dialog flows, and the `Button` routerLink variant
* BUG-007 — `deletedDate` added to `StudentSummaryResponse`/model and the archived grid

### Milestone 9 QA bug fixes, round 2 (BUG-UI-001–002, issues #52–#53)

* BUG-UI-001 — `app-button`'s `routerLink` (`<a>`) variant rendered with no visible label. Root cause: Angular resolves content projection statically at compile time, so a component template that places `<ng-content>` directly inside more than one `@if`/`@else` branch only ever projects into one of the branches (documented upstream: angular/angular#7795, #61282, #53310) — ours projected into the `<button>` `@else` branch only. Fixed in `button.html` by hoisting the projected content into a single `<ng-template #label>` and rendering it in both branches via `<ng-container [ngTemplateOutlet]="label" />`; no change to the component's public API (inputs/outputs) or any of the 3 call sites (`student-list-page`, `student-inactive-list-page`)
* BUG-UI-002 — `button.spec.ts` only asserted tag type/CSS class for the `routerLink` variant, never the projected label, which is why BUG-UI-001 shipped undetected. Added label-text regression coverage (`button.spec.ts`, driven through a host wrapper component so real content projection is exercised) plus header-label assertions in `student-list-page.spec.ts` and `student-inactive-list-page.spec.ts`

### Milestone 9 closure

* Issues #45–#53 closed
* Merged to `main` via PR #54 (`e68e03c`)

---

## Milestone 10 — Reports & PDF Export (completed and closed; merged to `main`)

### Backend

* OpenPDF dependency; `oms.reports` config (organization name, max selected/filter limits)
* Shared `StudentSpecifications.buildListSpecification` used by list + filter export
* Exact admission lookup via additive `GET /students?admissionNumber=` (case-insensitive)
* `GET /api/v1/reports/student/{id}`, `POST /api/v1/reports/students`, `POST /api/v1/reports/filter`
* Filter export `scope`: `ACTIVE` | `ARCHIVED` | `ALL` (ReportService orchestrates active Spec vs deleted native query)
* `GET /students/inactive` accepts the same optional filters as the active list (except status/admissionNumber)
* Selected-export max enforced only in `ReportService` (config-driven; no hardcoded `@Size(max=50)`)
* `StudentPdfRenderer`: org header, student sections, profile photo, inline image docs, PDF doc references, page numbers, generated date/by
* SLF4J audit logs for report generation (persistent `audit_logs` deferred to M12)
* Unit tests (`StudentPdfRendererTest`, `ReportServiceTest`) + `ReportExportIntegrationTest`

### Frontend

* `ReportService` + blob download; Reports page (single by admission #, filtered export with scope + match-count preview, link to list for selection)
* CDK `ExportReportDialog` with filter/selection preview (`+N more` when truncated)
* Student list + archived list: AG Grid multi-select with cross-page selection map + client max-selected check + Export selected
* Student profile: Export PDF (active and archived)
* Component specs for report page, export dialog; ReportService HTTP specs

### Docs

* `docs/07_API_Design.md` Reports section expanded (scope, admissionNumber, inactive filters)
* `.env.example` report config keys documented

### Milestone 10 QA bug fixes (BUG-001–009, issues #55–#63)

* BUG-001 — Exact `admissionNumber` list param; Reports page no longer uses fragile contains-search `size=5`
* BUG-002 — Cross-page selection via `Map<id, StudentSummary>` on active and archived lists
* BUG-003 — Removed hardcoded `@Size(max=50)`; service uses `ReportProperties`
* BUG-004 / BUG-009 — Filter export supports Active / Archived / All; Status dropdown replaced by Scope
* BUG-005 — Client max-selected check (`environment.reportsMaxSelected`); server remains authoritative
* BUG-006 — Added report page, export dialog, and related list/service specs
* BUG-007 — Filtered export confirms with match count (and blocks over `reportsMaxFilterResults`)
* BUG-008 — Selection preview appends `+N more` when truncated

### Milestone 10 closure

* Issues #55–#63 addressed on `milestone/report-export`
* Merged to `main` via PR #64 (`cd23119`)

---

## Milestone 11 — Dashboard (completed and closed; merged to `main`)

### Backend

* `DashboardController` at `/api/v1/dashboard` (ADMIN/STAFF via `@PreAuthorize`)
* Endpoints: `GET /summary`, `GET /admissions`, `GET /gender`, `GET /status`
* `DashboardService` aggregates via `StudentRepository` (`COUNT` / `GROUP BY`; native queries bypass `@SQLRestriction` for inactive/status/admissions-including-deleted)
* Summary: total/active/inactive, new admissions (current UTC calendar month), male/female (active only), recent admissions/updates (top 5 active)
* Admissions trend: last 12 months zero-filled; includes later-archived students
* Gender chart: active roster including `OTHER`; status chart: ACTIVE vs INACTIVE across all retained rows
* No Flyway / schema changes
* Unit tests (`DashboardServiceTest`) + integration tests (`DashboardIntegrationTest`)

### Frontend

* Dashboard page replaces M11 placeholder: stat cards (Left Students label for inactive), ECharts (gender / status distribution / monthly admissions), recent lists, quick actions, skeletons, refresh
* `provideEchartsCore` scoped to dashboard page (tree-shaken echarts core: pie + bar)
* `DashboardService` + models; component + HTTP specs
* Chart colors resolved from CSS theme tokens (`--color-primary-600`, `--color-warning-600`, `--color-inactive-500`) with fallbacks
* Chart a11y: `role="img"`, `aria-label`, visually hidden value summaries
* New Admissions hint discloses UTC month window (`This month (UTC)`)

### Docs

* `docs/07_API_Design.md` Dashboard section added
* `docs/08_UI_UX.md` Dashboard section updated for shipped cards/charts/quick actions/error Retry (no longer “Charts (Future)”)

### Milestone 11 QA bug fixes (BUG-001–007, issues #65–#71)

* BUG-001 — Dashboard GETs use `SKIP_ERROR_TOAST`; empty state is the sole error UX (no multi-toast spam from `forkJoin`)
* BUG-002 — Error empty state projects **Retry** (same pattern as student list)
* BUG-003 — Charts expose `role="img"` / `aria-label` plus visually hidden text summaries
* BUG-004 — `docs/08_UI_UX.md` reflects implemented dashboard; removed Future “Dashboard Charts”
* BUG-005 — Middle chart title renamed to **Status Distribution**
* BUG-006 — ECharts colors read from theme CSS variables via `getComputedStyle`
* BUG-007 — New Admissions hint: “This month (UTC)” (UTC product decision unchanged)

### Milestone 11 closure

* Issues #65–#71 closed
* Merged to `main` via PR #72 (`215c1b9`)

---

# Technology Decisions

Unchanged from Milestone 1–10, plus Milestone 11 dashboard definitions:

* Soft delete/restore with optional exit payload captured at archive time (extended in QA BUG-005; no new endpoint)
* Archived list/profile readable by ADMIN and STAFF; restore ADMIN-only (STAFF read access is an explicit product decision vs Business Rules wording that emphasizes administrators for historical search)
* Active `GET /students` continues to exclude soft-deleted rows
* Document soft-delete / GCS objects are not cascaded when archiving a student
* Dashboard: “Left Students” UI = `inactiveStudents` (soft-deleted); new admissions = current UTC calendar month (UI hint: “This month (UTC)”); gender cards active-only; status chart titled **Status Distribution**

---

# Current Objective

Begin Milestone 12 — Audit Logging (planning / architecture review before implementation).

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
* `@SQLRestriction` hides soft-deleted rows by default; uniqueness and restore/inactive queries must bypass explicitly (native/`@Query`)
* Milestone 5–11 completed and merged to `main`
* Milestone 9: soft delete with optional exit details captured at archive time (QA BUG-005); ADMIN+STAFF can view archived students; only ADMIN restores
* Profile photo is never exposed as a storage path in JSON; clients fetch via authenticated photo endpoint (blob URL in UI)
* Milestone 10: PDF docs are references only; image docs embedded inline; report audit is SLF4J until Milestone 12; sync PDF download (no job queue); PDFs not persisted to GCS
* Milestone 10 filter export uses `scope` (`ACTIVE` | `ARCHIVED` | `ALL`); active path uses `GET /students` predicates; archived path uses the inactive/deleted query path; `GET /students` itself still excludes soft-deleted rows
* Milestone 11: dashboard recent rows expose `firstName`/`lastName` (not a computed `fullName`); monthly admissions use portable `EXTRACT(YEAR/MONTH)` for H2 + PostgreSQL compatibility; New Admissions window is UTC (UI discloses it)

---

# Pending Milestones

* Milestone 12 — Audit Logging
* … (see roadmap)

---

# Blockers

None.

**Local note:** Windows service `postgresql-x64-17` may occupy `5432`; OMS Docker Postgres often uses `DB_PORT=5433` in local `.env`. Low Windows paging-file memory can cause Surefire fork OOMs; use `-DforkCount=0` if needed locally.

---

# Next Session Goal

1. Read Milestone 12 scope in `docs/13_DEVELOPMENT_ROADMAP.md`
2. Planning + Architecture Review for Audit Logging
3. Create/switch to `milestone/audit-logging` after approval

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
