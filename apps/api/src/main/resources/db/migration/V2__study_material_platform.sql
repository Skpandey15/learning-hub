CREATE TABLE learning_domain (
    id UUID PRIMARY KEY, slug VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL, display_order INTEGER NOT NULL CHECK (display_order > 0),
    lifecycle_status VARCHAR(20) NOT NULL CHECK (lifecycle_status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) NOT NULL, updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX learning_domain_order_uq ON learning_domain(display_order) WHERE lifecycle_status <> 'ARCHIVED';

CREATE TABLE learning_technology (
    id UUID PRIMARY KEY, domain_id UUID NOT NULL REFERENCES learning_domain(id),
    slug VARCHAR(80) NOT NULL, name VARCHAR(120) NOT NULL, description TEXT NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    lifecycle_status VARCHAR(20) NOT NULL CHECK (lifecycle_status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) NOT NULL, updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(domain_id, slug)
);
CREATE UNIQUE INDEX learning_technology_order_uq ON learning_technology(domain_id, display_order) WHERE lifecycle_status <> 'ARCHIVED';

CREATE TABLE learning_topic (
    id UUID PRIMARY KEY, technology_id UUID NOT NULL REFERENCES learning_technology(id),
    slug VARCHAR(100) NOT NULL, title VARCHAR(180) NOT NULL, summary TEXT NOT NULL,
    skill_level VARCHAR(20) NOT NULL CHECK (skill_level IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes BETWEEN 5 AND 1440),
    objectives JSONB NOT NULL CHECK (jsonb_typeof(objectives) = 'array'),
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    lifecycle_status VARCHAR(20) NOT NULL CHECK (lifecycle_status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    active BOOLEAN NOT NULL DEFAULT TRUE, current_content_version_id UUID,
    version BIGINT NOT NULL DEFAULT 0, created_by VARCHAR(255) NOT NULL, updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(technology_id, slug)
);
CREATE UNIQUE INDEX learning_topic_order_uq ON learning_topic(technology_id, display_order) WHERE lifecycle_status <> 'ARCHIVED';
CREATE INDEX learning_topic_catalog_idx ON learning_topic(technology_id, lifecycle_status, active, skill_level, display_order);

CREATE TABLE learning_topic_prerequisite (
    topic_id UUID NOT NULL REFERENCES learning_topic(id), prerequisite_topic_id UUID NOT NULL REFERENCES learning_topic(id),
    PRIMARY KEY(topic_id, prerequisite_topic_id), CHECK(topic_id <> prerequisite_topic_id)
);

CREATE TABLE learning_path (
    id UUID PRIMARY KEY, slug VARCHAR(100) NOT NULL UNIQUE, name VARCHAR(180) NOT NULL, description TEXT NOT NULL,
    skill_level VARCHAR(20) NOT NULL CHECK (skill_level IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    lifecycle_status VARCHAR(20) NOT NULL CHECK (lifecycle_status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) NOT NULL, updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE learning_path_topic (
    path_id UUID NOT NULL REFERENCES learning_path(id), topic_id UUID NOT NULL REFERENCES learning_topic(id),
    position INTEGER NOT NULL CHECK(position > 0), PRIMARY KEY(path_id, topic_id), UNIQUE(path_id, position)
);

CREATE TABLE study_content_version (
    id UUID PRIMARY KEY, topic_id UUID NOT NULL REFERENCES learning_topic(id), version_number INTEGER NOT NULL CHECK(version_number > 0),
    status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED','SUPERSEDED','REJECTED')),
    title VARCHAR(180) NOT NULL, introduction TEXT NOT NULL, conclusion TEXT NOT NULL,
    model_name VARCHAR(120) NOT NULL, prompt_version VARCHAR(40) NOT NULL, content_hash CHAR(64) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, reviewed_by VARCHAR(255), publication_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL, UNIQUE(topic_id, version_number)
);
CREATE UNIQUE INDEX one_published_content_per_topic ON study_content_version(topic_id) WHERE status = 'PUBLISHED';

CREATE TABLE study_unit (
    id UUID PRIMARY KEY, content_version_id UUID NOT NULL REFERENCES study_content_version(id), stable_key VARCHAR(100) NOT NULL,
    unit_type VARCHAR(20) NOT NULL CHECK(unit_type IN ('OVERVIEW','THEORY','EXAMPLE','EXERCISE','SUMMARY')),
    title VARCHAR(180) NOT NULL, body_markdown TEXT NOT NULL, code_language VARCHAR(40), code_example TEXT,
    key_takeaways JSONB NOT NULL CHECK(jsonb_typeof(key_takeaways) = 'array'), display_order INTEGER NOT NULL CHECK(display_order > 0),
    estimated_minutes INTEGER NOT NULL CHECK(estimated_minutes > 0), created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(content_version_id, stable_key), UNIQUE(content_version_id, display_order)
);

CREATE TABLE content_generation_job (
    id UUID PRIMARY KEY, topic_id UUID NOT NULL REFERENCES learning_topic(id), requested_by_subject VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK(status IN ('QUEUED','GENERATING','VALIDATING','AWAITING_REVIEW','PUBLISHED','REJECTED','FAILED')),
    requested_at TIMESTAMPTZ NOT NULL, started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, heartbeat_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0, prompt_version VARCHAR(40) NOT NULL, model_name VARCHAR(120) NOT NULL,
    result_version_id UUID REFERENCES study_content_version(id), error_code VARCHAR(80), error_message VARCHAR(500),
    correlation_id VARCHAR(128) NOT NULL, idempotency_key VARCHAR(128) NOT NULL, UNIQUE(requested_by_subject, idempotency_key)
);
CREATE UNIQUE INDEX one_active_generation_per_topic ON content_generation_job(topic_id)
    WHERE status IN ('QUEUED','GENERATING','VALIDATING');
CREATE INDEX generation_monitoring_idx ON content_generation_job(status, requested_at);

ALTER TABLE learning_topic ADD CONSTRAINT learning_topic_current_content_fk
    FOREIGN KEY(current_content_version_id) REFERENCES study_content_version(id);

CREATE TABLE user_unit_progress (
    user_subject VARCHAR(255) NOT NULL, unit_id UUID NOT NULL REFERENCES study_unit(id), completed BOOLEAN NOT NULL,
    completed_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(user_subject, unit_id)
);
CREATE INDEX user_unit_progress_subject_idx ON user_unit_progress(user_subject, updated_at DESC);

CREATE TABLE admin_audit_event (
    id UUID PRIMARY KEY, occurred_at TIMESTAMPTZ NOT NULL, actor_subject VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL, target_type VARCHAR(80) NOT NULL, target_id UUID,
    correlation_id VARCHAR(128) NOT NULL, outcome VARCHAR(30) NOT NULL, reason VARCHAR(500), details JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX admin_audit_event_time_idx ON admin_audit_event(occurred_at DESC);
