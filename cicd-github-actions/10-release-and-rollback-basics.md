[Previous: Roles and Operating Model](./09-roles-and-operating-model.md) | [Index](./README.md) | [Next: GitLab to GitHub Actions Migration](./11-migration-from-gitlab.md)

---

# Release and Rollback Basics

## Release Baseline

Production releases should be:
- Tagged (semver)
- Traceable to one commit SHA
- Deployable from one immutable image
- Approved through GitHub Environment

---

## Semantic Versioning

```text
vMAJOR.MINOR.PATCH

v1.3.0  -> new feature release
v1.3.1  -> bugfix/hotfix release
v2.0.0  -> breaking change
```

Use tags on `main` to mark production releases.

---

## Simple Release Procedure

```bash
git checkout main
git pull origin main
git tag -a v1.4.0 -m "Release v1.4.0"
git push origin v1.4.0
```

Then deploy the matching image tag through the approved workflow.

---

## Rollback Layers (Fundamental)

Layer 1:
- Deployment rollback (redeploy previous known-good image)

Layer 2:
- Source rollback (revert commit and redeploy)

For phase 1, prioritize deployment rollback first because it is faster.

---

## Rollback Decision Guide

```text
Is production impacted now?
  yes -> redeploy last known-good image immediately
  no  -> evaluate fix-forward vs scheduled rollback

After mitigation:
  revert/fix in Git
  validate in stage
  redeploy safely
```

---

## What Must Be Ready Before Prod Deploy

```text
[ ] previous stable image tag is recorded
[ ] runbook link exists
[ ] monitoring dashboard is available
[ ] approver is available
[ ] smoke test plan is clear
```

---

## Common Rollback Mistakes

| Mistake | Consequence | Correct Practice |
|--------|-------------|------------------|
| No record of previous image | Slow recovery | Store release metadata per deploy |
| Rebuild old code during incident | Non-reproducible rollback | Reuse previously published image |
| Rollback without post-checks | Hidden ongoing failure | Run smoke checks after rollback |

---

[Previous: Roles and Operating Model](./09-roles-and-operating-model.md) | [Index](./README.md) | [Next: GitLab to GitHub Actions Migration](./11-migration-from-gitlab.md)
