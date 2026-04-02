[← Previous](./07-blue-green-canary.md) | [📋 Index](./README.md) | [Next →](./09-rollback-decision-tree.md)

---

# Rollback Layers

## There Is Not One Rollback

**"Rollback" means different things at different layers.**

```
┌─────────────────────────────────────────────────────────────┐
│                    ROLLBACK LAYERS                          │
├─────────────┬─────────────┬─────────────┬──────────────────┤
│    CODE     │  ARTIFACT   │ DEPLOYMENT  │     TRAFFIC      │
│             │             │             │                  │
│ Git commit  │Docker image │ What runs   │ What users see   │
└─────────────┴─────────────┴─────────────┴──────────────────┘
```

---

## Layer 1: Code Rollback

**Question:** "Which source code version should be truth?"

| Action | When to Use |
|--------|-------------|
| `git revert` | Bad commit merged, need audit trail |
| `git reset` | Local cleanup only, never on shared branches |

```bash
# Create new commit that undoes a previous commit
git revert <bad-commit-sha>
git push
```

**Note:** Reverting in Git doesn't instantly fix prod. CI must rebuild and redeploy.

---

## Layer 2: Artifact Rollback

**Question:** "Which built version is safe to deploy?"

```
myapp:1.4.1  ← known good
myapp:1.4.2  ← bad (current)

Solution: Redeploy myapp:1.4.1
```

**Why this matters:**
- Prod runs artifacts, not Git commits
- Keep immutable versioned images
- Never rely only on "latest"

---

## Layer 3: Deployment Rollback

**Question:** "What should actually run in the cluster?"

| Tool | Command |
|------|---------|
| Kubernetes | `kubectl rollout undo deployment/myapp` |
| Helm | `helm rollback myapp 1` |
| ArgoCD | Sync to older revision |
| Docker Compose | `docker-compose up -d --force-recreate` |

```bash
# Kubernetes rollback
kubectl rollout undo deployment/myapp

# Helm rollback to revision 1
helm rollback myapp 1
```

---

## Layer 4: Traffic Rollback

**Question:** "Which version should users receive traffic to?"

| Strategy | Rollback Action |
|----------|-----------------|
| Blue/Green | Switch traffic back to Blue |
| Canary | Stop canary, route 100% to stable |
| Feature flag | Disable flag |

**Often the fastest rollback from user perspective!**

---

## The Layers Are Related but Different

```
Git commit "abc123"
     │
     ▼ (CI builds)
Docker image "myapp:1.4.2"
     │
     ▼ (CD deploys)
Kubernetes Deployment running "myapp:1.4.2"
     │
     ▼ (traffic routing)
Users receiving traffic to pods
```

You can rollback at ANY of these points.

---

## Mapping Tools to Layers

| Layer | Question | Tool | Action |
|-------|----------|------|--------|
| Code | What should exist? | Git | `git revert` |
| Artifact | What is safe? | Registry | Redeploy old tag |
| Deployment | What runs now? | K8s/Helm | `rollout undo` |
| Desired state | What should cluster be? | ArgoCD | Sync older revision |
| Traffic | What do users see? | LB/Ingress | Switch route |
| Behavior | What feature is on? | Feature flags | Disable flag |


---

[← Previous](./07-blue-green-canary.md) | [📋 Index](./README.md) | [Next →](./09-rollback-decision-tree.md)
