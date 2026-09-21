-- Insert-only: nothing in this codebase ever updates or deletes a row here.
CREATE TABLE audit_logs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT      NOT NULL,
    user_id          BIGINT,
    action           VARCHAR(100) NOT NULL,
    entity           VARCHAR(100) NOT NULL,
    entity_id        VARCHAR(100),
    old_value        JSON,
    new_value        JSON,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE INDEX idx_audit_logs_organization_id ON audit_logs (organization_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity, entity_id);
