[← Previous](./06-deployment-strategies.md) | [📋 Index](./README.md) | [Next →](./08-rollback-layers.md)

---

# Blue/Green & Canary Deep Dive

## Blue/Green Deployment

### How It Works

```
                    Load Balancer
                         │
            ┌────────────┴────────────┐
            │                         │
            ▼                         ▼
    ┌──────────────┐         ┌──────────────┐
    │     BLUE     │         │    GREEN     │
    │     (v1)     │         │     (v2)     │
    │   ACTIVE     │         │   STANDBY    │
    └──────────────┘         └──────────────┘
```

**Rollback = Switch traffic back to Blue**

---

### Blue/Green Flow

```
1. Blue running v1 (receives traffic)
2. Deploy v2 to Green (no traffic)
3. Test Green internally
4. Switch traffic: Blue → Green
5. Green is now active (v2)
6. Blue becomes standby (keep for rollback)
7. Later: deploy v3 to Blue, repeat
```

---

### Blue/Green Challenges

| Challenge | Solution |
|-----------|----------|
| Database schema | Use backward-compatible migrations |
| Session state | Externalize to Redis/DB |
| Long-running requests | Drain connections before switch |
| Double cost | Use auto-scaling, spot instances |

---

## Canary Deployment

### How It Works

```
                    Load Balancer
                         │
                   Traffic Split
                    /         \
                   /           \
            95% traffic     5% traffic
                 │               │
                 ▼               ▼
         ┌────────────┐  ┌────────────┐
         │  STABLE    │  │   CANARY   │
         │   (v1)     │  │    (v2)    │
         └────────────┘  └────────────┘
```

---

### Canary Rollout Stages

```
Stage 1:  v1 ████████████████████  v2 █          (5%)
              Monitor metrics...

Stage 2:  v1 ████████████████      v2 █████      (25%)
              Monitor metrics...

Stage 3:  v1 ████████              v2 ██████████ (50%)
              Monitor metrics...

Stage 4:  v1                       v2 ████████████████████ (100%)
              Full rollout!
```

**At any stage:** If errors spike → abort → 100% back to v1

---

### Canary Metrics to Watch

| Metric | Threshold |
|--------|-----------|
| Error rate (5xx) | < 1% |
| Latency (p99) | < 500ms |
| CPU/Memory | Normal range |
| Business metrics | Conversion, revenue |

```yaml
# Argo Rollouts example
analysis:
  metrics:
    - name: error-rate
      successCondition: result < 0.01
      provider:
        prometheus:
          query: |
            sum(rate(http_errors[5m])) / sum(rate(http_requests[5m]))
```

---

## Canary vs A/B Testing

| Aspect | Canary | A/B Testing |
|--------|--------|-------------|
| **Purpose** | Validate stability | Compare business outcomes |
| **Traffic split** | Gradual rollout | Fixed split |
| **Metrics** | Errors, latency | Conversion, engagement |
| **Duration** | Hours/days | Days/weeks |
| **Outcome** | Ship or rollback | Winner variant |

**Canary:** "Is v2 stable?"
**A/B:** "Does button color A convert better than B?"

---

## Tools for Traffic Splitting

| Tool | How |
|------|-----|
| **Kubernetes Ingress** | Weighted backends |
| **Istio** | VirtualService traffic split |
| **Argo Rollouts** | Native canary support |
| **AWS ALB** | Weighted target groups |
| **Nginx** | Upstream weights |


---

[← Previous](./06-deployment-strategies.md) | [📋 Index](./README.md) | [Next →](./08-rollback-layers.md)
