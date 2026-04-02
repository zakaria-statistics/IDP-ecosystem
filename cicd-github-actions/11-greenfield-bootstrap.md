[Previous: Release and Rollback Basics](./10-release-and-rollback-basics.md) | [Index](./README.md) | [Next: Adoption Roadmap](./12-adoption-roadmap.md)

---

# Greenfield Platform Bootstrap

## Objective

Start clean with a minimal, reliable CI/CD foundation on GitHub Actions.

Priority:
- Fast developer feedback
- Safe production releases
- Reusable standards for all services

---

## Bootstrap Sequence

1. Create repository baseline.
2. Add protected branches (`dev`, `stage`, `main`).
3. Add PR checks workflow.
4. Add build/push workflow for GHCR.
5. Add branch-based deploy workflow.
6. Configure production environment approval.

---

## Minimum Repository Baseline

Each service repo should include:
- `.github/workflows/` with CI/CD workflows
- `Dockerfile`
- Local `docker-compose.yml` (when applicable)
- Basic README run/deploy instructions
- Health endpoint or readiness check

---

## Branch Protection Baseline

For `dev`, `stage`, `main`:
- Require pull request
- Require passing status checks
- Block force push
- Block direct push

This enforces the same delivery contract across repositories.

---

## Environment Baseline

Create GitHub Environments:
- `development`
- `staging`
- `production`

For `production`:
- Required reviewers enabled
- Environment secrets configured
- Deployment URL documented

---

## Shared Workflow Strategy

Adopt one reusable workflow per service type:
- Spring service CI
- Next.js service CI
- Python service CI

Then allow repo-specific extension jobs when needed.

---

## Bootstrap Done Criteria

Platform baseline is done when:
- New repo can deploy `dev` within one PR merge
- `stage` and `prod` follow the same image promotion path
- Production deploy is approval-gated and auditable
- Rollback to previous image is documented and tested

---

[Previous: Release and Rollback Basics](./10-release-and-rollback-basics.md) | [Index](./README.md) | [Next: Adoption Roadmap](./12-adoption-roadmap.md)
