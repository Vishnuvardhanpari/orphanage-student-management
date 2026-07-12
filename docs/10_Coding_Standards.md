# Coding Standards

# Objective

The purpose of this document is to establish coding standards and best practices that ensure consistency, maintainability, readability, security, and production readiness throughout the project.

Every AI-generated and manually written code must comply with these standards.

---

# General Principles

Follow

- SOLID Principles
- Clean Code
- DRY
- KISS
- YAGNI
- Separation of Concerns
- Layered Architecture

Always prioritize readability over clever code.

---

# Technology Versions

Java

21

Spring Boot

3.x

Angular

Latest Stable

PostgreSQL

Latest Stable

Flyway

Latest Stable

---

# Backend Architecture

Follow Layered Architecture

```
Controller

↓

Service

↓

Repository

↓

Database
```

Never skip layers.

---

# Package Structure

```
controller

service

repository

entity

dto

mapper

config

security

exception

util

audit

report

user

student
```

Group classes by feature, not by technical layer where appropriate.

---

# Naming Conventions

Classes

PascalCase

```
StudentController
```

Variables

camelCase

```
studentName
```

Constants

UPPER_SNAKE_CASE

```
MAX_UPLOAD_SIZE
```

Methods

camelCase

```
saveStudent()
```

Packages

lowercase

```
student
```

---

# Dependency Injection

Always use

Constructor Injection

Never use

```
@Autowired
```

on fields.

---

# Controllers

Responsibilities

- Receive Request
- Validate Input
- Call Service
- Return Response

Controllers must never contain business logic.

---

# Services

Business Logic belongs here.

Responsibilities

- Validation
- Processing
- Transactions
- Calling repositories

---

# Repositories

Only Database Operations.

No business logic.

---

# DTO Pattern

Never expose JPA entities.

Always use

Request DTO

Response DTO

Mapper

MapStruct

---

# Validation

Use

Bean Validation

Examples

```
@NotNull

@NotBlank

@Size

@Email

@Pattern
```

Validation belongs in both

Frontend

Backend

Backend validation is mandatory.

---

# Exception Handling

Use

Global Exception Handler

```
@ControllerAdvice
```

Never use

try-catch

inside controllers unless absolutely necessary.

---

# Logging

Use

SLF4J

Never use

```
System.out.println()
```

Log Levels

INFO

WARN

ERROR

DEBUG

Never log

Passwords

JWT

Personal Sensitive Information

---

# Database

Use

Spring Data JPA

Flyway

Soft Delete

UUID Primary Keys

Indexes

Audit Fields

Never use

DDL Auto Update

```
spring.jpa.hibernate.ddl-auto=update
```

Production should use

Flyway Migrations

---

# Soft Delete

Never physically delete students.

Use

deleted

deletedDate

deletedBy

Status

INACTIVE

---

# Transactions

Use

```
@Transactional
```

Only in Service Layer.

---

# API Standards

REST APIs

Plural Resource Names

Examples

```
/students

/users

/reports
```

Use

HTTP Status Codes

200

201

204

400

401

403

404

500

---

# Security

Spring Security

JWT

Google OAuth

BCrypt

Never expose stack traces to clients.

---

# Angular Standards

Use

Standalone Components (if using Angular 17+)

Reactive Forms

Lazy Loading

Feature Modules (where appropriate)

Services

Interceptors

Guards

Resolvers (if needed)

---

# Angular Signals

Use Angular Signals for reactive UI and service state where appropriate.

## computed() dependency rule

`computed()` only tracks **signal** reads.

Never mix signal reads with non-signal reads inside `computed()` when the result must be correct on every call (especially route guards, interceptors, and auth checks).

Non-signal examples that must not be the sole freshness source inside `computed()`:

* `sessionStorage` / `localStorage`
* Plain class fields or getters
* Arbitrary method calls that do not read signals

### Bad

```typescript
// getAccessToken() reads sessionStorage — not tracked by computed()
readonly isAuthenticated = computed(
  () => !!this.tokenStorage.getAccessToken() && !!this.currentUserSignal()
);
```

This can leave a memoized / stale value (e.g. `false` after login) so `authGuard` blocks navigation even though tokens exist in storage.

### Good

Prefer a normal method when any input is non-reactive:

```typescript
isAuthenticated(): boolean {
  return !!this.tokenStorage.getAccessToken() && !!this.currentUserSignal();
}
```

Or make **all** dependencies signals, then `computed()` is safe:

```typescript
readonly accessToken = signal<string | null>(null);
readonly isAuthenticated = computed(
  () => !!this.accessToken() && !!this.currentUserSignal()
);
```

### Rule of thumb

* Guards / interceptors / “ask every time” checks → method (or fully signal-based state)
* Derived UI state from signals only → `computed()` is fine

---

# Angular Folder Structure

```
core/

shared/

layout/

auth/

dashboard/

student/

reports/

users/
```

---

# UI Guidelines

Tailwind CSS

Create reusable UI components.

Use reusable utility classes.

Avoid duplicated Tailwind utilities.

Prefer reusable component classes using @apply where appropriate.

Maintain consistent spacing, typography, colors, and border radius throughout the application.

Responsive Design

Accessibility

Dark Mode Ready

Consistent Colors

Reusable Tailwind Components

---

# File Upload

Use Multipart Upload

Validate

File Type

File Size

Content Type

Never trust client validation.

---

# PDF Generation

Generate on Backend.

Never generate PDFs inside Angular.

---

# Documentation

Every

Public Class

Public Method

Complex Logic

Must include JavaDoc.

---

# Testing

Every Service

Unit Test

Every Controller

Integration Test

Every Bug

Regression Test

---

# Git Standards

Branch Naming

```
feature/student-module

feature/pdf-export

bugfix/login

hotfix/security
```

Commit Message Format

```
feat:

fix:

refactor:

docs:

test:

chore:
```

Examples

```
feat: Add student registration module

fix: Resolve JWT expiration issue

refactor: Optimize student search query
```

---

# Code Review Checklist

Before every commit verify

- Code Compiles
- Tests Pass
- No Warnings
- No Dead Code
- No Duplicate Logic
- Proper Logging
- Proper Validation
- Secure APIs
- DTO Used
- Flyway Updated
- Documentation Updated

---

# Performance Guidelines

Always

Use Pagination

Use Database Indexes

Avoid N+1 Queries

Use Fetch Joins when needed

Optimize SQL

Avoid unnecessary API calls

---

# Future Standards

Caching

Redis

Messaging

RabbitMQ

Observability

Micrometer

Prometheus

Grafana

Containerization

Docker

Kubernetes (Future)