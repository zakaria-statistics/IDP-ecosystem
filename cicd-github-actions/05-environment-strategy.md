[Previous: Artifact Management](./04-artifact-management.md) | [Index](./README.md) | [Next: Workflow Examples](./06-workflow-examples.md)

---

# Environment Strategy

## Core Environments

```text
dev -> stage -> prod
```

Each environment has a specific purpose:
- `dev`: integration and fast validation
- `stage`: release candidate validation
- `prod`: real user traffic

---

## Branch to Environment Mapping

```text
feature/* -> PR to dev (CI only)
dev       -> deploy dev automatically
stage     -> deploy stage automatically
main      -> deploy prod with approval
```

This mapping stays aligned with the existing Git workflow deck.

---

## Environment Ownership

| Environment | Primary Owner | Goal |
|------------|---------------|------|
| Dev | Developers | Fast feedback |
| Stage | Senior dev + QA | Release confidence |
| Prod | Tech lead + on-call | Reliability and safety |

---

## GitHub Environments for Safety

Create environments in repo settings:
- `development`
- `staging`
- `production`

Example usage:

```yaml
jobs:
  deploy-prod:
    environment:
      name: production
      url: https://app.example.com
```

---

## Production Protection Rules

For `production` environment:
- Required reviewers (tech lead at minimum)
- Optional wait timer
- Restrict to `main` branch deploys

This creates an explicit approval checkpoint.

---

## Secrets and Configuration

Use the right storage per concern:
- Repository secrets: shared technical values (for example, registry auth)
- Environment secrets: environment-specific credentials
- Azure Key Vault: production secret source when needed

Never commit secrets in Git.

---

## Configuration Principles

```text
Code stays identical across environments
Configuration values change per environment
```

Good examples of environment-specific values:
- Database URL
- Third-party API keys
- Feature flags
- Horizontal scale limits

---

## Environment Parity

| Item | Dev | Stage | Prod |
|------|-----|-------|------|
| Docker image | Same | Same | Same |
| Deployment template | Same | Same | Same |
| Runtime values | Different | Different | Different |

Parity reduces "works in stage but fails in prod" incidents.

---

## Azure Kubernetes and Rancher Mapping

```text
GitHub Actions
  -> pulls/pushes ghcr.io images
  -> deploys manifests/helm values
  -> targets Rancher-managed clusters
  -> uses separate namespaces per environment
```

---

## Promotion Checklist

Before promoting `stage -> main`:
- CI gates are green
- Stage smoke checks are green
- Release tag/version is prepared
- Rollback target is known
- Prod approval owner is available

---

[Previous: Artifact Management](./04-artifact-management.md) | [Index](./README.md) | [Next: Workflow Examples](./06-workflow-examples.md)
