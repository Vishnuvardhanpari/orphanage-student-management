CREATE TABLE students (
    id                      UUID          PRIMARY KEY,
    admission_number        VARCHAR(50)   NOT NULL,
    first_name              VARCHAR(100)  NOT NULL,
    last_name               VARCHAR(100),
    gender                  VARCHAR(20)   NOT NULL,
    date_of_birth           DATE          NOT NULL,
    blood_group             VARCHAR(10),
    religion                VARCHAR(100),
    nationality             VARCHAR(100),
    aadhaar_number          VARCHAR(12),
    phone_number            VARCHAR(20),
    guardian_name           VARCHAR(100),
    guardian_relationship   VARCHAR(50),
    guardian_phone          VARCHAR(20),
    guardian_address        TEXT,
    school_name             VARCHAR(255),
    standard                VARCHAR(50),
    medium                  VARCHAR(50),
    previous_school         VARCHAR(255),
    medical_conditions      TEXT,
    allergies               TEXT,
    disability              TEXT,
    emergency_notes         TEXT,
    admission_date          DATE          NOT NULL,
    exit_date               DATE,
    exit_reason             TEXT,
    exit_remarks            TEXT,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    profile_photo_path      VARCHAR(500),
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_by              UUID,
    deleted_date            TIMESTAMPTZ,
    created_by              UUID,
    created_date            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by              UUID,
    updated_date            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_students_admission_number UNIQUE (admission_number),
    CONSTRAINT uq_students_aadhaar_number UNIQUE (aadhaar_number),
    CONSTRAINT ck_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_students_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE student_documents (
    id                  UUID          PRIMARY KEY,
    student_id          UUID          NOT NULL,
    document_type       VARCHAR(50)   NOT NULL,
    original_file_name  VARCHAR(255)  NOT NULL,
    stored_file_name    VARCHAR(255)  NOT NULL,
    storage_path        VARCHAR(500)  NOT NULL,
    content_type        VARCHAR(100)  NOT NULL,
    file_size           BIGINT        NOT NULL,
    uploaded_by         UUID,
    uploaded_date       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_date        TIMESTAMPTZ,
    CONSTRAINT fk_student_documents_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT ck_student_documents_document_type CHECK (document_type IN (
        'PHOTOGRAPH',
        'AADHAAR_CARD',
        'BIRTH_CERTIFICATE',
        'MEDICAL_CERTIFICATE',
        'MARK_SHEET',
        'TRANSFER_CERTIFICATE',
        'IDENTITY_PROOF',
        'OTHER'
    ))
);

CREATE INDEX idx_students_admission_number ON students (admission_number);
CREATE INDEX idx_students_first_name ON students (first_name);
CREATE INDEX idx_students_last_name ON students (last_name);
CREATE INDEX idx_students_aadhaar_number ON students (aadhaar_number);
CREATE INDEX idx_students_guardian_name ON students (guardian_name);
CREATE INDEX idx_students_admission_date ON students (admission_date);
CREATE INDEX idx_students_status ON students (status);
CREATE INDEX idx_students_deleted ON students (deleted);
CREATE INDEX idx_students_deleted_status ON students (deleted, status);

CREATE INDEX idx_student_documents_student_id ON student_documents (student_id);
CREATE INDEX idx_student_documents_document_type ON student_documents (document_type);
CREATE INDEX idx_student_documents_deleted ON student_documents (deleted);
