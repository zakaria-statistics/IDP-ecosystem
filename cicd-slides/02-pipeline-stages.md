[← Previous](./01-what-is-cicd.md) | [📋 Index](./README.md) | [Next →](./03-artifact-management.md)

---

# Pipeline Stages

## Typical Pipeline Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                        CI PIPELINE                              │
├─────────┬─────────┬─────────┬─────────┬─────────┬──────────────┤
│  Build  │  Lint   │  Test   │  SAST   │ Secrets │   Package    │
└─────────┴─────────┴─────────┴─────────┴─────────┴──────────────┘
                                                          │
                                                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                        CD PIPELINE                              │
├───────────────┬───────────────┬───────────────┬────────────────┤
│  Deploy Dev   │ Deploy Stage  │   Approval    │  Deploy Prod   │
└───────────────┴───────────────┴───────────────┴────────────────┘
```

---

## CI Stages Explained

| Stage | Purpose | Tools |
|-------|---------|-------|
| **Build** | Compile code, resolve deps | npm, maven, go build |
| **Lint** | Code style & quality | ESLint, Prettier, golint |
| **Unit Test** | Test individual functions | Jest, pytest, go test |
| **Integration Test** | Test components together | Testcontainers |
| **SAST** | Static security analysis | SonarQube, Semgrep |
| **Secret Detection** | Find leaked credentials | Gitleaks, TruffleHog |
| **Package** | Create deployable artifact | Docker build, npm pack |

---

## CD Stages Explained

| Stage | Purpose | Gate |
|-------|---------|------|
| **Deploy Dev** | Developer testing | Auto |
| **Deploy Stage** | QA validation | Auto or Manual |
| **Deploy Prod** | Live users | Manual approval |

---

## Pipeline Per Branch Type

```yaml
feature/* → dev:
  Build → Lint → Unit Tests → SAST → Secrets → Light E2E

dev → stage:
  Full Integration → API Contract → DB Migration → E2E → Perf

stage → main:
  Manual Approval → Full E2E → Security Audit → Deploy → Tag
```


---

[← Previous](./01-what-is-cicd.md) | [📋 Index](./README.md) | [Next →](./03-artifact-management.md)
