[← Previous](./03-artifact-management.md) | [📋 Index](./README.md) | [Next →](./05-ephemeral-environments.md)

---

# Environment Strategy

## The Three Core Environments

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│     DEV     │───►│    STAGE    │───►│    PROD     │
│             │    │             │    │             │
│ Integration │    │     QA      │    │  Live Users │
│   Testing   │    │  Validation │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
     │                   │                   │
   dev branch       stage branch        main branch
```

---

## Environment Purposes

| Environment | Purpose | Data | Access |
|-------------|---------|------|--------|
| **Dev** | Integration, experiments | Synthetic/mock | Developers |
| **Stage** | QA, pre-release validation | Sanitized copy | QA + Devs |
| **Prod** | Real users | Real data | Restricted |

---

## Configuration Management

**Never hardcode environment-specific values!**

```yaml
# ❌ Bad
database_url: "postgres://prod-db:5432/myapp"

# ✅ Good
database_url: "${DATABASE_URL}"
```

---

## Configuration Sources

| Method | Use Case |
|--------|----------|
| **Environment variables** | Runtime config, secrets |
| **Config files** | Application settings |
| **Secret managers** | Vault, AWS Secrets Manager |
| **ConfigMaps/Secrets** | Kubernetes native |

---

## Environment Parity

**Keep environments as similar as possible.**

| Aspect | Dev | Stage | Prod |
|--------|-----|-------|------|
| Same Docker image | ✓ | ✓ | ✓ |
| Same K8s manifests | ✓ | ✓ | ✓ |
| Same app config structure | ✓ | ✓ | ✓ |
| Different values | URLs, secrets, scale | | |

---

## Branch-to-Environment Mapping

```
feature/* ──► (no deploy, CI only)
       │
       ▼
     dev ─────► Dev environment
       │
       ▼
    stage ────► Stage environment
       │
       ▼
     main ────► Prod environment
```


---

[← Previous](./03-artifact-management.md) | [📋 Index](./README.md) | [Next →](./05-ephemeral-environments.md)
