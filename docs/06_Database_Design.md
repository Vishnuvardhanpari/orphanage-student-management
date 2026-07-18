# Database Design

## Database

PostgreSQL

---

# Design Principles

* Third Normal Form (3NF)
* UUID Primary Keys
* Audit Fields
* Soft Delete
* Foreign Key Constraints
* Indexed Search Columns

---

# Tables

## users

Stores application users.

Fields

* id
* username
* email
* password
* auth_provider
* enabled
* role_id
* created_date
* updated_date

---

## roles

Stores system roles.

Default Roles

* ADMIN
* STAFF

---

## students

Stores the primary student information.

Fields

* id
* admission_number
* first_name
* last_name
* gender
* date_of_birth
* blood_group
* religion
* nationality
* aadhaar_number
* phone_number

Guardian Information

* guardian_name
* guardian_relationship
* guardian_phone
* guardian_address

Education

* school_name
* standard
* medium
* previous_school

Medical

* medical_conditions
* allergies
* disability
* emergency_notes

Admission

* admission_date

Exit

* exit_date
* exit_reason
* exit_remarks

Student Status

* ACTIVE
* INACTIVE

Photo

* profile_photo_path

Soft Delete

* deleted
* deleted_by
* deleted_date

Audit

* created_by
* created_date
* updated_by
* updated_date

---

## student_documents

Stores metadata for uploaded documents.

Actual files remain in Google Cloud Storage.

Fields

* id
* student_id
* document_type
* original_file_name
* stored_file_name
* storage_path
* content_type
* file_size
* uploaded_by
* uploaded_date

Soft Delete

* deleted
* deleted_date

---

## audit_logs

Stores important application events.

Fields

* id
* module
* action
* entity_id
* description
* username
* ip_address
* created_date

Append-only: Flyway `V8__audit_logs_immutability.sql` installs a `BEFORE UPDATE OR DELETE` row trigger that raises an exception. Application APIs never mutate audit rows.

---

# Relationships

roles

↓

users

students

↓

student_documents

users

↓

audit_logs

---

# Indexes

Create indexes for:

* admission_number
* first_name
* last_name
* aadhaar_number
* guardian_name
* admission_date
* date_of_birth (Flyway `V6__add_students_search_indexes.sql`; supports the age-range filter translated into a date_of_birth range)
* status
* deleted
* (deleted, status) composite for default list queries

Document Table

* student_id
* document_type
* deleted

Audit Table

* created_date
* username

---

# Constraints

Admission Number

UNIQUE on `LOWER(admission_number)` (case-insensitive; Flyway `V5__admission_number_case_insensitive_unique.sql`)

Aadhaar Number

UNIQUE (Nullable)

Student Documents

Foreign Key

student_id

ON DELETE RESTRICT

---

# Soft Delete Strategy

Never physically delete students.

Only update:

deleted = true

deleted_date

deleted_by

---

# Future Tables

* attendance
* education_history
* medical_history
* sponsors
* donations
* notifications
