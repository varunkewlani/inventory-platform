CREATE TABLE organizations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Email is globally unique (not scoped per organization) so that login can
-- resolve a user from email alone, with no separate org selector step.
-- See ADR-005 / SECURITY.md for the trade-off this makes against the
-- originally-sketched UNIQUE(organization_id, email).
CREATE TABLE users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT       NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE INDEX idx_users_organization_id ON users (organization_id);

CREATE TABLE refresh_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    device_info  VARCHAR(255),
    expires_at   TIMESTAMP    NOT NULL,
    revoked_at   TIMESTAMP    NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
