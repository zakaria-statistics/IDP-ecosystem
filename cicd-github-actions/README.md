# CI/CD with GitHub Actions

Practical CI/CD fundamentals for our Internal Developer Platform (IDP) direction.

Audience:
- Developers
- Senior developers
- Tech leads

Scope:
- Fundamentals first (no advanced GitOps/canary topics in this deck)
- Realistic workflow for our stack and branch model
- GitHub Actions after GitLab CI migration

---

## Table of Contents

### Foundation
1. [What is CI/CD and Why Now](./01-what-is-cicd.md)
2. [GitHub Actions Basics](./02-github-actions-basics.md)
3. [Pipeline Stages](./03-pipeline-stages.md)

### Delivery Flow
4. [Artifact Management](./04-artifact-management.md)
5. [Environment Strategy](./05-environment-strategy.md)
6. [Workflow Examples](./06-workflow-examples.md)
7. [Golden Rules](./07-golden-rules.md)

### Operating Model
8. [IDP Vision and Golden Path](./08-idp-vision.md)
9. [Roles and Operating Model](./09-roles-and-operating-model.md)
10. [Release and Rollback Basics](./10-release-and-rollback-basics.md)
11. [GitLab to GitHub Actions Migration](./11-migration-from-gitlab.md)
12. [Adoption Roadmap](./12-adoption-roadmap.md)

---

## Our Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot (Java/Kotlin) |
| Frontend | Next.js (React) |
| AI/ML | Python (services and experiments) |
| Containers | Docker, Docker Compose |
| Registry | GitHub Container Registry (`ghcr.io`) |
| CI/CD | GitHub Actions |
| Runtime | Azure Kubernetes via Rancher |
| SCM | Git + GitHub |

---

## Existing Branch Model (Aligned)

```text
feature/* -> dev -> stage -> main
```

Protected branches:
- `dev`
- `stage`
- `main`

---

## Expected Outcomes

After this deck, the team should be able to:
- Explain our minimum CI/CD standard
- Read and maintain core GitHub Actions workflows
- Promote one immutable image from `dev` to `prod`
- Operate manual production approval and simple rollback

---

## Quick Start

Start with [slide 1](./01-what-is-cicd.md) and follow the navigation links.

---

Companion to:
- Git workflow deck in `git-workflow/`
- Future advanced CI/CD deck (GitOps, canary, feature flags)
