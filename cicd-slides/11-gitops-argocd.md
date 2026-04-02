[← Previous](./10-roll-forward.md) | [📋 Index](./README.md) | [Next →](./12-feature-flags.md)

---

# GitOps & ArgoCD

## What is GitOps?

**Git as the single source of truth for infrastructure and deployments.**

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│     Git      │────────►│   ArgoCD     │────────►│  Kubernetes  │
│   (desired   │ watches │  (operator)  │  syncs  │   (actual    │
│    state)    │         │              │         │    state)    │
└──────────────┘         └──────────────┘         └──────────────┘
```

**Principle:** If it's not in Git, it shouldn't be in the cluster.

---

## GitOps vs Traditional CI/CD

| Traditional | GitOps |
|-------------|--------|
| CI/CD pushes to cluster | ArgoCD pulls from Git |
| Pipeline has cluster access | Only ArgoCD has access |
| Imperative: "do this" | Declarative: "be this" |
| Drift possible | Continuous reconciliation |

---

## ArgoCD Core Concepts

```
┌─────────────────────────────────────────────────┐
│                  ArgoCD                         │
│                                                 │
│  Application:                                   │
│  ┌─────────────────────────────────────────┐   │
│  │ name: myapp                             │   │
│  │ source: git@github.com:org/k8s-manifests│   │
│  │ destination: kubernetes cluster          │   │
│  │ sync: automatic                          │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## ArgoCD Sync Strategies

| Strategy | Behavior |
|----------|----------|
| **Manual** | Click to sync |
| **Auto Sync** | Sync on Git change |
| **Self Heal** | Revert manual kubectl changes |
| **Prune** | Delete resources removed from Git |

```yaml
syncPolicy:
  automated:
    prune: true
    selfHeal: true
```

---

## GitOps Rollback

### ⚠️ Important Nuance

With GitOps, **manual kubectl rollback may be overwritten by ArgoCD**.

```
You: kubectl rollout undo deployment/myapp
ArgoCD: "Git says v1.4.2, I'll sync back to v1.4.2"
```

### Correct Approach

1. Revert commit in Git (or sync older revision)
2. ArgoCD sees change
3. ArgoCD syncs cluster to match Git

---

## ArgoCD Rollback Methods

### Method 1: Sync to older Git revision
```bash
argocd app sync myapp --revision <older-commit-sha>
```

### Method 2: Revert in Git
```bash
git revert <bad-commit>
git push
# ArgoCD auto-syncs
```

### Method 3: ArgoCD UI
- History tab → Select previous revision → Rollback

---

## GitOps Repo Structure

```
k8s-manifests/
├── apps/
│   ├── myapp/
│   │   ├── base/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── kustomization.yaml
│   │   └── overlays/
│   │       ├── dev/
│   │       ├── stage/
│   │       └── prod/
│   └── another-app/
└── argocd/
    └── applications.yaml
```

---

## CI/CD + GitOps Flow

```
1. Developer pushes code to app repo
2. CI builds and tests
3. CI pushes Docker image to registry
4. CI updates image tag in GitOps repo
5. ArgoCD detects GitOps repo change
6. ArgoCD syncs cluster with new image
```

```yaml
# CI job updates GitOps repo
- name: Update manifest
  run: |
    cd k8s-manifests
    sed -i "s|image:.*|image: myapp:${NEW_TAG}|" apps/myapp/deployment.yaml
    git commit -am "Deploy myapp:${NEW_TAG}"
    git push
```


---

[← Previous](./10-roll-forward.md) | [📋 Index](./README.md) | [Next →](./12-feature-flags.md)
