# Local k3d development deployment

The `Deploy k3d development` workflow deploys every successful `main` release to the local `k3d-dev` cluster through a repository-scoped, user-level GitHub Actions runner.

The development ingress is available at `https://learning.127.0.0.1.nip.io:8443`. Its certificate is locally generated and self-signed.

Platform services are declared in `platform.yaml`. Secret values are deliberately not committed; the cluster must contain `learning-hub-runtime` and `learning-hub-dev-tls` in the `learning-hub-development` namespace. The workflow consumes immutable, attested image digests from the corresponding Release run.

Run `./deploy/k3d-dev/bootstrap-cluster.ps1` once to generate the local secrets and reconcile the platform. By default it copies the OpenAI key already stored in `online-interview-dev/platform-secrets`; override the source-secret parameters when needed.

Run `./deploy/k3d-dev/install-runner.ps1` once to install the checksum-verified, repository-scoped runner. It registers a per-user logon task so deployments continue after a reboot without requiring administrator privileges.
