-- Milestone 12 — Audit Logging
CREATE TABLE audit_logs (
    id              UUID          PRIMARY KEY,
    module          VARCHAR(50)   NOT NULL,
    action          VARCHAR(50)   NOT NULL,
    entity_id       UUID,
    description     TEXT          NOT NULL,
    username        VARCHAR(100)  NOT NULL,
    ip_address      VARCHAR(45),
    created_date    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_audit_logs_module CHECK (module IN ('AUTH', 'STUDENT', 'DOCUMENT', 'REPORT')),
    CONSTRAINT ck_audit_logs_action CHECK (action IN (
        'LOGIN', 'LOGOUT', 'CREATED', 'UPDATED', 'DELETED', 'RESTORED',
        'UPLOADED', 'REPLACED', 'GENERATED'
    ))
);

CREATE INDEX idx_audit_logs_created_date ON audit_logs (created_date DESC);
CREATE INDEX idx_audit_logs_username ON audit_logs (username);
CREATE INDEX idx_audit_logs_module_action ON audit_logs (module, action);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs (entity_id);
