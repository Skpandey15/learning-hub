# Learning Hub V1 — Administration Design

## Scope

The admin console is a production control plane for curriculum and authorization. An administrator can create any ecosystem, build its hierarchy, generate or revise study content, publish it, configure role capabilities, and inspect audit history.

## Authorization

Admin access requires all of:

1. A valid Keycloak identity with the mapped `admin` role.
2. The specific database-backed capability for the action.
3. Recent MFA/step-up authentication for policy, publish, regenerate, or archive actions.

The UI never serves as an authorization boundary. Controllers and application services both enforce capabilities. Policy changes cannot remove the final active policy administrator without a separately tested break-glass workflow.

## Curriculum lifecycle

```text
DRAFT → PUBLISHED → ARCHIVED
  ▲          │          │
  └─ new revision       └─ restore to DRAFT
```

- Any ecosystem/domain can be created using name, slug, description, icon key, and order.
- Technologies, topics, prerequisites, paths, objectives, and skill levels are managed beneath it.
- Publishing validates uniqueness, hierarchy completeness, ordering, cycles, and content readiness.
- Published objects are edited through a new revision so learner reads remain stable.
- Archive is reversible. Hard delete is limited to unused drafts and is not exposed initially.

## Content workflow

Administrators can request AI generation, inspect structured output, edit Markdown/code/takeaways, preview sanitized rendering, and publish an immutable version. Regeneration never overwrites a published version. Publication records editor, approver, reason, content hash, prompt version, and model.

For stronger separation of duties, production configuration can require different users for edit and publish; local development may allow the same administrator.

## Access-policy workflow

The page displays roles against named capabilities. Saving requires:

- an effective-policy diff;
- reason for change;
- optimistic version match;
- recent MFA claim;
- confirmation when access is reduced;
- append-only audit event.

Explicit deny overrides allow. Policy evaluation defaults to deny for unknown roles or capabilities.

## Production controls

- Idempotency keys on creates and actions.
- Optimistic locking on all mutable resources.
- Database transactions for hierarchy reorder and policy replacement.
- Rate limits and concurrency caps for AI actions.
- No mass assignment: DTO allowlists fields.
- Pagination, indexed filtering, and bounded exports.
- CSRF protection if cookies are introduced; bearer-only SPA APIs remain protected by strict CORS and token validation.
- Audit records redact tokens, secrets, prompts, and sensitive identity claims.
- Metrics cover admin failures, policy changes, publish latency, and stale drafts.

## Acceptance criteria

- An authorized admin can create and publish a new ecosystem without deployment.
- A learner sees it only after publication and only with the required capability.
- Candidate/interviewer permissions can be changed and take effect promptly.
- Concurrent edits produce a resolvable `409`, never silent data loss.
- Every administrative mutation has an attributable audit record.
- Non-admin and partially privileged users receive `403` for prohibited operations.
