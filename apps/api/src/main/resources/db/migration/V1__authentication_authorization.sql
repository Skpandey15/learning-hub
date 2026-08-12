CREATE TABLE access_policy (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    shared_study_access BOOLEAN NOT NULL DEFAULT TRUE,
    candidate_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    interviewer_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL
);

INSERT INTO access_policy (id, updated_by) VALUES (1, 'system');

CREATE TABLE security_audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    actor_roles VARCHAR(512) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    source_ip_hash VARCHAR(128) NOT NULL,
    details VARCHAR(1000)
);

CREATE INDEX security_audit_event_occurred_at_idx ON security_audit_event (occurred_at DESC);
CREATE INDEX security_audit_event_actor_id_idx ON security_audit_event (actor_id, occurred_at DESC);
