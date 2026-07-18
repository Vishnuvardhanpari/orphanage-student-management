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

# Navigation

Sidebar Menu

Dashboard

Students

Reports

Users (Admin Only)

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

Responsive Layout

Proper Labels

High Contrast

---

# Future UI Improvements

* Dark Mode refinements
* Multi-language Support
* Drag-and-drop file upload
* Mobile-friendly optimized views
