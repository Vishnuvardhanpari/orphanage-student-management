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

Authorization: JWT required. `ADMIN` or `STAFF` may register students, view profiles, list documents, and download files.

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

Soft-deleted or unknown students → `404`.

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

GET

```
/students
```

Supports

* Pagination
* Sorting
* Filtering
*(Milestone 8)*

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

Supports

* Update Student
* Replace Documents
* Upload Additional Documents
*(Milestone 7)*

DELETE

```
/students/{id}
```

Soft Delete
*(Milestone 9)*

PATCH

```
/students/{id}/restore
```

Admin Only
*(Milestone 9)*

---

# Student Documents

Documents are managed through Student APIs.

There are no standalone CRUD APIs for document management.

Milestone 6 endpoints:

* `GET /students/{id}/documents` — list
* `GET /students/{id}/documents/{documentId}/download` — download

Later milestones:

DELETE

```
/students/{id}/documents/{documentId}
```

Logical document removal.
*(Milestone 7+)*

---

# Reports

GET

```
/reports/student/{studentId}
```

Generate Single Student PDF

POST

```
/reports/students
```

Generate PDF for selected students.

POST

```
/reports/filter
```

Generate PDF using filters.

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
