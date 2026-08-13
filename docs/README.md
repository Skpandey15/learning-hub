# Learning Hub V1 — Design Pack

## Phase 3 study-material design

- [Study-material platform design](STUDY_MATERIAL_PLATFORM_DESIGN.md) — authoritative end-to-end design for curriculum, AI-generated drafts, review/publication, learner access, progress, security, and delivery slices.

## Product vision

Learning Hub is a greenfield, production-oriented study platform for candidates and interviewers. It combines a structured engineering curriculum with AI-generated study material and persistent learner progress.

## V1 goals

- Browse a database-driven engineering curriculum.
- Generate structured study material through OpenAI.
- Store generated material in PostgreSQL and reuse it on later visits.
- Read theory, worked examples, key takeaways, and practice exercises.
- Mark learning units complete and resume after signing in again.
- Show progress by topic, technology, and domain.
- Allow both `candidate` and `interviewer` roles to study.
- Let administrators configure which roles receive each learning capability.
- Provide an admin console for managing any ecosystem and its curriculum.
- Support deployment behind a zero-trust identity-aware access layer.
- Run the complete local stack through Docker Compose.

## Learning flow

```text
Domain → Technology → Topic → Skill level → Learning path
       → Generated study units → Completion → Progress → Next topic
```

## V1 domains

The initial seed contains:

1. Java
2. Python
3. Artificial Intelligence
4. System Design
5. Databases
6. CI/CD and DevOps
7. AWS and Cloud
8. Software Engineering Principles

Administrators can add, reorder, disable, publish, and archive any ecosystem without a code deployment. “Domain” remains the internal API/database term for ecosystem in V1.

## V1 scope

The application includes:

- React and TypeScript web application
- Java 21 and Spring Boot REST API
- Python FastAPI AI service
- PostgreSQL catalog, content, and progress storage
- Keycloak authentication and role management
- Database-driven role-to-capability policy
- Administrative curriculum and policy console with audit history
- LiteLLM as the OpenAI-compatible model gateway
- Docker Compose local environment
- Flyway database migrations

The taxonomy is stored in PostgreSQL. The frontend must not contain authoritative domain, technology, topic, path, or study-unit lists.

## Content policy

OpenAI generates study material from server-owned prompts and database metadata. Generated content is validated, versioned, and persisted before being displayed. A previously published version remains available if the AI service is unavailable. The browser never receives an LLM credential and never calls the AI service directly.

## Explicit V1 non-goals

- Assessments, quizzes, scoring, or attempt history
- Interview execution or interview scheduling
- Payments or subscriptions
- Course marketplace
- Video hosting
- Certificates
- Multi-tenant authoring
- Multi-tenant editorial workflows
- Personalized recommendation models
- User-generated public courses

## Design documents

- `ARCHITECTURE.md`
- `DATA_MODEL.md`
- `REST_API.md`
- `UI_UX.md`
- `AI_DESIGN.md`
- `DOCKER_COMPOSE.md`
- `CODEX_IMPLEMENTATION_PLAN.md`
- `ADMIN_DESIGN.md`
- `ZERO_TRUST_SECURITY.md`
- `DEPENDENCY_BASELINE.md`
- `CICD_DESIGN.md`
- `CICD_OPERATIONS.md`
- `OBSERVABILITY_ERROR_HANDLING.md`
- `TESTING_QUALITY_GATES.md`

## Definition of done

V1 is complete when:

- Docker Compose starts the full local platform.
- Flyway creates and seeds the learning taxonomy.
- Candidate, interviewer, and admin access follows configurable capability policy.
- Administrators can manage the complete ecosystem hierarchy.
- Study material can be generated through the AI service and persisted.
- Published material remains readable during an AI outage.
- Unit completion and aggregate progress persist across sessions.
- Authentication and ownership checks are enforced by the backend.
- Backend, frontend, AI-service, and integration tests pass.
- No key, password, token, or generated secret is committed.

## Agreed product decisions

- This is a greenfield implementation.
- V1 concentrates on study material; assessments are excluded.
- Study material is generated using OpenAI through LiteLLM.
- Candidate and interviewer permissions are configurable; the initial seed grants both the same study capabilities.
- `admin` manages curriculum and access policy through a separately protected console.
