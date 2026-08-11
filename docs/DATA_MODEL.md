# Learning Hub V1 — Data Model

## 1. Conventions

- PostgreSQL 18.4 baseline and Flyway own schema evolution; upgrades follow `DEPENDENCY_BASELINE.md`.
- Primary keys are UUIDs generated server-side.
- Timestamps are `timestamptz` in UTC.
- Slugs are lowercase, URL-safe, and unique within their parent.
- Ordered children use positive integer `display_order` values.
- Catalog rows use `active`; published content versions are immutable.
- User identity is the Keycloak `sub` claim stored as `varchar(255)`.

## 2. Enumerations

Use PostgreSQL check constraints or application enums consistently:

- `skill_level`: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- `unit_type`: `OVERVIEW`, `THEORY`, `EXAMPLE`, `EXERCISE`, `SUMMARY`
- `content_status`: `DRAFT`, `PUBLISHED`, `SUPERSEDED`
- `job_status`: `QUEUED`, `GENERATING`, `VALIDATING`, `PUBLISHED`, `FAILED`

## 3. Catalog tables

### `learning_domain`

| Column | Type | Rules |
|---|---|---|
| id | uuid | primary key |
| slug | varchar(80) | unique, not null |
| name | varchar(120) | not null |
| description | text | not null |
| icon_key | varchar(60) | nullable |
| display_order | integer | positive, not null |
| active | boolean | default true |
| created_at / updated_at | timestamptz | not null |

### `learning_technology`

| Column | Type | Rules |
|---|---|---|
| id | uuid | primary key |
| domain_id | uuid | FK domain, not null |
| slug | varchar(80) | not null |
| name | varchar(120) | not null |
| description | text | not null |
| display_order | integer | positive, not null |
| active | boolean | default true |
| created_at / updated_at | timestamptz | not null |

Unique: `(domain_id, slug)` and `(domain_id, display_order)`.

All catalog tables additionally carry `lifecycle_status` (`DRAFT`, `PUBLISHED`, `ARCHIVED`), `version` for optimistic locking, `created_by`, and `updated_by`. Learner APIs return only published, active rows. Ecosystem creation inserts a `learning_domain`; no code or enum change is required.

### `learning_topic`

| Column | Type | Rules |
|---|---|---|
| id | uuid | primary key |
| technology_id | uuid | FK technology, not null |
| slug | varchar(100) | not null |
| title | varchar(180) | not null |
| summary | text | not null |
| skill_level | varchar(20) | checked enum |
| estimated_minutes | integer | between 5 and 1440 |
| objectives | jsonb | non-empty string array |
| display_order | integer | positive, not null |
| active | boolean | default true |
| current_content_version_id | uuid | nullable; FK added after version table |
| created_at / updated_at | timestamptz | not null |

Unique: `(technology_id, slug)` and `(technology_id, display_order)`.

### `learning_topic_prerequisite`

| Column | Type | Rules |
|---|---|---|
| topic_id | uuid | FK topic |
| prerequisite_topic_id | uuid | FK topic; differs from topic_id |

Composite primary key on both columns. Cycles are rejected by the application service.

### `learning_path`

Contains `id`, globally unique `slug`, `name`, `description`, `skill_level`, `active`, and timestamps.

### `learning_path_topic`

Contains `path_id`, `topic_id`, and positive `position`. Primary key `(path_id, topic_id)`; unique `(path_id, position)`.

## 4. Generated-content tables

### `study_content_version`

| Column | Type | Rules |
|---|---|---|
| id | uuid | primary key |
| topic_id | uuid | FK topic, not null |
| version_number | integer | positive, not null |
| status | varchar(20) | checked enum |
| title | varchar(180) | not null |
| introduction | text | not null |
| conclusion | text | not null |
| model_name | varchar(120) | not null |
| prompt_version | varchar(40) | not null |
| content_hash | char(64) | SHA-256, not null |
| generated_at | timestamptz | not null |
| published_at | timestamptz | nullable |
| created_at | timestamptz | not null |

Unique: `(topic_id, version_number)`. A partial unique index permits at most one `PUBLISHED` version per topic. Changing the current version supersedes the previous version transactionally.

### `study_unit`

| Column | Type | Rules |
|---|---|---|
| id | uuid | primary key |
| content_version_id | uuid | FK version, cascade delete for drafts only |
| stable_key | varchar(100) | stable within a content version |
| unit_type | varchar(20) | checked enum |
| title | varchar(180) | not null |
| body_markdown | text | not null, size-limited |
| code_language | varchar(40) | nullable |
| code_example | text | nullable, size-limited |
| key_takeaways | jsonb | string array |
| display_order | integer | positive, not null |
| estimated_minutes | integer | positive, not null |
| created_at | timestamptz | not null |

Unique: `(content_version_id, stable_key)` and `(content_version_id, display_order)`.

### `content_generation_job`

Contains `id`, `topic_id`, optional `requested_by_subject`, `status`, `requested_at`, `started_at`, `completed_at`, `attempt_count`, `prompt_version`, `model_name`, optional `result_version_id`, safe `error_code`, truncated `error_message`, `correlation_id`, and timestamps.

A partial unique index on `topic_id` where status is active (`QUEUED`, `GENERATING`, `VALIDATING`) prevents duplicate concurrent generation.

## 5. Progress tables

### `user_topic_progress`

| Column | Type | Rules |
|---|---|---|
| user_subject | varchar(255) | Keycloak subject |
| topic_id | uuid | FK topic |
| content_version_id | uuid | FK content version |
| completed_units | integer | non-negative, derived |
| total_units | integer | non-negative snapshot |
| percent_complete | numeric(5,2) | 0–100, derived |
| started_at | timestamptz | not null |
| completed_at | timestamptz | nullable |
| last_accessed_at | timestamptz | not null |
| updated_at | timestamptz | not null |

Primary key: `(user_subject, topic_id, content_version_id)`.

### `user_unit_progress`

| Column | Type | Rules |
|---|---|---|
| user_subject | varchar(255) | Keycloak subject |
| unit_id | uuid | FK study unit |
| completed | boolean | not null |
| completed_at | timestamptz | nullable |
| updated_at | timestamptz | not null |

Primary key: `(user_subject, unit_id)`.

## 6. Progress calculation

Topic percentage is `completed published-version units / total published-version units × 100`, rounded to two decimals. Technology and domain percentages are weighted by unit count, not an average of topic percentages. Inactive catalog items are excluded from new aggregate views but existing history is retained.

When a new content version is published, earlier progress remains historical. V1 does not automatically transfer completion between versions. The UI clearly identifies an updated lesson and starts progress for the new version.

## 7. Required indexes

- Active domain and child ordering indexes.
- Topic lookup on `(technology_id, active, skill_level, display_order)`.
- Current content lookup on `learning_topic.current_content_version_id`.
- Unit ordering on `(content_version_id, display_order)`.
- User progress indexes on `(user_subject, updated_at desc)`.
- Generation monitoring on `(status, requested_at)`.

## 8. Seed data

Flyway seeds all eight domains, representative technologies, learning paths, and an ordered topic outline. It does not call OpenAI. Detailed content is generated after deployment through the generation workflow.

At minimum seed:

- Java: Core Java, Collections, Concurrency, Spring Boot
- Python: Language Fundamentals, Data Structures, Async Python, FastAPI
- AI: ML Foundations, LLM Fundamentals, Prompt Engineering, RAG
- System Design: Fundamentals, Scalability, Distributed Systems
- Databases: SQL, PostgreSQL, Data Modeling, NoSQL
- CI/CD and DevOps: Git, CI Pipelines, Docker, Kubernetes
- AWS and Cloud: Cloud Fundamentals, IAM, Compute, Storage, Networking
- Software Engineering Principles: Clean Code, SOLID, Testing, Design Patterns

## 9. Migration rules

- Never edit an applied migration.
- Add forward-only migrations with rollback documented separately.
- Do not delete published content or learner progress in normal migrations.
- Test clean installation and upgrade from the previous released schema.

## 10. Authorization and administration tables

### `application_role`

Stores externally mapped role key, display name, active flag, and optimistic-lock version. Seed `candidate`, `interviewer`, and `admin`; additional roles can be mapped without changing endpoint code.

### `capability`

Immutable application-defined capability key and description. Seed at least `LEARNING_CATALOG_READ`, `LEARNING_CONTENT_READ`, `LEARNING_PROGRESS_WRITE`, `CONTENT_GENERATE`, `ADMIN_CATALOG_WRITE`, `ADMIN_CONTENT_PUBLISH`, `ADMIN_POLICY_WRITE`, and `ADMIN_AUDIT_READ`.

### `role_capability_grant`

Composite key `(role_id, capability_id)` with `effect` (`ALLOW`, `DENY`), timestamps, actor subject, and version. Explicit deny wins. Policy updates are transactional and immediately invalidate authorization caches.

### `admin_audit_event`

Append-only UUID, occurrence time, actor subject, action, target type/id, correlation ID, request IP/device context when lawfully available, redacted before/after JSON patches, outcome, and reason. Application credentials cannot update or delete audit events.
