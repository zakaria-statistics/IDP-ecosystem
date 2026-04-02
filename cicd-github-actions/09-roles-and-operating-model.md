[Previous: IDP Vision and Golden Path](./08-idp-vision.md) | [Index](./README.md) | [Next: Release and Rollback Basics](./10-release-and-rollback-basics.md)

---

# Roles and Operating Model

## Why Role Clarity Matters

CI/CD reliability fails when ownership is unclear.

This model defines decision rights for:
- Developers
- Senior developers
- Tech leads
- Platform contributors

---

## Responsibility Matrix

| Area | Developer | Senior | Tech Lead | Platform |
|------|-----------|--------|-----------|----------|
| Application code | Owns | Reviews complex parts | Oversees | Supports standards |
| Service tests | Owns | Defines strategy | Enforces quality bar | Supports templates |
| Workflow file updates | Proposes | Reviews correctness | Approves risky changes | Maintains shared flows |
| Production release | Supports | Validates readiness | Approves/go-no-go | Supports execution |
| Incident rollback | Supports | Drives fix plan | Owns final decision | Supports platform recovery |

---

## Pull Request Rules by Branch

| Target Branch | Minimum Approval | Additional Rule |
|--------------|------------------|-----------------|
| `dev` | 1 reviewer | CI must pass |
| `stage` | 2 reviewers | Senior review required |
| `main` | 2 reviewers | Tech lead approval required |

No direct commit on protected branches.

---

## Release Flow Ownership

```text
Developer:
  merge feature -> dev

Senior:
  validate stage readiness

Tech Lead:
  approve production deployment

Platform:
  ensure workflow and environment reliability
```

---

## Incident Handling (Simple Model)

1. Detect issue (alerts, logs, user reports).
2. Assign incident owner (tech lead or delegated senior).
3. Decide rollback or fix-forward.
4. Execute through workflow (not ad-hoc shell history).
5. Record learning and improve guardrails.

---

## Definition of Platform Success

Operational indicators:
- Median PR-to-dev lead time decreases
- Deployment frequency increases safely
- Change failure rate decreases
- Mean time to recovery improves

These are team outcomes, not only CI metrics.

---

[Previous: IDP Vision and Golden Path](./08-idp-vision.md) | [Index](./README.md) | [Next: Release and Rollback Basics](./10-release-and-rollback-basics.md)
