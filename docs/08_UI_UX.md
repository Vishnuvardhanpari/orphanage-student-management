# UI / UX Design

## Design Goals

The application should be:

* Modern
* Responsive
* Clean
* Fast
* Simple
* Easy for non-technical users

---

# Theme

Use Tailwind CSS.

Design Philosophy

- Modern
- Minimal
- Professional
- Clean
- Responsive
- Accessible

Use utility-first styling.

Avoid inline styles.

Create reusable UI components.

Maintain a consistent design system across the application.

Primary Color

Blue

Secondary

White

Accent

Green

Status Colors

Success

Green

Warning

Orange

Error

Red

Inactive Student

Gray

---

# Layout

```
Top Navigation

↓

Sidebar

↓

Content Area

↓

Footer
```

---

# Pages

## Authentication

* Login
* Google Login

---

## Dashboard

Cards

* Total Students
* Active Students
* Left Students
* New Admissions (current UTC calendar month)
* Male Students (active roster)
* Female Students (active roster)

Charts

* Gender distribution
* Status Distribution (Active vs Left)
* Monthly admissions (last 12 UTC months)

Recent activity

* Recent admissions
* Recent updates

Quick Actions

* Register student
* Students list
* Reports
* Archived students

Loading & errors

* Skeleton placeholders while loading
* Header Refresh control
* Error empty state with Retry (page owns error UX; no multi-toast spam)

---

## Student List

Features

* Search
* Filters
* Pagination
* Sorting

Actions

* Add Student
* Edit
* View
* Soft Delete
* Export

---

## Student Form

Single page form.

Suggested sections:

Personal Information

Guardian Information

Education

Medical

Admission

Exit

Student Photo Upload

Supporting Documents Upload

Save

Cancel

Documents should be uploaded directly within this form.

---

## Student Details

Display:

* Student Information
* Student Photo
* Uploaded Documents
* Download Links
* Audit Information

---

## Reports

Dedicated module.

Supports

* Export Single Student
* Export Multiple Students
* Export Filtered Students

Display selected filters before generating PDF.

---

## User Management

Admin Only

* User List
* Add User
* Edit User
* Disable User
* Enable User

---

## Audit Logs

Admin Only

* Audit Log List (search, module/action/username/date filters, pagination)
* Audit Event Detail (immutable read-only view)

---

# Navigation

Sidebar Menu

Dashboard

Students

Reports

Users (Admin Only)

Audit Logs (Admin Only)

Profile

Logout

---

# Form Guidelines

Use Angular Reactive Forms.

Use:

* Required indicators
* Validation messages
* File upload progress
* Preview uploaded images
* PDF icon for PDF files

---

# Accessibility

Keyboard Navigation

Skip link to main content

Responsive Layout (including mobile navigation drawer)

Proper Labels

High Contrast

Focus-visible rings on interactive controls

`prefers-reduced-motion` respected for animations

---

# Design System (Milestone 13)

## Tokens

CSS variables in `frontend/src/styles.css` (`@theme` + `.dark`):

* Primary blue, accent/success green, warning, error, inactive
* Surface / glass / overlay colors
* Dark mode surface + status token overrides

## Shared components

* Button, PageHeader, EmptyState (default/error), ConfirmDialog
* Card (elevated / glass / muted), Skeleton, DialogShell
* Field, Input, Select, Textarea
* StatusBadge, FilterPanel, PaginationBar, ErrorPage (404)
* MobileNavDrawer (CDK dialog from topbar hamburger)

## Patterns

* Glassmorphism on premium surfaces: shell, cards, dialogs, dashboard widgets, filter panels
* Lists (active students, archived students, users, audit) use FilterPanel + PaginationBar
* Page-owned load errors: EmptyState (error/`role=alert`) + Retry; list GETs use `SKIP_ERROR_TOAST`
* StatusBadge on profile/user-detail; AG Grid cells reuse global `.status-badge*` classes
* User form uses Field/Input/Select; student form and report page migration is follow-up
* Theme toggle via ThemeService (`dark` class on `documentElement`)
* AG Grid Quartz receives dark CSS variable overrides; toastr shadow softened in dark mode
* DialogShell / MobileNavDrawer do not nest `role=dialog` (CDK owns dialog a11y)
* FilterPanel root uses `role=search`
* Skeleton uses typed `tall` boolean (not free-form className)

## Manual verification checklist

* Desktop / tablet / mobile layouts; hamburger opens nav and closes on route/Escape
* Keyboard: skip link, focus rings, dialog focus trap; field labels associate with controls
* Light and dark theme on dashboard, lists, dialogs, forms
* Loading skeletons and empty/error states on major pages
* Unknown route shows 404 (not silent redirect to dashboard)
* Archived students filters parity with active list
* Student/user list load failure: EmptyState only (no interceptor toast)

---

# Future UI Improvements

* Dark Mode refinements for third-party widgets (full AG Grid / toastr themes)
* Multi-language Support
* Drag-and-drop file upload
* Further mobile density optimizations
