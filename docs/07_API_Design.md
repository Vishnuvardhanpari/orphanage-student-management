# API Design

## API Standards

* RESTful APIs
* JSON Request/Response
* Versioned APIs
* Stateless Authentication using JWT

Base URL

```
/api/v1
```

---

# Authentication

POST

```
/auth/login
```

Request

```json
{
  "username": "admin",
  "password": "********"
}
```

Response `200`

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "username": "admin",
    "email": "admin@oms.local",
    "role": "ADMIN"
  }
}
```

POST

```
/auth/google
```

Request

```json
{
  "idToken": "<google-id-token>"
}
```

Response shape matches `/auth/login`. Google login requires a pre-provisioned enabled user whose email matches the verified Google account. Open self-registration is not supported.

POST

```
/auth/refresh
```

Request

```json
{
  "refreshToken": "<opaque>"
}
```

Rotates the refresh token (reuse of a revoked token revokes all sessions for that user) and returns a new token pair using the same response shape as login.

POST

```
/auth/logout
```

Request (optional body)

```json
{
  "refreshToken": "<opaque>"
}
```

Revokes the presented refresh token, or all refresh tokens for the authenticated user when Bearer is present without a refresh body. Response `204`.

When `Authorization: Bearer` is present, an `AUTH`/`LOGOUT` audit row is written. The Angular client attaches Bearer on logout when an access token exists (endpoint remains `permitAll` so revoke-without-JWT still works; no refresh-on-401 for logout).

GET

```
/auth/me
```

Requires `Authorization: Bearer <accessToken>`.

Response `200`

```json
{
  "id": "uuid",
  "username": "admin",
  "email": "admin@oms.local",
  "role": "ADMIN"
}
```

Public auth endpoints (no JWT required): `/auth/login`, `/auth/google`, `/auth/refresh`, `/auth/logout`.

---

# Students

Base path: `/api/v1/students`

Authorization: JWT required. `ADMIN` or `STAFF` may register students, view and update profiles (including archived reads), manage photos/documents, list documents, download files, soft-delete students, and list inactive students. Only `ADMIN` may restore soft-deleted students.

## Register student (Milestone 5)

`POST /students`

`Content-Type: multipart/form-data`

| Part / field | Type | Required | Description |
|--------------|------|----------|-------------|
| `data` | JSON (`application/json`) | Yes | `CreateStudentRequest` body |
| `photo` | file | No | Profile photo (JPG/JPEG/PNG, max 10 MB) |
| `documents` | file (repeatable) | No | Supporting documents (PDF/JPG/JPEG/PNG, max 10 MB each) |
| `documentTypes` | string (repeatable) | Yes if `documents` present | Parallel `DocumentType` enum values (not `PHOTOGRAPH`) |

### CreateStudentRequest

Required: `admissionNumber`, `firstName`, `gender`, `dateOfBirth`, `admissionDate`

Optional: `lastName`, `bloodGroup`, `religion`, `nationality`, `aadhaarNumber` (12 digits if set), `phoneNumber`, guardian fields, education fields, medical fields

Not accepted: `status` (always `ACTIVE`), exit fields, soft-delete fields

### Response `201 Created`

```json
{
  "id": "uuid",
  "admissionNumber": "ADM-100",
  "status": "ACTIVE",
  "createdDate": "2026-07-13T05:00:00Z"
}
```

### Errors

* `400` — validation / invalid file / mismatched `documents` and `documentTypes`
* `401` — unauthenticated
* `403` — authenticated but not ADMIN/STAFF
* `409` — duplicate admission number or Aadhaar

---

## Get student profile (Milestone 6)

`GET /students/{id}`

### Response `200 OK`

`StudentDetailResponse` — personal, guardian, education, medical, admission/exit, `status`, `hasProfilePhoto`, `createdDate`, `updatedDate`.

Does **not** return storage paths (`profilePhotoPath`).

Returns active **and** soft-deleted (archived) students so ADMIN and STAFF can open archived profiles. Unknown id → `404`.

### Errors

* `401` — unauthenticated
* `403` — authenticated but not ADMIN/STAFF
* `404` — student not found

---

## Stream profile photo (Milestone 6)

`GET /students/{id}/photo`

Returns the stored image bytes with `Content-Disposition: inline` and an appropriate `Content-Type` (`image/jpeg` / `image/png`).

### Errors

* `401` / `403` — as above
* `404` — student not found, no photo, or missing storage object

---

## List student documents (Milestone 6)

`GET /students/{id}/documents`

Returns active (non-deleted) supporting documents only, newest first.

### Response item `StudentDocumentResponse`

`id`, `documentType`, `originalFileName`, `contentType`, `fileSize`, `uploadedDate`

Does **not** return `storagePath` / `storedFileName`.

### Errors

* `401` / `403` — as above
* `404` — student not found

---

## Download student document (Milestone 6)

`GET /students/{id}/documents/{documentId}/download`

Returns file bytes with `Content-Disposition: attachment` (original filename) and stored `Content-Type`.

Document must belong to the given student and not be soft-deleted.

### Errors

* `401` / `403` — as above
* `404` — student or document not found / ownership mismatch / missing storage object

---

## Update student fields (Milestone 7)

`PUT /students/{id}`

`Content-Type: application/json`

Updates editable student fields only. Photo and supporting documents use the dedicated endpoints below.

### UpdateStudentRequest

Required: `firstName`, `gender`, `dateOfBirth`, `admissionDate`

Optional: `lastName`, `bloodGroup`, `religion`, `nationality`, `aadhaarNumber` (12 digits if set), `phoneNumber`, guardian fields, education fields, medical fields

**Not accepted / immutable:** `admissionNumber` (cannot change after creation), `status`, exit fields, soft-delete fields, audit create fields

Cross-field: `dateOfBirth` must be on or before `admissionDate`.

Aadhaar uniqueness: if provided, must not belong to another student (including soft-deleted rows). The current student's own Aadhaar is allowed.

### Response `200 OK`

`StudentDetailResponse` (same shape as Get student profile). Soft-deleted or unknown → `404`.

### Errors

* `400` — validation / DOB after admission date
* `401` / `403` — as above
* `404` — student not found
* `409` — Aadhaar number already in use by another student

---

## Replace profile photo (Milestone 7)

`PUT /students/{id}/photo`

`Content-Type: multipart/form-data`

| Part | Type | Required | Description |
|------|------|----------|-------------|
| `photo` | file | Yes | Profile photo (JPG/JPEG/PNG, max 10 MB) |

Replaces `profile_photo_path`. Old storage object is removed after the new file is stored and the DB path is updated. Not stored as a `PHOTOGRAPH` document row.

### Response `204 No Content`

### Errors

* `400` — missing/invalid/oversized file
* `401` / `403` — as above
* `404` — student not found

---

## Add supporting documents (Milestone 7)

`POST /students/{id}/documents`

`Content-Type: multipart/form-data`

| Part / field | Type | Required | Description |
|--------------|------|----------|-------------|
| `documents` | file (repeatable) | Yes (at least one) | Supporting documents (PDF/JPG/JPEG/PNG, max 10 MB each) |
| `documentTypes` | string (repeatable) | Yes | Parallel `DocumentType` values (not `PHOTOGRAPH`); count must match `documents` |

### Response `201 Created`

Array of `StudentDocumentResponse` for the newly created document rows.

### Errors

* `400` — validation / mismatched counts / `PHOTOGRAPH` type / empty upload
* `401` / `403` — as above
* `404` — student not found

---

## Replace supporting document (Milestone 7)

`PUT /students/{id}/documents/{documentId}`

`Content-Type: multipart/form-data`

| Part / field | Type | Required | Description |
|--------------|------|----------|-------------|
| `document` | file | Yes | Replacement file (PDF/JPG/JPEG/PNG, max 10 MB) |
| `documentType` | string | No | If present, updates `DocumentType` (not `PHOTOGRAPH`) |

Document must belong to the given student and not be soft-deleted. Old storage object is removed after the new file is stored and metadata is updated.

### Response `200 OK`

`StudentDocumentResponse` for the updated document.

### Errors

* `400` — validation / invalid type
* `401` / `403` — as above
* `404` — student or document not found / ownership mismatch

---

## Remove profile photo (Milestone 7 QA — BUG-007)

`DELETE /students/{id}/photo`

Clears `profile_photo_path` and removes the storage object (best effort; DB is the source of truth).

### Response `204 No Content`

### Errors

* `401` / `403` — as above
* `404` — student not found, or no photo on file

---

## Delete supporting document (Milestone 7 QA — BUG-007)

`DELETE /students/{id}/documents/{documentId}`

Logical (soft) delete: sets `deleted = true` and `deleted_date`. The storage object is retained for historical recovery. The document disappears from listing, download, and further replace/delete calls.

### Response `204 No Content`

### Errors

* `401` / `403` — as above
* `404` — student or document not found / ownership mismatch / already deleted

---

## List / search students (Milestone 8)

`GET /students`

Paginated student list with optional global search, combinable filters, and sorting. ADMIN and STAFF. Soft-deleted students are always excluded.

### Query parameters (all optional)

* `page` — zero-based page index (default `0`)
* `size` — page size (default `20`)
* `sort` — `property,direction` (default `admissionDate,desc`). Allowed properties: `admissionNumber`, `firstName`, `lastName`, `gender`, `dateOfBirth`, `admissionDate`, `status`, `createdDate`, `schoolName`, `standard`. Any other property → `400`.
* `search` — case-insensitive contains match across first name, last name, full name, admission number, Aadhaar number, guardian name, and phone number
* `admissionNumber` — exact case-insensitive match on admission number (unique); typically used with `size=1` for Reports single-student lookup
* `gender` — `MALE` | `FEMALE` | `OTHER`
* `status` — `ACTIVE` | `INACTIVE`
* `admissionYear` — calendar year of `admissionDate` (e.g. `2024`)
* `school` — case-insensitive contains match on school name
* `ageMin` / `ageMax` — inclusive age range in whole years, translated server-side into a `dateOfBirth` window (age is derived, never persisted). `400` if negative or `ageMin > ageMax`.

Filters may be combined; each omitted parameter is simply not applied.

### Response `200`

```json
{
  "content": [
    {
      "id": "uuid",
      "admissionNumber": "ADM-2024-001",
      "firstName": "Anita",
      "lastName": "Sharma",
      "gender": "FEMALE",
      "dateOfBirth": "2014-03-15",
      "status": "ACTIVE",
      "schoolName": "Green Valley School",
      "standard": "5",
      "admissionDate": "2024-06-01"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

### Errors

* `400` — invalid sort property, invalid age range, or invalid enum filter value
* `401` — missing/invalid token
* `403` — authenticated but not ADMIN/STAFF

GET

```
/students/{id}
```

*(See Get student profile above.)*

POST

```
/students
```

Supports

* Student Details
* Student Photo Upload
* Multiple Document Upload
*(See Register student section above for the Milestone 5 multipart contract.)*

PUT

```
/students/{id}
```

*(See Update student fields above.)*

PUT

```
/students/{id}/photo
```

*(See Replace profile photo above.)*

POST

```
/students/{id}/documents
```

*(See Add supporting documents above.)*

PUT

```
/students/{id}/documents/{documentId}
```

*(See Replace supporting document above.)*

## Soft delete student (Milestone 9; exit details added in Milestone 9 QA — BUG-005)

`DELETE /students/{id}`

`Content-Type: application/json` (optional — the request body itself is optional)

ADMIN and STAFF. Sets the soft-delete flags and, when the caller provides them, records the student's exit details.

### SoftDeleteStudentRequest (optional body; every field optional)

| Field | Type | Description |
|-------|------|-------------|
| `exitDate` | date | Must be on/before today and on/after the student's `admissionDate` |
| `exitReason` | string | Free text |
| `exitRemarks` | string | Free text |

Omitting the body (or any individual field) preserves the original flags-only behavior for that field — existing exit fields are left unchanged unless explicitly provided.

Sets:

* `deleted = true`
* `deletedBy` — current user id
* `deletedDate` — server timestamp
* `status = INACTIVE`
* `exitDate` / `exitReason` / `exitRemarks` — only when provided in the request body

Does not cascade soft-delete of documents or remove storage objects.

### Response `204 No Content`

### Errors

* `400` — `exitDate` in the future or before `admissionDate`
* `401` / `403` — as above
* `404` — student not found or already soft-deleted

---

## Restore student (Milestone 9)

`PATCH /students/{id}/restore`

**ADMIN only.** Clears soft-delete flags and reactivates the student. Does **not** modify exit fields.

Sets:

* `deleted = false`
* `deletedBy = null`
* `deletedDate = null`
* `status = ACTIVE`

### Response `200 OK`

`StudentDetailResponse` (same shape as Get student profile).

### Errors

* `401` — unauthenticated
* `403` — authenticated but not ADMIN (STAFF receive 403)
* `404` — student not found
* `409` — student is not soft-deleted (already active)

---

## List inactive / archived students (Milestone 9)

`GET /students/inactive`

Paginated list of soft-deleted students (`deleted = true`). ADMIN and STAFF. Bypasses the default soft-delete filter used by `GET /students`.

### Query parameters

* `page` — zero-based page index (default `0`)
* `size` — page size (default `20`)
* `sort` — `property,direction` (default `deletedDate,desc`). Allowed properties: same as list/search, plus `deletedDate`. Any other property → `400`.
* `search` — same semantics as active list search
* `gender` — `MALE` | `FEMALE` | `OTHER`
* `admissionYear` — calendar year of `admissionDate`
* `school` — case-insensitive contains match on school name
* `ageMin` / `ageMax` — same age-range semantics as active list

Response also includes `deletedDate` (ISO-8601 timestamp) per row, only ever populated here (Milestone 9 QA — BUG-007).

### Response `200`

Same `PageResponse<StudentSummaryResponse>` shape as Milestone 8 list/search. Rows have `status = INACTIVE`.

### Errors

* `400` — unsupported sort property
* `401` / `403` — as above

---

## Archived profile reads (Milestone 9)

For soft-deleted students, the following remain available to ADMIN and STAFF (used by the archived profile UI):

* `GET /students/{id}` — profile
* `GET /students/{id}/photo` — profile photo
* `GET /students/{id}/documents` — active documents
* `GET /students/{id}/documents/{documentId}/download` — download

Mutations (`PUT` fields/photo/docs, `POST` documents, `DELETE` photo/document) still require an **active** (non-deleted) student → soft-deleted → `404`.

---

DELETE

```
/students/{id}
```

*(See Soft delete student above.)*

PATCH

```
/students/{id}/restore
```

*(See Restore student above.)*

GET

```
/students/inactive
```

*(See List inactive / archived students above.)*

---

# Student Documents

Documents are managed through Student APIs.

There are no standalone CRUD APIs for document management.

Milestone 6–7 endpoints:

* `GET /students/{id}/documents` — list
* `GET /students/{id}/documents/{documentId}/download` — download
* `POST /students/{id}/documents` — add
* `PUT /students/{id}/documents/{documentId}` — replace
* `DELETE /students/{id}/documents/{documentId}` — logical removal (Milestone 7 QA, BUG-007)

---

# Dashboard

ADMIN and STAFF (`hasAnyRole('ADMIN', 'STAFF')`). JWT required. No schema / Flyway changes.

Read-only aggregate endpoints for the executive dashboard. Soft-deleted students are included only where noted.

## Summary

`GET /dashboard/summary`

Response `200`:

```json
{
  "totalStudents": 120,
  "activeStudents": 100,
  "inactiveStudents": 20,
  "newAdmissions": 5,
  "maleStudents": 48,
  "femaleStudents": 50,
  "recentAdmissions": [
    {
      "id": "uuid",
      "firstName": "Anita",
      "lastName": "Rao",
      "admissionNumber": "ADM-2026-001",
      "admissionDate": "2026-07-01",
      "updatedDate": "2026-07-01T10:00:00Z"
    }
  ],
  "recentUpdates": [
    {
      "id": "uuid",
      "firstName": "Anita",
      "lastName": "Rao",
      "admissionNumber": "ADM-2026-001",
      "admissionDate": "2026-07-01",
      "updatedDate": "2026-07-10T12:00:00Z"
    }
  ]
}
```

Definitions:

* `activeStudents` — `deleted = false` and `status = ACTIVE`
* `inactiveStudents` — soft-deleted (`deleted = true`); UI label **Left Students**
* `totalStudents` — active + inactive
* `newAdmissions` — students whose `admission_date` falls in the current calendar month (UTC), including later-archived rows
* `maleStudents` / `femaleStudents` — active roster only (`OTHER` is not a summary card)
* `recentAdmissions` — up to 5 active students ordered by `admissionDate` descending
* `recentUpdates` — up to 5 active students ordered by `updatedDate` descending

Empty database returns zeros and empty recent lists (not an error).

## Admissions trend

`GET /dashboard/admissions`

Response `200` — always 12 months (oldest → newest), zero-filled gaps:

```json
[
  { "yearMonth": "2025-08", "count": 2 },
  { "yearMonth": "2025-09", "count": 0 }
]
```

* Counts by `admission_date` month for the last 12 calendar months ending in the current month (UTC)
* Includes soft-deleted students (admission is historical)

## Gender distribution

`GET /dashboard/gender`

Response `200` — one entry per `Gender` enum value (zeros included):

```json
[
  { "gender": "MALE", "count": 48 },
  { "gender": "FEMALE", "count": 50 },
  { "gender": "OTHER", "count": 2 }
]
```

* Active (non-deleted) students only

## Status distribution

`GET /dashboard/status`

Response `200` — one entry per `StudentStatus` (zeros included):

```json
[
  { "status": "ACTIVE", "count": 100 },
  { "status": "INACTIVE", "count": 20 }
]
```

* All retained students (active + soft-deleted). Soft-deleted rows are `INACTIVE`.

---

# Audit Logs

ADMIN only (`hasRole('ADMIN')` at SecurityConfig path matcher and `@PreAuthorize`). JWT required.

Immutable event history for accountability. There is **no** separate `/audit/search` endpoint — list + filters replace the roadmap’s `/search` (same pattern as Users / Students).

Records are append-only: no create/update/delete APIs for clients.

## List audit logs

`GET /audit`

Query parameters (all optional except paging defaults):

| Param | Description |
|---|---|
| `search` | Case-insensitive match on `description` or `username` |
| `module` | `AUTH` \| `STUDENT` \| `DOCUMENT` \| `REPORT` |
| `action` | `LOGIN` \| `LOGOUT` \| `CREATED` \| `UPDATED` \| `DELETED` \| `RESTORED` \| `UPLOADED` \| `REPLACED` \| `GENERATED` |
| `username` | Exact username (case-insensitive) |
| `entityId` | UUID of related entity when present |
| `from` / `to` | Inclusive `created_date` range (ISO-8601 Instant). The Admin UI maps HTML date inputs to local-timezone start/end of day before sending Instants. |
| `page` / `size` / `sort` | Default `createdDate,desc`, size 20. Allowed sort: `createdDate`, `username`, `module`, `action` |

Response `200` — `PageResponse<AuditLogResponse>`:

```json
{
  "content": [
    {
      "id": "…",
      "module": "AUTH",
      "action": "LOGIN",
      "entityId": "…",
      "description": "User logged in via password",
      "username": "admin",
      "ipAddress": "127.0.0.1",
      "createdDate": "2026-07-18T12:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

* `400` — invalid sort property, or `from` after `to`
* `403` — STAFF or non-admin

## Get audit log by id

`GET /audit/{id}`

Response `200` — same shape as a list row.

* `404` — unknown id
* `403` — STAFF or non-admin

## Recorded events (writers)

| Event | Module | Action |
|---|---|---|
| Login (password / Google) | AUTH | LOGIN |
| Logout (authenticated) | AUTH | LOGOUT |
| Student created | STUDENT | CREATED |
| Student updated | STUDENT | UPDATED |
| Student soft-deleted | STUDENT | DELETED |
| Student restored | STUDENT | RESTORED |
| Document uploaded (registration or add) | DOCUMENT | UPLOADED |
| Document replaced | DOCUMENT | REPLACED |
| Report generated (single / selected / filter) | REPORT | GENERATED |

Username and IP are snapshotted at write time (no FK to `users`). Descriptions must not contain passwords, JWTs, or full Aadhaar.

Schema: Flyway `V7__create_audit_logs.sql` (`audit_logs`); immutability trigger in `V8__audit_logs_immutability.sql` (BEFORE UPDATE OR DELETE).

---

# Reports

ADMIN and STAFF (`hasAnyRole('ADMIN', 'STAFF')`). JWT required. No schema / Flyway changes.

PDFs are generated on the backend (OpenPDF). Response body is raw PDF bytes.

**Document handling (Milestone 10):**

* Profile photo embedded when available
* Supporting **image** documents (JPG/JPEG/PNG) embedded inline with caption
* Supporting **PDF** documents listed as references only (type, original file name, uploaded date) — not merged into the report
* Report generation also writes a persistent `audit_logs` row (Milestone 12); structured SLF4J logging remains for ops

Configurable limits (`oms.reports` / env):

* `OMS_REPORTS_ORGANIZATION_NAME` — PDF header organization name (default: Orphanage Management System)
* `OMS_REPORTS_MAX_SELECTED` — max IDs for selected export (default: 50)
* `OMS_REPORTS_MAX_FILTER` — max students for filter export (default: 100)

## Export single student

`GET /reports/student/{studentId}`

* Includes soft-deleted (archived) students
* Response `200` — `application/pdf`
* `Content-Disposition: attachment; filename="student-report-{admissionNumber}.pdf"`
* `404` — student not found

## Export selected students

`POST /reports/students`

Request body:

```json
{
  "studentIds": ["uuid-1", "uuid-2"]
}
```

* At least one ID required; max size enforced in `ReportService` via `max-selected-students` (configurable; not hardcoded Bean Validation)
* Each ID may be active or archived; unknown ID → `404`
* Sorted by `admissionDate` descending in the PDF
* Response `200` — `application/pdf`
* `Content-Disposition: attachment; filename="students-report-{yyyyMMdd-HHmm}.pdf"`
* `400` — empty selection or over limit

## Export filtered students

`POST /reports/filter`

Request body (all fields optional; mirrors list filters, no pagination):

```json
{
  "scope": "ACTIVE | ARCHIVED | ALL",
  "search": "string",
  "gender": "MALE | FEMALE | OTHER",
  "admissionYear": 2024,
  "school": "string",
  "ageMin": 5,
  "ageMax": 18
}
```

* `scope` defaults to `ACTIVE` when omitted
* `ACTIVE` — non-deleted students (same population as `GET /students`)
* `ARCHIVED` — soft-deleted students (same population as `GET /students/inactive`)
* `ALL` — union of active and archived matching the other filters
* Filter predicates for active rows shared with `GET /students` via `StudentSpecifications.buildListSpecification`
* `400` — invalid age range, zero matches, or match count exceeds `max-filter-results`
* Response `200` — same PDF attachment pattern as selected export

### Exact admission number lookup (list API)

`GET /students?admissionNumber={value}`

* Additive query parameter on the existing list endpoint
* Exact, case-insensitive match (admission numbers are unique)
* Soft-deleted rows remain excluded (same as other `GET /students` queries)
* Combinable with pagination; typically `page=0&size=1` for report lookup

### Archived list filters

`GET /students/inactive` accepts the same optional filters as the active list except `status` / `admissionNumber`: `search`, `gender`, `admissionYear`, `school`, `ageMin`, `ageMax`, plus `page` / `size` / `sort`.

---

# Users

Admin only (`ROLE_ADMIN`). JWT required. Soft-disable only — no physical delete.

## List users

`GET /users`

Query parameters:

* `search` — optional; matches username or email (contains, case-insensitive)
* `role` — optional; `ADMIN` or `STAFF`
* `enabled` — optional; `true` or `false`
* `page`, `size`, `sort` — Spring Data pagination (default `size=20`, `sort=username,asc`)

Response `200` — Spring Data page of user objects:

```json
{
  "content": [
    {
      "id": "uuid",
      "username": "staff1",
      "email": "staff1@oms.local",
      "role": "STAFF",
      "enabled": true,
      "authProvider": "LOCAL",
      "accountNonLocked": true,
      "lastLoginAt": null,
      "createdDate": "2026-01-01T00:00:00Z",
      "updatedDate": "2026-01-01T00:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Get user

`GET /users/{id}`

Response `200` — single user object (same shape as list item).

## Create user

`POST /users`

Request

```json
{
  "username": "staff1",
  "email": "staff1@oms.local",
  "role": "STAFF",
  "authProvider": "LOCAL",
  "password": "********"
}
```

* `authProvider` must be `LOCAL` or `GOOGLE`.
* `LOCAL` requires `password` (min 8 characters).
* `GOOGLE` must omit password (pre-provision for Google login by email).

Response `201` — created user object.

## Update user

`PUT /users/{id}`

Request

```json
{
  "username": "staff1",
  "email": "staff1@oms.local",
  "role": "STAFF"
}
```

Does not change password. Cannot demote yourself or demote the last enabled `ADMIN`.

Response `200` — updated user object.

## Disable user

`POST /users/{id}/disable`

Also available as `DELETE /users/{id}` (alias — soft disable, not hard delete).

Revokes all refresh tokens. Cannot disable yourself or the last enabled `ADMIN`.

Response `200` — user with `enabled: false`.

## Enable user

`POST /users/{id}/enable`

Clears lockout counters. Response `200` — user with `enabled: true`.

## Reset password

`POST /users/{id}/reset-password`

Request

```json
{
  "newPassword": "********"
}
```

BCrypt-hashes the password, clears lockout, revokes refresh tokens. If the user was `GOOGLE`-only, upgrades `authProvider` to `LOCAL_GOOGLE`.

Response `200` — updated user object.

---

# Error Response

```
{
  "timestamp":"",
  "status":400,
  "error":"Validation Error",
  "message":"",
  "path":""
}
```

---

# Response Standards

Every API should return:

* HTTP Status
* Message
* Data
* Errors (if any)

---

# Security

JWT Required

Except

* Login
* Google Login
* Refresh Token
* Logout

Role Based Authorization

Admin

Staff

Access tokens are short-lived JWTs (HS256). Refresh tokens are opaque, stored as SHA-256 hashes, and rotated on each use.
