# CI/CD Operations

## Implemented delivery controls

The repository now contains an executable, cluster-isolated GitHub Actions pipeline:

- `ci.yml` runs test, lint, type, 95% coverage, dependency-policy, container-build, and Compose-contract gates.
- `security.yml` runs CodeQL for Java, TypeScript, and Python plus scheduled Trivy vulnerability and misconfiguration scanning.
- `release.yml` runs only after a successful `main` CI push (or an explicit full-SHA dispatch), builds multi-architecture images, pushes immutable SHA tags to GHCR, scans by digest, embeds BuildKit SBOM/provenance attestations, creates GitHub provenance attestations, and emits an attested release manifest.
- Development is promoted automatically through a short-lived GitHub App token. CI has no cluster credentials.
- `promote.yml` promotes the same digest-pinned images to protected staging or production environments.
- Dependabot maintains Actions, Gradle, Python, npm, and container dependencies.
- `deploy/charts/learning-hub` provides the production Helm deployment contract, including TLS Ingress, internal AI routing, restricted workloads, autoscaling, disruption budgets, secret integration, and default-deny networking.
- `deploy/gitops-template` contains the receiving deployment-repository workflow and Argo CD applications for development, staging, and production.

## Required GitHub configuration

Create these repository or organization variables:

| Variable | Example | Purpose |
|---|---|---|
| `GITOPS_OWNER` | `Skpandey15` | Deployment repository owner |
| `GITOPS_REPOSITORY_NAME` | `learning-hub-deployments` | Deployment repository name only |
| `GITOPS_APP_ID` | GitHub App numeric ID | Mints a scoped installation token |

Create this repository secret:

| Secret | Purpose |
|---|---|
| `GITOPS_APP_PRIVATE_KEY` | Private key for the GitHub App installed only on the deployment repository |

The GitHub App needs only repository metadata read and contents read/write permission on the deployment repository. It must not have organization administration, package deletion, Actions administration, or cluster access.

Create GitHub environments named `development`, `staging`, and `production`:

- Restrict all three to the `main` branch.
- Require independent reviewers and prevent self-review for production.
- Add a staging reviewer when operational risk warrants it.
- Configure a production wait timer and any external change-management protection rule required by policy.
- Store environment-specific secrets in the GitOps/runtime secret manager, not this application repository.

Protect `main` with pull requests, conversation resolution, signed commits where supported, no force pushes, no deletion, CODEOWNERS review, and these required checks:

- `CI success`
- all `CodeQL` language jobs
- `Repository vulnerability and misconfiguration scan`

Restrict allowed Actions to GitHub-owned, Docker-owned, Astral, and Aqua Security actions and require full-length SHA pins.

## GitOps receiver contract

The deployment repository must handle a `repository_dispatch` event named `learning-hub-promote`. Its payload contains:

```json
{
  "environment": "development",
  "commit": "40-character source commit",
  "images": {
    "api": "ghcr.io/owner/learning-hub-api@sha256:...",
    "ai-service": "ghcr.io/owner/learning-hub-ai-service@sha256:...",
    "web": "ghcr.io/owner/learning-hub-web@sha256:..."
  }
}
```

The receiver must validate the payload, verify GitHub attestations, update only the selected environment's digest values, open or merge the policy-appropriate GitOps change, and let Argo CD reconcile it. It must never rebuild images or accept mutable tags.

Seed the separate deployment repository from `deploy/gitops-template`, replace all endpoint/DNS placeholders, install its `AppProject`, and bootstrap the environment `Application` objects. The Argo applications use the application repository's exact release commit for the Helm chart and the deployment repository for environment values.

## Release and promotion

1. Merge an approved PR into `main`.
2. CI passes; release builds and scans all three images once.
3. The attested manifest is retained for 90 days and development promotion is dispatched automatically.
4. Verify development health and copy the three digest-qualified image references from the release manifest into `Promote release`.
5. Select staging. After staging evidence is accepted, rerun with production; the protected environment supplies the human/control-plane approval.

The current chart uses zero-unavailable rolling Deployments protected by readiness probes and disruption budgets. Rollback changes desired-state digests to the last known-good manifest; database migrations are never automatically reversed. Metric-driven Argo Rollouts canary analysis remains a later enhancement and must not be claimed as active until its controller and analysis templates are installed.

Docker Compose is never used by post-merge deployment. It remains a local-development facility only.

## Failure behavior

- Failed tests, coverage, dependency policy, scans, image publication, attestation, or manifest assembly stop delivery.
- Superseded CI runs cancel; release and environment promotion never cancel in progress.
- A missing GitOps configuration fails the development-promotion stage. Releases cannot appear successfully deployed until the GitOps receiver is configured.
- Invalid or mutable image references are rejected before a GitOps event is sent.
- Promotion uses a short-lived installation token and never receives Kubernetes or cloud credentials.
