[Previous: GitHub Actions Basics](./02-github-actions-basics.md) | [Index](./README.md) | [Next: Artifact Management](./04-artifact-management.md)

---

# Pipeline Stages

## Standard Pipeline Shape

```text
CI:
  checkout -> build -> lint -> unit test -> basic security

CD:
  build image -> push ghcr -> deploy dev/stage/prod
```

Production deploy requires manual approval.

---

## Branch-Based Flow (Aligned with Git Workflow Deck)

```text
feature/* -> PR to dev:
  fast CI checks only

dev push:
  full CI + image publish + deploy dev

stage push:
  full CI + image promote + deploy stage

main push:
  full CI + prod approval + deploy prod
```

---

## Stage Objectives

| Stage | Objective | Fail Condition |
|------|-----------|----------------|
| Build | Compile/package app | Compile error, dependency issues |
| Lint | Keep code standards | Style or static check failures |
| Unit Tests | Validate behavior quickly | Test failure |
| Security | Catch obvious risks early | Secret or high severity finding |
| Package | Build immutable image | Docker build/push failure |
| Deploy | Release validated image | Health check or rollout failure |

---

## Example Stage Graph

```text
         +-----------+
         |   Build   |
         +-----------+
           /       \
          v         v
     +-------+   +-------+
     | Lint  |   | Tests |
     +-------+   +-------+
          \       /
           v     v
       +-----------+
       | Security  |
       +-----------+
             |
             v
       +-----------+
       | Package   |
       +-----------+
             |
             v
       +-----------+
       | Deploy    |
       +-----------+
```

---

## Minimal YAML Structure

```yaml
jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "build + lint + test + scan"

  package:
    needs: ci
    runs-on: ubuntu-latest
    steps:
      - run: echo "docker build/push"

  deploy:
    needs: package
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy by branch"
```

---

## Fast Feedback vs Full Validation

Recommended split:
- PR pipeline: fast checks, no deploy
- Branch pipeline (`dev`/`stage`/`main`): full validation and deploy path

Benefits:
- Faster developer feedback
- Reduced CI cost on feature branches
- Stable promotion on permanent branches

---

## Failure Handling Rules

If a stage fails:
1. Stop pipeline at first failing gate
2. Fix in the same branch context
3. Re-run only after root cause is clear
4. No manual bypass on protected branches

---

## Definition of Done for CI/CD

A change is "delivery ready" only if:
- Required CI gates pass
- Docker image is published and traceable
- Deploy target is known (`dev`, `stage`, or `prod`)
- Rollback target is known (previous tag/image)

---

[Previous: GitHub Actions Basics](./02-github-actions-basics.md) | [Index](./README.md) | [Next: Artifact Management](./04-artifact-management.md)
