[Previous: Workflow Examples](./06-workflow-examples.md) | [Index](./README.md) | [Next: IDP Vision and Golden Path](./08-idp-vision.md)

---

# Golden Rules

## Quality Gate Rules

1. CI must pass before merge on protected branches.
2. No direct pushes to `dev`, `stage`, `main`.
3. PR reviews are mandatory for protected branches.
4. Security and secret checks are not optional.

---

## Artifact Rules

5. Build once, deploy many.
6. Use immutable image tags (SHA and/or semver).
7. Never deploy `latest` to production.
8. Every deployment must be traceable to a commit.

---

## Deployment Rules

9. `dev` and `stage` deploy automatically after successful pipeline.
10. `prod` deployment always requires explicit approval.
11. Rollback target must be known before production release.
12. Deploy and rollback steps must be documented in repo.

---

## Secrets and Access Rules

13. No secrets in code, logs, or Docker images.
14. Use environment secrets for environment-specific credentials.
15. Keep workflow permissions minimal.
16. Rotate leaked credentials immediately.

---

## Team Operating Rules

17. Shared workflows are versioned and owned.
18. Every service follows the same minimum CI/CD contract.
19. Incidents are handled with runbooks, not improvisation.
20. Post-incident improvements are tracked and implemented.

---

## Quick Release Checklist

```text
[ ] PR approved and merged
[ ] CI/CD pipeline green
[ ] Image tag recorded (SHA/semver)
[ ] Stage validation completed
[ ] Production approver available
[ ] Rollback target identified
```

---

## Common Anti-Patterns

| Anti-Pattern | Risk | Better Approach |
|-------------|------|-----------------|
| Giant workflow file for everything | Hard maintenance | Split by concern (checks/build/deploy) |
| Copy-pasted workflows across repos | Drift and inconsistency | Reusable workflows |
| Manual prod commands from laptops | No auditability | GitHub Environment + workflow approvals |
| Rebuilding image per environment | Inconsistent releases | Build once, promote same image |

---

[Previous: Workflow Examples](./06-workflow-examples.md) | [Index](./README.md) | [Next: IDP Vision and Golden Path](./08-idp-vision.md)
