-- Milestone 8 — Student Search
-- Supports the age-range filter, which the backend translates into a
-- date_of_birth range (age itself is never persisted).
CREATE INDEX idx_students_date_of_birth ON students (date_of_birth);
