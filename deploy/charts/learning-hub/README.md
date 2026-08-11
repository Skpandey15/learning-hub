# Learning Hub Helm chart

This chart deploys the API, AI service, and web application. PostgreSQL, the OIDC provider, LiteLLM, ingress controller, certificate automation, metrics server, and secret store are platform dependencies and are intentionally not bundled.

Production values must use GHCR digest references, HTTPS OIDC, TLS Ingress, and an existing runtime Secret (normally reconciled by External Secrets Operator). The chart creates restricted non-root Deployments, ClusterIP Services, TLS Ingress, HPAs, PodDisruptionBudgets, default-deny NetworkPolicies, topology spreading, probes, resource boundaries, and a tokenless ServiceAccount.

```bash
helm lint deploy/charts/learning-hub -f deploy/charts/learning-hub/ci/test-values.yaml
helm template learning-hub deploy/charts/learning-hub \
  -f deploy/charts/learning-hub/ci/test-values.yaml
```

The AI service is never exposed through Ingress. Actuator endpoints are also not externally routed. Configure `networkPolicy.privateEgressCidrs` for the private PostgreSQL, identity, and LiteLLM endpoints, or replace the baseline NetworkPolicy with a CNI-specific FQDN policy.
