# Learning Hub V1 — Zero-Trust Security Design

## Principle

Never grant trust because a request originates on an internal network. Every human and workload request is authenticated, authorized for a specific capability, constrained by context, encrypted in transit, and observable.

## Human access

- Keycloak remains the OIDC authority and may federate to an enterprise IdP.
- Use authorization code with PKCE, short-lived access tokens, refresh-token rotation, MFA, and session limits.
- A zero-trust proxy may enforce identity, device posture, geography/risk policy, and reauthentication before traffic reaches the application.
- Spring Boot validates issuer, audience, signature, expiry, not-before, authorized party, and role/capability context.
- Forwarded identity/device headers are accepted only from pinned trusted-proxy addresses after the proxy strips incoming copies.
- Administrator routes use a separate access policy and require phishing-resistant MFA where available.

## Workload access

- Public ingress reaches only the web/API gateway and identity endpoints.
- API, AI service, LiteLLM, PostgreSQL, and administrative observability endpoints reside on segmented private networks.
- Prefer mTLS workload identities or signed short-lived service JWTs with audience restriction. Static bearer secrets are allowed only for local development.
- Each service has least-privilege database/network access. FastAPI and LiteLLM cannot write the application database.
- Egress is default-deny; LiteLLM alone may reach approved model-provider endpoints.

## Authorization

- Identity roles are mapped to application roles; application capabilities are database-driven.
- Default deny, explicit deny precedence, resource ownership, and service-layer checks are mandatory.
- High-risk operations require authentication freshness, reason, audit, and optional four-eyes approval.
- Break-glass access is time-limited, monitored, tested, and excluded from normal use.

## Data protection

- TLS 1.2+ externally and TLS/mTLS internally in production.
- Encryption at rest for databases, volumes, backups, and audit storage.
- Secrets come from a secret manager and are rotated; never stored in images, Git, logs, browser bundles, or database content.
- Backups are encrypted, restore-tested, access-controlled, and retention-limited.
- Minimize identity/device data and define retention for progress and audit records.

## Application and AI controls

- Strict input schemas, output encoding, Markdown sanitization, request limits, secure headers, and dependency scanning.
- Server-owned prompts, structured model output, validation, token limits, rate limits, and no browser-to-model path.
- Prompt and provider errors are not returned to users or written unredacted to logs.
- Generate SBOMs, sign images, verify provenance, scan containers, and run as non-root with read-only filesystems where possible.

## Detection and response

- Correlation IDs span proxy, API, AI, and model gateway.
- Centralize authentication, authorization-denial, admin, policy, content-publication, and secret-access events.
- Alert on privilege escalation, repeated denials, unusual generation volume, stale jobs, token validation anomalies, and audit pipeline failure.
- Define incident runbooks for credential compromise, malicious content publication, IdP outage, AI-provider outage, and database recovery.
- Audit failure must block high-risk administrative mutations rather than creating unaudited change.

## Compatibility contract

The system remains vendor-neutral by using OIDC/OAuth standards and standard proxy metadata mappings. Deployments may integrate Cloudflare Access, identity-aware gateways, service meshes, Kubernetes NetworkPolicies, or equivalent controls without changing business APIs.

## Verification

- Threat model and abuse-case review before release.
- Automated authorization matrix and cross-user isolation tests.
- Tests for forged forwarded headers, wrong audience/issuer, expired tokens, stale MFA, and direct internal-service access.
- Network-policy tests proving prohibited east-west and egress paths fail.
- SAST, SCA, secret scan, IaC scan, DAST, SBOM, image signature, and restore test as release evidence.
- Independent penetration test before material public exposure.
