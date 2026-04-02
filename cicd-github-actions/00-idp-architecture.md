[Index](./README.md) | [Next: What is CI/CD and Why Now](./01-what-is-cicd.md)

---

# IDP Architecture, GitHub Actions Workflow, and CI/CD Patterns

## 1) Platform Context (System-Level View)

```text
                        +-----------------------+
                        |  Developers / TL / Sr |
                        +-----------+-----------+
                                    |
                                    v
+--------------------+    +-----------------------+    +----------------------+
|  Service Repos     | -> | GitHub (SCM + PR +    | -> | GitHub Actions       |
|  spring / next /   |    | branch protection)    |    | workflow execution   |
|  python services   |    +-----------------------+    +----------+-----------+
+--------------------+                                        |
                                                               v
                                                     +----------------------+
                                                     | GHCR (Docker images) |
                                                     +----------+-----------+
                                                                |
                                                                v
                                                    +------------------------+
                                                    | Azure K8s via Rancher  |
                                                    | namespaces: dev/stage/ |
                                                    | prod                   |
                                                    +-----------+------------+
                                                                |
                                                                v
                                                    +------------------------+
                                                    | Monitoring + Alerts    |
                                                    | logs/metrics/traces    |
                                                    +------------------------+
```

---

## 2) IDP Logical Building Blocks

```text
+--------------------------------------------------------------------------+
|                       Internal Developer Platform                         |
+--------------------+------------------+------------------+---------------+
| Golden Path        | Delivery Engine  | Governance       | Ops Feedback  |
| - templates        | - Actions        | - branch rules   | - dashboards  |
| - docs/runbooks    | - GHCR build     | - env approvals  | - alerts      |
| - starter repos    | - deploy jobs    | - required checks| - incidents   |
+--------------------+------------------+------------------+---------------+
          |                    |                  |                |
          +--------------------+------------------+----------------+
                                      |
                                      v
                         Faster and safer software delivery
```

---

## 3) GitHub Actions Event and Workflow Topology

```text
Events:
  pull_request (dev, stage, main)
  push         (dev, stage, main)
  workflow_dispatch (manual operations)

                 +-------------------------+
PR ------------> | PR Checks Workflow      |
                 | build/lint/test/scan    |
                 +------------+------------+
                              |
                              v
                         merge allowed

                 +-------------------------+
Push ----------> | Build Image Workflow    |
                 | docker build + push     |
                 +------------+------------+
                              |
                              v
                 +-------------------------+
                 | Deploy Workflow         |
                 | branch -> environment   |
                 +------------+------------+
                              |
             +----------------+------------------+
             |                |                  |
             v                v                  v
        deploy-dev       deploy-stage       deploy-prod
        auto             auto               manual approval
```

---

## 4) Branch to Environment Promotion Pattern

```text
feature/* -> PR -> dev -> stage -> main
               |      |       |      |
               |      |       |      +-> prod deploy (approval)
               |      |       +-> stage deploy (auto)
               |      +-> dev deploy (auto)
               +-> CI quality gates
```

Rules:
- No direct push to `dev`, `stage`, `main`
- Required status checks before merge
- `main` deployment requires environment approval

---

## 5) Build Once, Deploy Many Pattern

```text
Commit SHA
   |
   v
Build Docker image once
   |
   v
ghcr.io/org/service:sha-<commit>
   |
   +--> dev deployment
   +--> stage deployment
   +--> prod deployment
```

Do not rebuild per environment.
Only configuration and secrets differ by environment.

---

## 6) Multi-Service Pattern (Spring / Next / Python)

```text
                    +-----------------------------+
                    | Mono-repo or multi-repo    |
                    +-------------+---------------+
                                  |
                 +----------------+----------------+
                 |                |                |
                 v                v                v
            backend/         frontend/           ai/
            spring           next.js             python
                 |                |                |
                 +--------+-------+-------+--------+
                          |               |
                          v               v
                   service-specific CI    shared policy checks
                          |
                          v
                    per-service image tags in GHCR
```

Use path filters to avoid running all jobs for unrelated changes.

---

## 7) Security and Secret Flow Pattern

```text
GitHub Secrets/Environments ---> Workflow runtime env vars ---> Deploy target
         |
         +--> never committed to source code
         +--> environment scope (dev/stage/prod)
         +--> rotated if leaked
```

Permission baseline:
- `contents: read`
- `packages: write` only where needed

---

## 8) Release and Rollback Pattern

```text
Release:
  stage validated
    -> tag main (vX.Y.Z)
    -> deploy approved image to prod

Incident:
  detect
    -> redeploy previous known-good image
    -> verify smoke checks
    -> fix in git and re-promote
```

```text
prod issue
   |
   v
rollback image tag (fast mitigation)
   |
   v
root cause fix in code (durable correction)
```

---

## 9) Recommended File Layout for Platform Repos

```text
.github/
  workflows/
    pr-checks.yml
    build-image.yml
    deploy.yml
    security.yml
docs/
  runbooks/
    deploy.md
    rollback.md
platform/
  templates/
    spring-service/
    next-service/
    python-service/
```

---

## 10) Minimum Viable IDP Contract

```text
Every service must have:
  - standard PR checks
  - container build and immutable tags
  - branch-based deployment mapping
  - production approval gate
  - rollback runbook
```

This is the baseline architecture for your fresh GitHub Actions start.

---

[Index](./README.md) | [Next: What is CI/CD and Why Now](./01-what-is-cicd.md)
