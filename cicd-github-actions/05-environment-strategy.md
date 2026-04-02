[Previous: Artifact Management](./04-artifact-management.md) | [Index](./README.md) | [Next: Workflow Examples](./06-workflow-examples.md)

---

# Environment Strategy

## The Three Core Environments

```
+--------------+    +--------------+    +--------------+
|     DEV      | -> |    STAGE     | -> |    PROD      |
|              |    |              |    |              |
| Integration  |    |      QA      |    |  Live Users  |
|   Testing    |    |  Validation  |    |              |
+--------------+    +--------------+    +--------------+
       |                  |                   |
   dev branch        stage branch        main branch
```

---

## Environment Purposes

| Environment | Purpose | Data | Who Uses |
|-------------|---------|------|----------|
| **Dev** | Integration, experiments | Synthetic/mock | Developers |
| **Stage** | QA, pre-release validation | Sanitized copy | QA + Devs |
| **Prod** | Real users | Real data | Everyone |

---

## Branch to Environment Mapping

```
feature/* ---> (PR to dev, CI only)
       |
       v
     dev ------> Dev environment (auto-deploy)
       |
       v
    stage -----> Stage environment (auto-deploy)
       |
       v
     main -----> Prod environment (manual approval)
```

---

## GitHub Environments

Define environments in your repo: **Settings -> Environments**

```yaml
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://app.example.com
    steps:
      - run: echo "Deploying to production..."
```

Environment features:
- **Protection rules** (required reviewers)
- **Wait timers** (delay before deploy)
- **Environment secrets** (per-environment)

---

## Environment Protection Rules

For production environment, configure:

- Required reviewers (Tech Lead approval)
- Wait timer (optional delay)
- Branch restrictions (only `main` can deploy)

```
Deploy to Prod
      |
  Waiting for approval...
      |
  Tech Lead approves
      |
  Deployment starts
```

---

## Configuration Management

**Never hardcode environment-specific values!**

```yaml
# Bad
database_url: "postgres://prod-db:5432/myapp"

# Good
database_url: "${DATABASE_URL}"
```

---

## Configuration Per Environment

| Method | Use Case |
|--------|----------|
| **GitHub Secrets** | API keys, passwords |
| **Environment Variables** | URLs, feature flags |
| **Config files** | Application settings |
| **Azure Key Vault** | Production secrets |

---

## GitHub Secrets: Repository vs Environment

```yaml
# Repository secret (available to all environments)
${{ secrets.REGISTRY_TOKEN }}

# Environment secret (only in that environment)
jobs:
  deploy:
    environment: production
    steps:
      - run: echo "${{ secrets.PROD_DATABASE_URL }}"
```

---

## Environment Parity

**Keep environments as similar as possible.**

| Aspect | Dev | Stage | Prod |
|--------|-----|-------|------|
| Same Docker image | Yes | Yes | Yes |
| Same deployment scripts | Yes | Yes | Yes |
| Same app config structure | Yes | Yes | Yes |
| Different values | URLs, secrets, scale |

Only difference = configuration values, not code or images.

---

## Our Setup (Azure/Rancher)

```
GitHub Actions
      |
      | pushes image to
      v
GitHub Container Registry (ghcr.io)
      |
      | pulls image
      v
Azure Kubernetes (Rancher)
      |
      v
  [dev] [stage] [prod] namespaces
```

---

[Previous: Artifact Management](./04-artifact-management.md) | [Index](./README.md) | [Next: Workflow Examples](./06-workflow-examples.md)
