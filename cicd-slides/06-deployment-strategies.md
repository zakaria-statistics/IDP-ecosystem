[← Previous](./05-ephemeral-environments.md) | [📋 Index](./README.md) | [Next →](./07-blue-green-canary.md)

---

# Deployment Strategies

## Why Deployment Strategies Matter

**Goal:** Deploy new versions with minimal risk and downtime.

| Concern | Strategy helps with |
|---------|---------------------|
| Downtime | Zero-downtime deploys |
| Risk | Limit blast radius |
| Rollback | Quick recovery path |
| Validation | Test in production safely |

---

## Overview of Strategies

```
┌────────────────────────────────────────────────────────────┐
│                  DEPLOYMENT STRATEGIES                     │
├──────────────┬──────────────┬──────────────┬──────────────┤
│   Recreate   │   Rolling    │  Blue/Green  │    Canary    │
│              │   Update     │              │              │
│  (downtime)  │ (gradual)    │  (instant    │  (gradual    │
│              │              │   switch)    │   traffic)   │
└──────────────┴──────────────┴──────────────┴──────────────┘
        ▲              ▲              ▲              ▲
     Simple        Default        Fast           Safest
                  K8s/Docker    rollback       validation
```

---

## 1. Recreate (Big Bang)

**Stop old → Start new**

```
v1 ████████████  (stop all)
                   ↓
v2                 ████████████  (start all)

   ─────── downtime ───────
```

| Pros | Cons |
|------|------|
| Simple | Downtime |
| Clean state | No gradual validation |

**Use when:** Downtime is acceptable, incompatible versions.

---

## 2. Rolling Update

**Gradually replace instances**

```
v1 ████████████
v1 ████████     v2 ████
v1 ████         v2 ████████
               v2 ████████████

   ─── zero downtime ───
```

| Pros | Cons |
|------|------|
| Zero downtime | Slow rollback |
| Default in K8s | Mixed versions temporarily |

**Use when:** Standard deployments, stateless apps.

---

## 3. Blue/Green

**Two identical environments, instant traffic switch**

```
        BEFORE                    AFTER

┌─────────────┐             ┌─────────────┐
│  BLUE (v1)  │◄── traffic  │  BLUE (v1)  │
└─────────────┘             └─────────────┘

┌─────────────┐             ┌─────────────┐
│ GREEN (v2)  │  (standby)  │ GREEN (v2)  │◄── traffic
└─────────────┘             └─────────────┘
```

| Pros | Cons |
|------|------|
| Instant rollback | Double infrastructure cost |
| Zero downtime | DB schema complexity |

---

## 4. Canary

**Gradual traffic shift to new version**

```
v1 ████████████████████  (99%)
v2 █                     (1%)

v1 ████████████████      (80%)
v2 █████                 (20%)

v1                       (0%)
v2 █████████████████████ (100%)
```

| Pros | Cons |
|------|------|
| Safest validation | Complex setup |
| Limited blast radius | Requires traffic splitting |

---

## Comparison Table

| Strategy | Downtime | Rollback Speed | Risk | Complexity |
|----------|----------|----------------|------|------------|
| Recreate | Yes | Slow | High | Low |
| Rolling | No | Medium | Medium | Low |
| Blue/Green | No | Instant | Low | Medium |
| Canary | No | Fast | Lowest | High |


---

[← Previous](./05-ephemeral-environments.md) | [📋 Index](./README.md) | [Next →](./07-blue-green-canary.md)
