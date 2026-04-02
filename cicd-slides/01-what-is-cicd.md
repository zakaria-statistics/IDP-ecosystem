[📋 Index](./README.md) | [Next →](./02-pipeline-stages.md)

---

# What is CI/CD?

## Continuous Integration (CI)

**Automatically build and test code on every change.**

```
Developer pushes code
        ↓
    CI Pipeline
        ↓
Build → Test → Scan
        ↓
   ✅ or ❌ Feedback
```

**Goal:** Catch bugs early, ensure code quality.

---

## Continuous Delivery (CD)

**Automatically prepare code for deployment.**

```
CI passes
    ↓
Package artifact (Docker image)
    ↓
Deploy to staging
    ↓
Manual approval → Deploy to production
```

---

## Continuous Deployment

**Automatically deploy to production (no manual gate).**

```
CI passes → Deploy to staging → Auto-deploy to production
```

⚠️ Requires high test confidence & feature flags.

---

## The Full Picture

```
Code → Build → Test → Scan → Package → Deploy → Monitor
  │                                        │
  └────────────── Feedback Loop ───────────┘
```

| Phase | CI | CD |
|-------|----|----|
| Build | ✓ | |
| Test | ✓ | |
| Security Scan | ✓ | |
| Package Artifact | | ✓ |
| Deploy | | ✓ |
| Release | | ✓ |


---

[📋 Index](./README.md) | [Next →](./02-pipeline-stages.md)
