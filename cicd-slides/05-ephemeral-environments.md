[← Previous](./04-environment-strategy.md) | [📋 Index](./README.md) | [Next →](./06-deployment-strategies.md)

---

# Ephemeral Environments

## What Are Ephemeral Environments?

**Temporary, on-demand environments that are created for testing and destroyed after.**

```
PR opened ──► Create environment ──► Run tests ──► PR merged ──► Destroy
```

Also called: Preview environments, Review apps, Dynamic environments

---

## Why Ephemeral Environments?

| Problem | Solution |
|---------|----------|
| "Works on my machine" | Test in real infra |
| Shared dev env conflicts | Isolated per PR/branch |
| Long-lived env drift | Fresh every time |
| QA bottleneck | Parallel testing |
| Cost of idle envs | Pay only when needed |

---

## What Gets Created?

```
┌─────────────────────────────────────────────────┐
│           Ephemeral Environment                 │
│                                                 │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│  │   App   │  │   DB    │  │  Cache  │        │
│  │Container│  │Container│  │Container│        │
│  └─────────┘  └─────────┘  └─────────┘        │
│                                                 │
│  Namespace: pr-123                              │
│  URL: https://pr-123.preview.company.com       │
└─────────────────────────────────────────────────┘
```

---

## Implementation Options

| Tool | Creates |
|------|---------|
| **Kubernetes + Helm** | Namespace per PR |
| **Docker Compose** | Container stack |
| **Terraform** | Full infra (VMs, DBs) |
| **Vercel/Netlify** | Preview deploys |
| **ArgoCD ApplicationSets** | K8s apps per branch |

---

## Example: K8s Namespace per PR

```yaml
# CI Pipeline
stages:
  - create-preview
  - test
  - cleanup

create-preview:
  script:
    - kubectl create namespace pr-${CI_MERGE_REQUEST_IID}
    - helm install myapp ./chart -n pr-${CI_MERGE_REQUEST_IID}
    - echo "Preview: https://pr-${CI_MERGE_REQUEST_IID}.preview.company.com"

cleanup:
  when: manual  # or on PR close
  script:
    - kubectl delete namespace pr-${CI_MERGE_REQUEST_IID}
```

---

## Example: Docker Compose for Tests

```yaml
# docker-compose.test.yml
services:
  app:
    build: .
    depends_on:
      - db
      - redis

  db:
    image: postgres:15
    environment:
      POSTGRES_DB: test

  redis:
    image: redis:7

  test-runner:
    build: .
    command: npm test
    depends_on:
      - app
```

```bash
# CI script
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
docker-compose -f docker-compose.test.yml down -v  # cleanup
```

---

## Lifecycle Management

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│   PR Opened ──► Create Env ──► Deploy ──► Run Tests     │
│                                              │           │
│                                              ▼           │
│   PR Updated ──────────────────► Redeploy ──► Retest    │
│                                              │           │
│                                              ▼           │
│   PR Merged/Closed ──────────────────────► Destroy      │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## Best Practices

| Practice | Why |
|----------|-----|
| Auto-destroy on PR close | Avoid zombie environments |
| TTL (time-to-live) | Safety net for forgotten PRs |
| Resource limits | Control costs |
| Seeded test data | Consistent testing |
| Isolated networking | No cross-env leaks |


---

[← Previous](./04-environment-strategy.md) | [📋 Index](./README.md) | [Next →](./06-deployment-strategies.md)
