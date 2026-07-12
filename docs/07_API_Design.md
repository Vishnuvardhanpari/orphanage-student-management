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

GET

```
/students
```

Supports

* Pagination
* Sorting
* Filtering

GET

```
/students/{id}
```

POST

```
/students
```

Supports

* Student Details
* Student Photo Upload
* Multiple Document Upload

PUT

```
/students/{id}
```

Supports

* Update Student
* Replace Documents
* Upload Additional Documents

DELETE

```
/students/{id}
```

Soft Delete

PATCH

```
/students/{id}/restore
```

Admin Only

---

# Student Documents

Documents are managed through Student APIs.

There are no standalone CRUD APIs for document management.

Optional endpoints

GET

```
/students/{id}/documents/{documentId}
```

Download

DELETE

```
/students/{id}/documents/{documentId}
```

Logical document removal.

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
