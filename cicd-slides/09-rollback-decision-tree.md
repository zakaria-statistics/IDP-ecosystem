[← Previous](./08-rollback-layers.md) | [📋 Index](./README.md) | [Next →](./10-roll-forward.md)

---

# Rollback Decision Tree

## When Incident Happens

```
Bad change detected
        │
        ▼
┌───────────────────────────────────────┐
│  Is the problem user-visible/urgent?  │
└───────────────────────────────────────┘
        │
    YES │
        ▼
┌───────────────────────────────────────┐
│  FASTEST SAFE MITIGATION              │
│                                       │
│  1. Disable feature flag              │
│  2. Stop canary / switch traffic      │
│  3. Blue/Green switch back            │
│  4. Redeploy previous image           │
└───────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────┐
│  RESTORE CONSISTENCY                  │
│                                       │
│  5. Revert bad commit in Git          │
│  6. Run pipeline / rebuild            │
│  7. Fix ArgoCD desired state          │
└───────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────┐
│  IF ROLLBACK UNSAFE (data/state)      │
│                                       │
│  8. Roll forward with hotfix          │
└───────────────────────────────────────┘
```

---

## Decision Questions

### Q1: Is the problem user-visible and urgent?

**YES →** Choose fastest safe mitigation
**NO →** Can fix in normal workflow

### Q2: Is the deployed artifact bad?

**YES →** Deployment rollback (redeploy previous image)

### Q3: Is Git/source of truth wrong?

**YES →** Revert commit, run pipeline

### Q4: Is rollback dangerous due to state changes?

**YES →** Prefer roll forward or partial mitigation

### Q5: Are you using GitOps?

**YES →** Fix Git/ArgoCD desired state, not just live cluster

---

## Hierarchy of Preferred Actions

### Fastest Mitigation (do first)
1. Disable feature flag
2. Stop canary / route traffic back
3. Blue/Green switch
4. Redeploy previous artifact

### Restore Consistency (do after)
5. Revert bad commit in Git
6. Run new pipeline
7. Update ArgoCD/GitOps state

### If Rollback Unsafe
8. Hotfix forward instead

---

## Real Scenarios

### Scenario 1: Bad frontend release
```
Action: Redeploy previous image → Then revert commit
```

### Scenario 2: Canary shows 5xx spike
```
Action: Stop canary immediately → Route 100% to stable → Investigate
```

### Scenario 3: Feature causes business issue (app stable)
```
Action: Turn off feature flag → Keep deployment → Fix later
```

### Scenario 4: Bad commit merged but not deployed yet
```
Action: Revert in Git before deployment → No prod rollback needed
```

### Scenario 5: Destructive DB migration deployed
```
Action: DO NOT blindly rollback app → Assess schema compatibility → May need forward fix
```

---

## Key Principle

**Rollback is not one action.**

It is a strategy for returning the system to a safe state, using the **fastest safe lever at the correct layer**.

Actions are often **sequentially complementary**:
- Rollback deployment NOW
- Revert commit NEXT
- Run pipeline to restore consistency


---

[← Previous](./08-rollback-layers.md) | [📋 Index](./README.md) | [Next →](./10-roll-forward.md)
