[← Previous](./11-gitops-argocd.md) | [📋 Index](./README.md) | [Next →](./13-monitoring.md)

---

# Feature Flags

## What Are Feature Flags?

**Runtime toggles that control feature availability without deploying.**

```javascript
if (featureFlags.isEnabled('new-checkout')) {
  return <NewCheckout />;
} else {
  return <OldCheckout />;
}
```

Also called: Feature toggles, Feature switches

---

## Why Feature Flags?

| Problem | Solution |
|---------|----------|
| Risky big-bang releases | Gradual rollout |
| Slow rollback | Instant disable |
| QA in production | Enable for testers only |
| A/B testing | Different experiences |
| Trunk-based development | Ship incomplete features safely |

---

## Feature Flag as "Behavior Rollback"

```
Traditional Rollback:
  Redeploy old version (minutes)

Feature Flag Rollback:
  Disable flag (seconds)
```

**Fastest rollback mechanism for business logic!**

---

## Types of Feature Flags

| Type | Purpose | Lifetime |
|------|---------|----------|
| **Release** | Hide incomplete features | Days/weeks |
| **Experiment** | A/B testing | Weeks |
| **Ops** | Circuit breakers, kill switches | Permanent |
| **Permission** | User-specific features | Permanent |

---

## Targeting Options

```yaml
feature: new-dashboard

rules:
  # Percentage rollout
  - percentage: 10%

  # Specific users
  - users: ["user-123", "user-456"]

  # User attributes
  - attribute: plan
    value: enterprise

  # Environment
  - environment: staging
    enabled: true
```

---

## Feature Flag Tools

| Tool | Type |
|------|------|
| **LaunchDarkly** | SaaS, enterprise |
| **Unleash** | Open source |
| **Flagsmith** | Open source + SaaS |
| **ConfigCat** | SaaS |
| **Split** | SaaS |
| **DIY** | Config/env vars |

---

## Simple DIY Implementation

```javascript
// flags.json (or from API/DB)
{
  "new-checkout": {
    "enabled": true,
    "percentage": 25,
    "users": ["beta-tester-1"]
  }
}

// Usage
function isEnabled(flag, userId) {
  const config = flags[flag];
  if (!config.enabled) return false;
  if (config.users?.includes(userId)) return true;
  return Math.random() * 100 < config.percentage;
}
```

---

## Best Practices

| Practice | Why |
|----------|-----|
| Default to OFF for new features | Safe by default |
| Clean up old flags | Technical debt |
| Log flag evaluations | Debug issues |
| Test both states | Don't break old path |
| Document flags | Team awareness |

---

## Feature Flag Lifecycle

```
1. Create flag (disabled)
2. Deploy code behind flag
3. Enable for internal testing
4. Gradual rollout (10% → 50% → 100%)
5. Flag becomes permanent ON
6. Remove flag from code
7. Delete flag configuration
```

**Don't forget step 6-7!** Flag debt is real.


---

[← Previous](./11-gitops-argocd.md) | [📋 Index](./README.md) | [Next →](./13-monitoring.md)
