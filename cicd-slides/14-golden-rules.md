[← Previous](./13-monitoring.md) | [📋 Index](./README.md) | [Next →](./15-iac-config-management.md)

---

# CI/CD Golden Rules

## Pipeline Rules

1. **Pipeline must pass before merge** — no exceptions
2. **Same artifact for all environments** — build once, deploy many
3. **Immutable versioned artifacts** — never overwrite tags
4. **Security scans in CI** — SAST, secrets, dependencies

---

## Deployment Rules

5. **Feature flags for risky changes** — instant rollback
6. **Canary or blue/green for production** — limit blast radius
7. **Monitor deployments actively** — watch metrics during rollout
8. **Keep rollback path clear** — know your last known good

---

## Rollback Rules

9. **Fastest safe mitigation first** — fix users, then fix Git
10. **Deployment rollback ≠ Code rollback** — do both as needed
11. **Roll forward if rollback is risky** — data/schema constraints
12. **In GitOps, fix Git not just cluster** — ArgoCD will override

---

## Environment Rules

13. **Environment parity** — same image, different config
14. **Never hardcode environment values** — use env vars/secrets
15. **Clean up ephemeral environments** — TTL or on PR close
16. **Protect production access** — minimal permissions

---

## Quick Reference: Rollback Hierarchy

```
1. Disable feature flag        (seconds)
2. Stop canary / switch traffic (seconds)
3. Blue/green switch back      (seconds)
4. Redeploy previous image     (minutes)
5. Revert Git + rebuild        (minutes)
6. Hotfix forward              (if rollback unsafe)
```

---

## Quick Reference: Deployment Checklist

```
□ Feature flagged (if risky)
□ Backward-compatible DB changes
□ Monitoring dashboard ready
□ Alert thresholds set
□ Previous artifact available
□ Rollback runbook reviewed
□ On-call notified
```

---

## The Compact Rollback Table

| Layer | Question | Tool | Action |
|-------|----------|------|--------|
| Code | What should exist? | Git | `git revert` |
| Artifact | What is safe? | Registry | Redeploy old tag |
| Deployment | What runs now? | K8s/Helm | `rollout undo` |
| Desired state | What should cluster be? | ArgoCD | Sync older revision |
| Traffic | What do users see? | LB/Ingress | Switch route |
| Behavior | What feature is on? | Flags | Disable flag |

---

## Remember

**"Rollback is not one action. It is a strategy for returning the system to a safe state, using the fastest safe lever at the correct layer."**

---

## Questions?

---

## Thank You!

**Key Takeaways:**
- Build once, deploy many
- Rollback at the right layer
- Feature flags are your friend
- Monitor everything
- GitOps = Git is truth


---

[← Previous](./13-monitoring.md) | [📋 Index](./README.md) | [Next →](./15-iac-config-management.md)
