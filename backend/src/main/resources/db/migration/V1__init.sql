-- Initial schema. See docs/database.md for the ERD and the reasoning behind each table.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_users_active_has_password CHECK (status = 'PENDING' OR password_hash IS NOT NULL)
);

-- Append-only log of every login attempt (successful or not).
CREATE TABLE login_history (
    id              UUID         PRIMARY KEY,
    user_id         UUID         REFERENCES users (id),
    email_attempted VARCHAR(255) NOT NULL,
    success         BOOLEAN      NOT NULL,
    failure_reason  VARCHAR(30),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_login_history_reason CHECK (
        (success AND failure_reason IS NULL)
        OR (NOT success AND failure_reason IN
            ('INVALID_CREDENTIALS', 'ACCOUNT_PENDING', 'ACCOUNT_INACTIVE', 'WRONG_PORTAL', 'RATE_LIMITED'))
    )
);
CREATE INDEX idx_login_history_user_created ON login_history (user_id, created_at DESC);
CREATE INDEX idx_login_history_created ON login_history (created_at DESC);

-- One-time invite and password-reset tokens. Only the SHA-256 hash is stored.
CREATE TABLE account_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    type       VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_account_tokens_type CHECK (type IN ('INVITE', 'RESET'))
);
CREATE INDEX idx_account_tokens_user ON account_tokens (user_id);

-- Refresh tokens (rotated on every use). Only the SHA-256 hash is stored.
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
