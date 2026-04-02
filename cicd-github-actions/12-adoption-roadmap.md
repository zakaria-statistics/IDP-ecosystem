[Previous: GitLab to GitHub Actions Migration](./11-migration-from-gitlab.md) | [Index](./README.md)

---

# Adoption Roadmap

## Phase 1 (Weeks 1-4): Standard Baseline

Deliver:
- Branch protections on `dev`, `stage`, `main`
- Mandatory PR checks
- Docker build and push to GHCR
- Auto deploy `dev` and `stage`
- Manual approval for `prod`

Success signal:
- First services running fully through GitHub Actions flow

---

## Phase 2 (Weeks 5-8): Consolidation

Deliver:
- Shared reusable workflows for common service types
- Consistent tagging and release metadata
- Runbooks for deploy and rollback
- Basic dashboards for deploy health

Success signal:
- New services onboard with minimal workflow customization

---

## Phase 3 (Weeks 9-12): Reliability and Scale

Deliver:
- Rollback drills in stage
- Pipeline performance tuning (cache, path filters)
- Security baseline hardening
- Formal ownership and SLO review rhythm

Success signal:
- Reduced incident recovery time and stable deployment cadence

---

## Training Plan by Role

Developers:
- Daily PR checks and troubleshooting

Seniors:
- Pipeline design reviews and stage readiness

Tech leads:
- Approval policy, release decisions, incident command

Platform contributors:
- Reusable workflows, governance, observability

---

## KPIs to Track

Track monthly:
- Deployment frequency
- Lead time from merge to production
- Change failure rate
- Mean time to recovery
- Percentage of services on standard workflows

---

## Next Topics After Fundamentals

When phase 1-3 are stable, then move to:
1. Progressive delivery (canary/blue-green)
2. GitOps promotion model
3. Feature flag governance
4. Ephemeral preview environments

---

[Previous: GitLab to GitHub Actions Migration](./11-migration-from-gitlab.md) | [Index](./README.md)
