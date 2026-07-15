# Development Roadmap

# Orphanage Management System (OMS)

## Overview

This roadmap defines the implementation order for the project.

Each milestone should be fully completed, reviewed, tested, documented, and committed before moving to the next milestone.

**Do not work on multiple milestones simultaneously unless there is a dependency.**

Development methodology:

* Agile
* Feature Driven Development (FDD)
* Test Early
* Continuous Refactoring
* Continuous Documentation

---

# Development Lifecycle

Every milestone must follow this sequence.

```
Requirements
        ↓
Architecture
        ↓
Database
        ↓
Backend
        ↓
Frontend
        ↓
Testing
        ↓
Documentation
        ↓
Code Review
        ↓
Git Commit
```

---

# Milestone 1 — Project Initialization

## Objective

Create the project foundation.

### Backend

* Create Spring Boot project
* Configure Java 21
* Configure Maven
* Configure Profiles
* Configure Logging
* Configure Flyway
* Configure PostgreSQL
* Configure Docker
* Configure OpenAPI
* Configure Global Exception Handling

### Frontend

* Create Angular project
* Install Tailwind CSS
* Install Lucide Angular
* Install AG Grid Community
* Install Apache ECharts
* Install Angular CDK
* Install ngx-toastr
* Configure Routing
* Configure Folder Structure
* Configure Environment Files

### Deliverables

* Application starts successfully
* Angular runs successfully
* Spring Boot connects to PostgreSQL

---

# Milestone 2 — Authentication

## Objective

Implement secure authentication.

### Backend

* Spring Security
* JWT
* Google OAuth2
* User Entity
* Role Entity
* Login API
* Refresh Token (Optional)
* Password Encryption

### Frontend

* Login Page
* Google Login
* JWT Storage
* Route Guards
* HTTP Interceptor

### Deliverables

* User can login
* JWT generated
* Protected routes working

---

# Milestone 3 — User Management

## Objective

Allow administrators to manage users.

### Backend

* CRUD APIs
* Role Assignment
* Enable/Disable User

### Frontend

* User List
* User Form
* User Details

### Deliverables

* Admin can manage staff accounts

---

# Milestone 4 — Student Database Design

## Objective

Design the student data model.

### Tasks

* Create Student Entity
* Create Student Document Entity
* Create Flyway Migration
* Create Indexes
* Configure Relationships

### Deliverables

* Database schema completed

---

# Milestone 5 — Student Registration

## Objective

Implement complete student registration.

### Backend

* Student CRUD
* DTOs
* Validation
* Mapper
* Service
* Controller

### Frontend

* Student Registration Form
* Validation
* Image Upload
* Document Upload

### Deliverables

* Student can be created successfully

---

# Milestone 6 — Student Profile

## Objective

Display complete student information.

---

## Features

- View Student
- Student Photo
- Uploaded Documents
- Download Documents

---

## Backend Deliverables

### APIs

- GET /api/v1/students/{id}
- GET /api/v1/students/{id}/documents
- GET /api/v1/students/{id}/documents/{documentId}/download

### Business Logic

- Retrieve student details
- Retrieve active documents only
- Download document
- Return appropriate error responses
- Authorization checks

---

## Frontend Deliverables

- Student Profile Page
- Student Details Card
- Student Photo
- Documents Section
- Download Button

---

## Testing Deliverables

- Unit Tests
- Integration Tests
- API Tests
- UI Tests

---

## Deliverables

- Complete student profile page
- Student profile APIs
- Document download working

---

# Milestone 7 — Student Update

## Objective

Allow editing of student records.

### Features

* Update Information
* Replace Photo
* Upload Additional Documents
* Replace Existing Documents

## Backend Deliverables

### APIs

- PUT /api/v1/students/{id}
- PUT /api/v1/students/{id}/photo
- POST /api/v1/students/{id}/documents
- PUT /api/v1/students/{id}/documents/{documentId}

### Business Logic

- Update student
- Replace photo
- Upload additional documents
- Replace document
- Validation
- Audit

## Frontend Deliverables

- Edit Student Page
- Edit Form
- Replace Photo
- Upload Documents

## Testing Deliverables

- Update Tests
- Validation Tests
- Upload Tests

## Deliverables

- Student editing completed

---

# Milestone 8 — Student Search

## Objective

Implement enterprise search.

### Features

* Global Search
* Server-side Pagination
* Sorting
* Filters

### Filters

* Gender
* Status
* Admission Year
* School
* Age

## Backend Deliverables

### APIs

- GET /api/v1/students (single list/search endpoint; no separate `/students/search`)

### Query Parameters

- page
- size
- sort
- search
- gender
- status
- admissionYear
- school
- ageMin
- ageMax

### Business Logic

- Pagination
- Sorting
- Filtering
- Search
- Specification Query
- Optimized Database Queries

## Frontend Deliverables

- Student Listing
- AG Grid
- Search Bar
- Filter Panel
- Pagination
- Sorting

## Testing Deliverables

- Search Tests
- Pagination Tests
- Filter Tests
- Performance Tests

## Deliverables

- Enterprise Search
- Server-side Pagination
- Dynamic Filters

---

# Milestone 9 — Student Soft Delete & Restore

## Objective

Implement historical record management while preserving data integrity through soft deletion.

---

## Features

* Soft Delete Student
* Restore Student
* View Inactive Students
* Exclude Deleted Students from Active Searches
* Maintain Historical Data

---

## Backend Deliverables

### APIs

* DELETE `/api/v1/students/{id}` (Soft Delete)
* PATCH `/api/v1/students/{id}/restore`
* GET `/api/v1/students/inactive`
* GET `/api/v1/students?status=ACTIVE|INACTIVE`

### Business Logic

* Soft delete using `deleted=true`
* Populate deleted date/time
* Populate deleted by
* Restore deleted student
* Prevent duplicate restores
* Filter active/inactive records
* Validation and authorization
* Audit logging

---

## Frontend Deliverables

* Delete Confirmation Dialog
* Restore Confirmation Dialog
* Inactive Student Listing
* Status Badge
* Active/Inactive Filter
* Toast Notifications

---

## Database Changes

No schema changes expected.

Use existing audit and soft delete columns.

---

## Testing Deliverables

* Unit Tests
* Integration Tests
* API Tests
* UI Tests
* Regression Tests
* Authorization Tests

---

## Definition of Done

* Student can be soft deleted.
* Student can be restored.
* Deleted records remain in database.
* Active searches exclude deleted students.
* Inactive view displays deleted students.
* Audit information recorded.

---

## Deliverables

* Historical records preserved
* Soft delete functionality completed
* Restore functionality completed

---

# Milestone 10 — Reports & PDF Export

## Objective

Generate professional PDF reports for students.

---

## Features

* Export Single Student
* Export Multiple Students
* Export Filtered Students
* Download PDF

---

## PDF Contents

* Student Photo
* Student Information
* Uploaded Documents
* Generated Date
* Generated By
* Organization Header
* Page Numbers

---

## Backend Deliverables

### APIs

* GET `/api/v1/reports/student/{id}`
* POST `/api/v1/reports/students`
* POST `/api/v1/reports/filter`

### Business Logic

* Generate PDF
* Merge student information
* Embed images
* Embed uploaded documents
* Handle large exports
* Audit report generation

---

## Frontend Deliverables

* Export Dialog
* Student Selection
* Filter Selection
* Export Progress Indicator
* Download Handling

---

## Database Changes

No schema changes expected.

---

## Testing Deliverables

* PDF Generation Tests
* API Tests
* UI Tests
* Performance Tests
* Large Dataset Tests

---

## Definition of Done

* Single student export works.
* Multiple student export works.
* Filter export works.
* Images included.
* Documents included.
* PDF layout consistent.

---

## Deliverables

* Professional PDF generation
* Report export module completed

---

# Milestone 11 — Dashboard

## Objective

Provide an executive dashboard for quick insights.

---

## Statistics

* Total Students
* Active Students
* Inactive Students
* New Admissions
* Male Students
* Female Students

---

## Charts

Using Apache ECharts

* Student Distribution
* Gender Distribution
* Admission Trends
* Status Distribution
* Monthly Admissions

---

## Backend Deliverables

### APIs

* GET `/api/v1/dashboard/summary`
* GET `/api/v1/dashboard/admissions`
* GET `/api/v1/dashboard/gender`
* GET `/api/v1/dashboard/status`

### Business Logic

* Aggregate student statistics
* Monthly trend calculations
* Dashboard summaries
* Optimized database queries

---

## Frontend Deliverables

* Dashboard Page
* Statistics Cards
* ECharts Integration
* Responsive Layout
* Loading Skeletons
* Refresh Support

---

## Database Changes

No schema changes expected.

---

## Testing Deliverables

* API Tests
* Dashboard UI Tests
* Performance Tests
* Chart Rendering Tests

---

## Definition of Done

* Dashboard loads successfully.
* Statistics are accurate.
* Charts display correctly.
* Responsive across devices.

---

## Deliverables

* Executive dashboard completed

---

# Milestone 12 — Audit Logging

## Objective

Track all important business operations for accountability and traceability.

---

## Logged Events

* Login
* Logout
* Student Created
* Student Updated
* Student Deleted
* Student Restored
* Report Generated
* Document Uploaded
* Document Replaced

---

## Backend Deliverables

### APIs

* GET `/api/v1/audit`
* GET `/api/v1/audit/{id}`
* GET `/api/v1/audit/search`

### Business Logic

* Record audit events
* Capture user details
* Capture timestamps
* Capture operation type
* Search audit history
* Filter audit logs

---

## Frontend Deliverables

* Audit Log Page
* Audit Table
* Search
* Filters
* Pagination
* Detail View

---

## Database Changes

Create Audit Log table.

Store:

* User
* Action
* Entity
* Entity ID
* Timestamp
* Description

---

## Testing Deliverables

* Audit API Tests
* UI Tests
* Security Tests
* Performance Tests

---

## Definition of Done

* Every configured action generates an audit record.
* Audit history searchable.
* Audit records immutable.

---

## Deliverables

* Audit logging completed

---

# Milestone 13 — UI Polish

## Objective

Refine the application into a production-quality user experience.

---

## Features

* Empty States
* Loading Skeletons
* Responsive Layout
* Accessibility Improvements
* Better Animations
* Dark Mode Preparation
* Consistent Spacing
* Improved Typography

---

## Frontend Deliverables

* Skeleton Components
* Empty State Components
* Error Pages
* Responsive Layout Refinements
* Accessibility Enhancements
* Theme Preparation
* Reusable UI Components
* Animation Improvements

---

## Backend Deliverables

No functional backend changes expected.

Only minor API adjustments if required by UI improvements.

---

## Database Changes

None.

---

## Testing Deliverables

* Responsive Tests
* Accessibility Tests
* Browser Compatibility Tests
* UI Regression Tests
* Performance Tests

---

## Definition of Done

* UI is responsive.
* Accessibility checks pass.
* Consistent design system.
* Smooth user experience.
* Production-ready appearance.

---

## Deliverables

* Enterprise-quality UI
* Responsive experience
* Accessibility improvements
* Production-ready user interface


---

# Milestone 14 — Testing

## Backend

* Unit Tests
* Integration Tests

## Frontend

* Component Tests
* E2E Tests

## Manual Testing

* Authentication
* Student CRUD
* Reports
* Upload
* Download

### Deliverables

* 80%+ test coverage
* Critical flows verified

---

# Milestone 15 — Production Deployment

## Backend

Deploy to Google Cloud Run

## Frontend

Deploy to Google Cloud Storage

## Database

Supabase PostgreSQL

## Storage

Google Cloud Storage

### Configure

* Environment Variables
* Secrets
* HTTPS
* Logging
* Monitoring

### Deliverables

* Production environment live

---

# Milestone 16 — Production Validation

## Verify

* Login
* CRUD
* Search
* Filters
* Reports
* Uploads
* Downloads
* Authorization
* Logging
* Performance

### Deliverables

* Application accepted for production

---

# Git Workflow

Every milestone should use a dedicated feature branch.

Example

```
feature/authentication

feature/student-registration

feature/student-search

feature/pdf-export

feature/dashboard
```

Merge into `main` only after:

* Code Review
* Tests Pass
* Documentation Updated

---

# Definition of Done

A milestone is complete only when:

* Requirements implemented
* Backend complete
* Frontend complete
* Validation complete
* Tests passing
* Documentation updated
* Manual testing completed
* Code reviewed
* No critical bugs
* Git committed

---

# Cursor Development Instructions

Before starting any milestone:

1. Read `docs/12_AI_Context.md`.
2. Read the relevant documentation in the `docs/` folder.
3. Explain the implementation plan.
4. Identify affected modules.
5. Recommend the best approach.
6. Implement incrementally.
7. Generate tests.
8. Review the code.
9. Update documentation if necessary.

Never generate an entire application in one response.

Always complete one milestone before moving to the next.

---

# Future Enhancements

The architecture should support future modules without major redesign:

* Attendance Management
* Visitor Management
* Inventory Management
* Donation Management
* Sponsor Management
* Medical History
* Education History
* Fee Management (if applicable)
* Notifications (Email/SMS)
* Mobile Application
* Analytics Dashboard
* Multi-Orphanage Support
* Multi-language Support
* Backup & Restore Dashboard
