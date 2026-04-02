[← Previous](./09-rollback-decision-tree.md) | [📋 Index](./README.md) | [Next →](./11-gitops-argocd.md)

---

# Roll Forward vs Rollback

## Two Strategies for Recovery

```
        ROLLBACK                    ROLL FORWARD
           │                             │
           ▼                             ▼
   Return to previous            Create new fix and
    known-good state              move ahead
           │                             │
           ▼                             ▼
   v1.4.1 ← v1.4.2 (bad)        v1.4.2 (bad) → v1.4.3 (fix)
```

---

## When to Rollback

**Rollback is appropriate when:**
- Previous version is known to be stable
- No state/data dependencies
- Quick recovery needed
- Fix is complex or unknown

**Examples:**
- Redeploy old Docker image
- Revert traffic to blue
- Stop canary
- Sync older ArgoCD revision

---

## When to Roll Forward

**Roll forward is appropriate when:**
- Issue is small and fix is easy
- Rollback is riskier than patching
- Database/schema changes prevent rollback
- External side effects already happened

**Examples:**
- Quick hotfix commit
- Patch release v1.4.3
- Feature flag off + corrective commit

---

## Why Rollback Can Be Dangerous

### Easy to Rollback
- Stateless app code
- UI bugs
- Config changes
- Wrong container image

### Hard to Rollback
- Database migrations (schema changed)
- Destructive data changes
- Message queue side effects
- External API calls already made
- Cache invalidation issues
- Backward-incompatible contracts

---

## Roll Forward Pattern

```
v1.4.2 deployed (has bug)
        │
        ▼
Don't rollback (DB schema changed)
        │
        ▼
Create hotfix branch from main
        │
        ▼
Minimal fix, no extras
        │
        ▼
Fast-track review (1 approval)
        │
        ▼
Deploy v1.4.3 (fix)
```

---

## Safe Release Design

To make rollback safer, design for it:

| Practice | Why |
|----------|-----|
| Backward-compatible DB migrations | Old code can run with new schema |
| Expand/contract pattern | Add first, remove later |
| Feature flags | Disable without redeploy |
| Canary deployments | Limit blast radius |
| Immutable artifacts | Known-good versions available |
| Observability | Detect issues fast |

---

## The Expand/Contract Pattern

**For database schema changes:**

```
Phase 1: EXPAND
- Add new column (nullable)
- Deploy code that writes to both old and new
- Both v1 and v2 code work

Phase 2: MIGRATE
- Backfill data to new column
- Verify data integrity

Phase 3: CONTRACT
- Deploy code that only uses new column
- Remove old column
```

**This allows rollback at any phase!**


---

[← Previous](./09-rollback-decision-tree.md) | [📋 Index](./README.md) | [Next →](./11-gitops-argocd.md)
