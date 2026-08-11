# Learning Hub V1 — REST API

## 1. General contract

- Public base path: `/api/v1`
- Media type: `application/json`
- Authentication: Keycloak bearer JWT
- Dates: ISO-8601 UTC
- IDs: UUID strings
- Pagination: `page`, `size`, `sort`; maximum `size=100`
- Errors: `application/problem+json` following RFC 9457
- Browser-provided user IDs are ignored or rejected.

Problem example:

```json
{
  "type": "https://learninghub.local/problems/content-generation-failed",
  "title": "Study material generation failed",
  "status": 503,
  "detail": "Study material could not be generated at this time.",
  "instance": "/api/v1/learning/topics/4b.../generation",
  "correlationId": "8c...",
  "code": "AI_SERVICE_UNAVAILABLE"
}
```

## 2. Catalog endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/learning/domains` | Active domains with aggregate user progress |
| GET | `/learning/domains/{domainSlug}` | Domain detail |
| GET | `/learning/domains/{domainSlug}/technologies` | Ordered technologies |
| GET | `/learning/domains/{domainSlug}/technologies/{technologySlug}` | Technology detail |
| GET | `/learning/domains/{domainSlug}/technologies/{technologySlug}/topics` | Topics; filter by `skillLevel` |
| GET | `/learning/topics/{topicId}` | Topic metadata, current content state, and progress |
| GET | `/learning/paths` | Active paths; optional `skillLevel` |
| GET | `/learning/paths/{pathSlug}` | Ordered path topics and progress |

Topic summary example:

```json
{
  "id": "4b5a...",
  "slug": "java-collections",
  "title": "Java Collections",
  "summary": "Choose and use Java collection types effectively.",
  "skillLevel": "INTERMEDIATE",
  "estimatedMinutes": 90,
  "objectives": ["Compare List, Set, Queue, and Map"],
  "contentState": "PUBLISHED",
  "currentContentVersion": 1,
  "progress": {"completedUnits": 2, "totalUnits": 6, "percent": 33.33}
}
```

## 3. Study-content endpoints

### `GET /learning/topics/{topicId}/content`

Returns the topic's current published version and ordered units. Response contains `ETag`; clients send `If-None-Match` on subsequent reads. If content is absent, return `404` with code `CONTENT_NOT_GENERATED` and the topic metadata endpoint still reports `MISSING`.

### `POST /learning/topics/{topicId}/generation`

Requests initial generation. Both V1 roles are permitted. The endpoint is idempotent for an active topic job and is protected by per-user and per-topic rate limits.

Response: `202 Accepted` for a new job and `200 OK` when returning an existing active job.

```json
{
  "jobId": "f8d...",
  "topicId": "4b5...",
  "status": "QUEUED",
  "requestedAt": "2026-08-11T10:00:00Z",
  "pollAfterSeconds": 2
}
```

### `GET /learning/generation-jobs/{jobId}`

Returns safe job state. A user may read a job they requested; both roles may also see the active job for a topic without private requester data.

### `POST /learning/topics/{topicId}/regeneration`

Creates a new version. Disabled by default in V1 public UI and reserved for a future admin policy. The API exists only when the deployment property enables it.

## 4. Progress endpoints

| Method | Path | Purpose |
|---|---|---|
| PUT | `/learning/units/{unitId}/completion` | Idempotently set completion |
| POST | `/learning/topics/{topicId}/access` | Record topic start/last access |
| GET | `/learning/progress/me` | Aggregate dashboard and recent topics |
| GET | `/learning/progress/me/topics/{topicId}` | Current and historical version progress |

Completion request:

```json
{"completed": true}
```

Completion response:

```json
{
  "unitId": "9a2...",
  "completed": true,
  "completedAt": "2026-08-11T10:15:00Z",
  "topicProgress": {
    "completedUnits": 3,
    "totalUnits": 6,
    "percent": 50.00,
    "completed": false
  }
}
```

The unit and topic aggregate are updated in one transaction. Completing a unit not belonging to the current published version returns `409 CONTENT_VERSION_CHANGED` with the new topic version.

## 5. Internal AI endpoint

FastAPI exposes only to the internal network:

### `POST /internal/v1/study-content/generate`

Headers include `Authorization: Bearer <service-token>`, `X-Correlation-ID`, and an idempotency key equal to the generation job ID.

Request:

```json
{
  "jobId": "f8d...",
  "promptVersion": "study-material-v1",
  "topic": {
    "id": "4b5...",
    "domain": "Java",
    "technology": "Core Java",
    "title": "Java Collections",
    "summary": "...",
    "skillLevel": "INTERMEDIATE",
    "estimatedMinutes": 90,
    "objectives": ["..."]
  }
}
```

The response follows the schema in `AI_DESIGN.md`. FastAPI does not decide database IDs, publication state, or user progress.

## 6. Health endpoints

- Spring Boot: `/actuator/health/liveness`, `/actuator/health/readiness`
- FastAPI: `/health/live`, `/health/ready`
- Readiness checks required dependencies; liveness does not call OpenAI.

## 7. Configurable access policy

The following is seed policy, not hardcoded controller logic:

| Capability | candidate | interviewer | admin |
|---|---:|---:|---:|
| Browse active catalog | Allow | Allow | Allow |
| Read published content | Allow | Allow | Allow |
| Request missing content | Allow | Allow | Allow |
| Manage own progress | Allow | Allow | Allow |
| Read another user's progress | Deny | Deny | Deny |
| Manage curriculum | Deny | Deny | Allow |
| Manage access policy | Deny | Deny | Allow |

Spring Security checks named capabilities through a policy service. Admin identity alone is insufficient when its capability is denied.

## 8. Admin API

Admin base path is `/api/v1/admin`. Create/update requests require `Idempotency-Key`; updates require `If-Match` with the resource version.

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/ecosystems` | List all or create ecosystem/domain |
| GET/PATCH | `/ecosystems/{id}` | Read or update metadata and ordering |
| POST | `/ecosystems/{id}/publish` | Publish after hierarchy validation |
| POST | `/ecosystems/{id}/archive` | Reversible archive; requires reason |
| GET/POST/PATCH | `/ecosystems/{id}/technologies[...]` | Manage technologies |
| GET/POST/PATCH | `/technologies/{id}/topics[...]` | Manage topics and objectives |
| GET/POST/PATCH | `/learning-paths[...]` | Manage paths and ordering |
| POST | `/topics/{id}/generation` | Generate draft content |
| POST | `/content-versions/{id}/publish` | Publish validated version |
| GET | `/roles` and `/capabilities` | Read policy catalog |
| PUT | `/roles/{roleKey}/capabilities` | Atomically replace grants |
| GET | `/audit-events` | Filtered, paginated audit history |

Hard delete is not exposed for published curriculum in V1. Archive is reversible and preserves learner history.

## 9. API safeguards

- Validate slugs, UUIDs, enums, page size, and request bodies.
- Apply optimistic locking to publication and progress aggregates.
- Rate-limit generation by user, topic, and deployment-wide concurrency.
- Use bounded timeouts for Spring Boot → FastAPI.
- Never return model prompts, credentials, stack traces, or raw provider errors.
- Enforce capability checks at controller and service layers for administrative writes.
- Require step-up authentication freshness for policy and publication operations.
