# Deployment repository template

Copy this directory's contents to the private `learning-hub-deployments` repository, replace DNS/private endpoint placeholders, and duplicate the development environment for staging and production. Install `project.yaml` once with cluster-administrator review, then install each environment's `Application` in the Argo CD namespace.

The receiver accepts only attested digest-qualified GHCR images and a full source commit. It updates the environment values and pins the Helm chart source to the exact application commit. Argo CD then automatically syncs, prunes, and self-heals without CI receiving Kubernetes credentials.

Do not bootstrap an environment while its values contain `PENDING_RELEASE`; the first successful application release replaces those values.
