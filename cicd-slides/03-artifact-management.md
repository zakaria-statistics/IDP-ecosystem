[← Previous](./02-pipeline-stages.md) | [📋 Index](./README.md) | [Next →](./04-environment-strategy.md)

---

# Artifact Management

## What is an Artifact?

A **built, versioned, deployable package** of your application.

Examples:
- Docker image: `myapp:1.4.2`
- npm package: `@company/lib@2.0.0`
- JAR file: `app-1.0.0.jar`
- Binary: `myapp-linux-amd64`

---

## Why Artifacts Matter

```
Git Commit ──────► CI Build ──────► Artifact ──────► Deploy
   │                                   │
abstract                            concrete
(source code)                    (runnable thing)
```

**Production runs artifacts, not Git commits directly.**

---

## Immutable Versioned Artifacts

**Golden Rule:** Once built, never modify. Create new version instead.

```
✅ Good:
myapp:1.4.1  → stable
myapp:1.4.2  → new feature
myapp:1.4.3  → hotfix

❌ Bad:
myapp:latest  → what version is this?
myapp:1.4.1   → rebuilt with different code
```

---

## Tagging Strategy

```bash
# Semantic version (releases)
myapp:1.4.2

# Git SHA (for traceability)
myapp:a1b2c3d

# Branch-based (for dev/testing)
myapp:feature-auth-123

# Environment-based (avoid!)
myapp:prod  ← don't do this
```

---

## Artifact Registry

Where artifacts live:

| Type | Registry |
|------|----------|
| Docker images | Docker Hub, ECR, GCR, Harbor |
| npm packages | npm registry, Artifactory |
| Maven/Gradle | Nexus, Artifactory |
| Helm charts | ChartMuseum, OCI registry |

---

## Traceability

Every deployed artifact should answer:
- What Git commit built this?
- When was it built?
- What pipeline built it?
- What tests passed?

```dockerfile
LABEL git.commit="a1b2c3d"
LABEL build.date="2024-01-15"
LABEL pipeline.id="12345"
```


---

[← Previous](./02-pipeline-stages.md) | [📋 Index](./README.md) | [Next →](./04-environment-strategy.md)
