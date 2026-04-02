[Previous: Golden Rules](./07-golden-rules.md) | [Index](./README.md) | [Next: Roles and Operating Model](./09-roles-and-operating-model.md)

---

# IDP Vision and Golden Path

## What We Mean by Internal Developer Platform

An Internal Developer Platform (IDP) gives teams:
- A clear standard way to build, test, release, and operate services
- Reusable automation and templates
- Faster delivery with fewer platform-level decisions per feature

---

## Why CI/CD is the First IDP Layer

Without CI/CD standards:
- Every repo behaves differently
- Quality bar is inconsistent
- Release risk increases

With CI/CD standards:
- Delivery becomes predictable
- Governance is easier
- Onboarding time decreases

---

## Our Golden Path

```text
Create service from template
  -> use standard workflows
  -> pass quality gates
  -> build/push image to ghcr
  -> auto deploy dev/stage
  -> approved deploy to prod
```

Teams can extend this path, but should not break core controls.

---

## Platform Contract (Minimum Standard)

Every service should provide:
- Build command
- Test command
- Container build definition
- Health endpoint/check
- Deployment manifest/chart values

Every service should consume:
- Shared workflow templates
- Standard tagging/versioning rules
- Standard branch protection rules

---

## Standardization vs Flexibility

Standardized:
- Branch protections
- Required CI gates
- Image tagging and traceability
- Prod approval process

Flexible:
- Runtime/framework (Spring, Next.js, Python)
- Repo-specific extra checks
- Service internals

---

## What We Intentionally Defer

Not in phase 1:
- Advanced traffic splitting (canary/blue-green)
- Full GitOps rollout automation
- Complex progressive delivery policies

Reason:
- We first need stable fundamentals and team adoption.

---

[Previous: Golden Rules](./07-golden-rules.md) | [Index](./README.md) | [Next: Roles and Operating Model](./09-roles-and-operating-model.md)
