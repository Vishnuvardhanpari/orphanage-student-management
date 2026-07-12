# User Roles

The application supports Role-Based Access Control (RBAC).

There are two roles:

1. Administrator
2. Staff

---

# Administrator

The Administrator has complete access to the system.

## Authentication

- Login using Username & Password
- Login using Google Gmail

## Permissions

### User Management

- Create Staff
- Edit Staff
- Disable Staff
- Enable Staff
- Reset Password
- Assign Roles

### Student Management

- Add Student
- View Student
- Edit Student
- Soft Delete Student
- Restore Student

### Student Documents

- Upload Documents
- Replace Documents
- Delete Documents
- Download Documents
- Preview Documents

### Reports

- Export Single Student
- Export Multiple Students
- Export Filtered Students

### Dashboard

- View Statistics
- View Reports

### Audit

- View Audit Logs
- View Deleted Records

---

# Staff

Staff users are responsible for day-to-day student management.

## Authentication

- Login using Username & Password
- Login using Google Gmail

## Permissions

### Student Management

- Add Student
- Edit Student
- View Student
- Upload Documents
- Replace Documents
- Download Documents
- Preview Documents

### Reports

- Export Single Student
- Export Multiple Students
- Export Filtered Students

### Dashboard

- View Dashboard

---

# Restricted Permissions

Staff cannot:

- Create Users
- Delete Users
- Manage Roles
- Restore Deleted Students
- View Audit Logs
- Change System Settings

---

# Permission Matrix

| Feature | Admin | Staff |
|----------|:-----:|:-----:|
| Login | ✅ | ✅ |
| Google Login | ✅ | ✅ |
| Dashboard | ✅ | ✅ |
| Add Student | ✅ | ✅ |
| Edit Student | ✅ | ✅ |
| View Student | ✅ | ✅ |
| Upload Documents | ✅ | ✅ |
| Replace Documents | ✅ | ✅ |
| Download Documents | ✅ | ✅ |
| Export Reports | ✅ | ✅ |
| Soft Delete Student | ✅ | ✅ |
| Restore Student | ✅ | ❌ |
| User Management | ✅ | ❌ |
| Audit Logs | ✅ | ❌ |
| System Settings | ✅ | ❌ |

---

# Security Principles

- Every request must be authenticated.
- Every API must be authorized based on role.
- JWT tokens shall be validated for every secured request.
- Passwords shall never be stored in plain text.
- BCrypt hashing shall be used for password storage.
- All communication shall use HTTPS.
- User actions shall be recorded in audit logs.