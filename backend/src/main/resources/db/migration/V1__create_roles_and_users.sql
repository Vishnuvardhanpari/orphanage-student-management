CREATE TABLE roles (
    id              UUID         PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    created_date    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id                      UUID          PRIMARY KEY,
    username                VARCHAR(100)  NOT NULL,
    email                   VARCHAR(255)  NOT NULL,
    password_hash           VARCHAR(255),
    auth_provider           VARCHAR(20)   NOT NULL,
    provider_subject        VARCHAR(255),
    role_id                 UUID          NOT NULL,
    enabled                 BOOLEAN       NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN       NOT NULL DEFAULT TRUE,
    failed_login_attempts   INTEGER       NOT NULL DEFAULT 0,
    lock_until              TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    created_date            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_date            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_by              UUID,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider_subject UNIQUE (provider_subject),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'LOCAL_GOOGLE'))
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_provider_subject ON users (provider_subject);
CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_users_enabled ON users (enabled);
