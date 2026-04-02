[← Previous](./12-feature-flags.md) | [📋 Index](./README.md) | [Next →](./14-golden-rules.md)

---

# Monitoring & Observability

## The Three Pillars

```
┌─────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY                            │
├───────────────────┬───────────────────┬────────────────────┤
│      LOGS         │     METRICS       │      TRACES        │
│                   │                   │                    │
│  What happened    │   How much/many   │   Request flow     │
│  (events)         │   (numbers)       │   (distributed)    │
└───────────────────┴───────────────────┴────────────────────┘
```

---

## Logs

**Discrete events with context.**

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "ERROR",
  "service": "payment",
  "message": "Payment failed",
  "user_id": "123",
  "error": "Card declined",
  "trace_id": "abc-xyz"
}
```

**Tools:** ELK Stack, Loki, CloudWatch Logs

---

## Metrics

**Numeric measurements over time.**

```
# Counter
http_requests_total{status="200"} 15234

# Gauge
memory_usage_bytes 1073741824

# Histogram
http_request_duration_seconds_bucket{le="0.5"} 2341
```

**Tools:** Prometheus, Grafana, Datadog

---

## Traces

**Request path across services.**

```
Request abc-xyz:
├── API Gateway (2ms)
│   └── Auth Service (15ms)
│       └── User DB (5ms)
└── Payment Service (120ms)
    └── Stripe API (95ms)
```

**Tools:** Jaeger, Zipkin, AWS X-Ray

---

## Key Metrics for Deployments

### The Four Golden Signals

| Signal | What to Watch |
|--------|---------------|
| **Latency** | p50, p95, p99 response times |
| **Traffic** | Requests per second |
| **Errors** | Error rate (5xx / total) |
| **Saturation** | CPU, memory, connections |

---

## Deployment Monitoring

```
Before deploy:
  Baseline: error_rate = 0.1%, latency_p99 = 200ms

During canary:
  Watch: error_rate, latency_p99, business metrics

Alert thresholds:
  error_rate > 1% → ALERT
  latency_p99 > 500ms → ALERT
```

---

## Alerting Strategy

| Severity | Response | Example |
|----------|----------|---------|
| **Critical** | Page on-call NOW | Service down, data loss |
| **Warning** | Check within hours | Error rate elevated |
| **Info** | Review next day | Deployment completed |

```yaml
# Prometheus alert rule
- alert: HighErrorRate
  expr: rate(http_errors_total[5m]) / rate(http_requests_total[5m]) > 0.01
  for: 5m
  labels:
    severity: critical
```

---

## Dashboards for Deployments

**Essential panels:**

```
┌──────────────────────────────────────────────────────────┐
│                  DEPLOYMENT DASHBOARD                    │
├────────────────────────┬─────────────────────────────────┤
│   Error Rate (%)       │   Latency p99 (ms)              │
│   📈 0.5%              │   📈 180ms                       │
├────────────────────────┼─────────────────────────────────┤
│   Requests/sec         │   Pod Status                    │
│   📈 1.2k              │   ✅ 5/5 Ready                   │
├────────────────────────┼─────────────────────────────────┤
│   CPU Usage            │   Memory Usage                  │
│   📈 45%               │   📈 2.1 GB                      │
└────────────────────────┴─────────────────────────────────┘
│        Current Version: myapp:1.4.2 | Deployed: 10m ago │
└──────────────────────────────────────────────────────────┘
```


---

[← Previous](./12-feature-flags.md) | [📋 Index](./README.md) | [Next →](./14-golden-rules.md)
