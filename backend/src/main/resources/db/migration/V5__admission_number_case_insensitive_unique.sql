-- Enforce case-insensitive uniqueness for admission numbers (BUG-002).
-- Soft-deleted rows remain covered by this index (BUG-003).
ALTER TABLE students DROP CONSTRAINT uq_students_admission_number;

CREATE UNIQUE INDEX uq_students_admission_number_lower
    ON students (LOWER(admission_number));
