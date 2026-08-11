# Learning Hub — CI/CD and Automatic Deployment Design

## 1. Objectives

The delivery platform must provide:

- Fast pull-request feedback with reproducible tests.
- Immutable, signed, traceable application artifacts.
- No long-lived cloud credentials in GitHub.
- Automatic development deployment after merge.
- Controlled staging and production promotion of the same image digests.
- Progressive production delivery with automatic rollback.
- Database migration safety, zero-trust boundaries, and complete auditability.
- Cloud portability across managed Kubernetes providers.

## 2. Selected stack

| Layer | Technology | Purpose |
|---|---|---|
| Source and automation | GitHub + GitHub Actions | Review, CI, release orchestration |
| Build | Docker Buildx/BuildKit | Cached multi-platform OCI builds |
| Registry | GitHub Container Registry | Immutable API, AI, and web images |
| Provenance | GitHub artifact attestations / Sigstore | Signed build provenance and SBOM |
| Vulnerability controls | Trivy plus GitHub dependency review/CodeQL | Source, dependency, IaC, image scanning |
| Runtime | Managed Kubernetes 1.36.x baseline | Portable production orchestration |
| Packaging | Helm 3 with environment values and JSON schemas | Versioned deployment contract |
| GitOps | Argo CD | Pull-based reconciliation and drift correction |
| Progressive delivery | Argo Rollouts | Canary analysis, promotion, and rollback |
| Policy enforcement | Sigstore Policy Controller + Kyverno | Admit only approved signed images and safe workloads |
| Secrets | External Secrets Operator + cloud secret manager | Short-lived, centrally rotated secrets |
| Infrastructure | OpenTofu with remote encrypted state | Reproducible cloud and cluster resources |
| Telemetry | OpenTelemetry, Prometheus, Grafana, Loki, Tempo | Release health and rollback signals |

Every tool, action, chart, and image is pinned to an exact version or digest. The implementation PR resolves the latest mutually compatible stable versions; prerelease and floating `latest` references are prohibited.

## 3. Repository model

Use two public repositories with different write boundaries:

```text
learning-hub/
├── application source
├── Dockerfiles
├── deploy/charts/learning-hub/
├── infrastructure/modules/
└── GitHub Actions workflows

learning-hub-deployments/
├── environments/dev/
├── environments/staging/
└── environments/production/
```

The application repository builds artifacts but cannot directly access a cluster. A narrowly scoped GitHub App or repository token may open a deployment-repository PR; it cannot merge production changes. Argo CD alone pulls approved desired state into clusters.

## 4. Delivery flow

```text
Pull request
  → deterministic CI and security gates
  → merge to main
  → build each changed image once
  → push immutable SHA tags to GHCR
  → generate SBOM + provenance attestations
  → update dev digest in GitOps repository
  → Argo CD syncs dev
  → smoke and integration tests
  → promotion PR to staging
  → staging sync + E2E/DAST/load checks
  → protected production approval
  → production canary 5% → 25% → 50% → 100%
  → automatic promotion or rollback from live metrics
```

Promotion changes only image digests and configuration references. Images are never rebuilt per environment.

## 5. Pull-request CI

Use path-aware jobs while retaining full required checks:

1. Repository hygiene
   - formatting, linting, lockfile integrity, conventional metadata;
   - secret scanning and generated-file checks;
   - workflow and action policy validation.
2. API
   - Gradle dependency verification;
   - unit, architecture, authorization, and Testcontainers integration tests;
   - Flyway clean-install and previous-release upgrade tests.
3. AI service
   - Ruff, mypy, pytest, contract tests, prompt snapshots, and mocked LiteLLM failures.
4. Web
   - ESLint, TypeScript, Vitest, production build, accessibility tests, and component tests.
5. Security
   - CodeQL/SAST, dependency review, license policy, IaC/Kubernetes scans, and Dockerfile checks.
6. Integration
   - Build local images;
   - start an ephemeral Compose stack without real OpenAI access;
   - run API contract and Playwright smoke tests.

Cancel superseded runs. Use least-privilege workflow permissions, protected reusable workflows, GitHub-hosted ephemeral runners by default, and SHA-pinned third-party actions.

## 6. Build and supply-chain security

On a protected `main` merge or signed release tag:

- Build Linux `amd64` and `arm64` OCI images with BuildKit.
- Use registry-backed cache scoped by image and branch.
- Tag with commit SHA and semantic version; deployment uses digest only.
- Generate SPDX or CycloneDX SBOM for every image.
- Produce GitHub/Sigstore provenance and SBOM attestations using OIDC.
- Scan the final image by digest and block critical/high exploitable findings according to the vulnerability SLA.
- Push API, AI-service, and web images to GHCR with immutable tags.
- Retain build metadata, test reports, SBOM, scan report, and digest manifest.

Kubernetes admission rejects unsigned/unattested application images, images outside the approved GHCR namespace, mutable tags, privileged workloads, root containers, and manifests missing resource/security constraints.

## 7. Environment strategy

### Development

- Automatic deployment for every successful merge to `main`.
- Argo CD auto-sync, prune, and self-heal enabled.
- Automated smoke tests run after health convergence.
- Optional short-lived preview namespace per pull request with TTL cleanup.

### Staging

- Promotion PR created automatically after development verification.
- Approval may be automatic for routine changes when all risk checks pass.
- Production-like topology, anonymized data, AI provider sandbox/budget, E2E, DAST, migration rehearsal, and baseline performance tests.

### Production

- GitHub protected environment requires designated approval and fresh successful staging evidence.
- Argo Rollouts performs canary delivery at 5%, 25%, 50%, and 100% with observation pauses.
- Prometheus analysis checks error rate, p95 latency, saturation, pod health, authentication failures, and business smoke probes.
- Failed analysis automatically aborts and returns traffic to the stable ReplicaSet.
- Emergency rollback changes the GitOps digest to the last known-good release; direct cluster mutation is break-glass only.

## 8. Database delivery

Use expand/migrate/contract migrations:

- Flyway validates migrations during CI and staging restore rehearsal.
- A pre-deployment Kubernetes Job runs forward-only compatible migrations once, using advisory locking and a least-privilege migration identity.
- Application rollout begins only after migration success.
- Destructive contract migrations occur in a later release after old application versions are retired.
- Application rollback must remain compatible with the expanded schema.
- Production backup and restore verification is required before high-risk migrations.
- Never automatically reverse a partially applied database migration.

## 9. Zero-trust deployment controls

- GitHub Actions obtains short-lived cloud identity through OIDC; no static cloud access key exists.
- CI can publish artifacts and propose desired state but cannot connect to Kubernetes.
- Argo CD uses workload identity and read-only source credentials, with namespace-scoped deployment permissions.
- Cluster ingress, east-west traffic, and egress use default-deny network policies.
- Workloads run non-root with read-only root filesystems, dropped capabilities, seccomp, resource limits, and dedicated service accounts.
- Secrets are delivered at runtime from a managed secret store and never committed to Git.
- Production approvals, GitOps merges, Argo syncs, policy decisions, and rollout analysis are centrally audited.

## 10. Workflow decomposition

```text
.github/workflows/
├── ci.yml                    # PR and main quality gates
├── security.yml              # scheduled/full security analysis
├── release.yml               # build, push, SBOM, attest
├── promote-dev.yml           # update dev digest automatically
├── promote-staging.yml       # create/verify staging promotion
├── promote-production.yml    # protected production promotion
├── dependency-update.yml     # controlled update validation
└── reusable/
    ├── java-checks.yml
    ├── python-checks.yml
    ├── web-checks.yml
    └── build-image.yml
```

Workflows use explicit concurrency groups, timeouts, minimal permissions, environment protection, pinned action SHAs, and artifact retention limits.

## 11. Release and versioning policy

- Trunk-based development with short-lived branches and required pull requests.
- Conventional commits are optional; release automation calculates semantic versions from approved release metadata.
- Release manifests bind application version, commit SHA, three image digests, chart version, migration version, SBOMs, and attestations.
- A release is immutable. Fixes create a new release; tags and images are not overwritten.
- Renovate proposes dependency/action/chart updates in grouped PRs and never auto-merges a major version.

## 12. Failure handling

| Failure | Result |
|---|---|
| CI test or policy failure | Merge blocked |
| Image/SBOM/attestation failure | No release published |
| Development smoke failure | Staging promotion not created |
| Migration failure | Application rollout blocked; operator alerted |
| Canary metric regression | Automatic rollback to stable ReplicaSet |
| Argo CD unavailable | Existing workload continues; no push-based fallback |
| Secret provider unavailable | Existing mounted secret behavior follows TTL; new pods fail closed |
| Audit/attestation verification unavailable | Production promotion fails closed |

## 13. Delivery metrics and SLOs

Track deployment frequency, lead time, change-failure rate, mean recovery time, pipeline duration, flaky-test rate, vulnerability age, rollback count, and GitOps drift. Initial targets:

- PR feedback under 10 minutes at p95.
- Development deployment under 15 minutes after merge.
- Production rollback decision within 10 minutes of harmful canary behavior.
- 100% of production workloads admitted by provenance policy.
- Zero production deployments using mutable tags or direct CI cluster credentials.

## 14. Implementation phases

1. Split current CI into reusable, SHA-pinned quality workflows.
2. Add Helm chart schemas and Kubernetes security defaults.
3. Build and publish digest-addressed images to GHCR.
4. Generate SBOM and provenance attestations; enforce admission policy.
5. Provision development Kubernetes and Argo CD through OpenTofu.
6. Create deployment repository and automate development promotion.
7. Add staging tests, protected environments, and migration rehearsal.
8. Add Argo Rollouts canary analysis and production rollback drills.
9. Add observability dashboards, SLOs, runbooks, and disaster-recovery exercises.

## 15. Acceptance criteria

- A merged change reaches development without human cluster access.
- Staging and production deploy the exact digests verified in development.
- No workflow stores a long-lived cloud credential.
- Unsigned or unattested images are rejected by the cluster.
- Production canary regression triggers automatic rollback.
- Database changes are forward-compatible and rehearsed against a restored backup.
- Direct configuration drift is detected and reconciled.
- Every release is traceable from running pod to image digest, SBOM, workflow, commit, review, and approval.

