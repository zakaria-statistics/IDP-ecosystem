[Previous: Release and Rollback Basics](./10-release-and-rollback-basics.md) | [Index](./README.md) | [Next: Adoption Roadmap](./12-adoption-roadmap.md)

---

# GitLab to GitHub Actions Migration

## Migration Goal

Shift CI/CD execution to GitHub Actions while preserving delivery behavior.

Priority:
- Keep developer experience simple
- Avoid release disruption
- Standardize where possible

---

## Concept Mapping

| GitLab CI | GitHub Actions |
|----------|----------------|
| `.gitlab-ci.yml` | `.github/workflows/*.yml` |
| stages | jobs + `needs` |
| runners | runners |
| variables | secrets/variables/environments |
| manual job | environment approval or `workflow_dispatch` |

---

## Recommended Migration Sequence

1. Port PR quality checks first.
2. Port image build and GHCR push second.
3. Port dev/stage deployment jobs third.
4. Port production deploy with approvals last.

This reduces blast radius during transition.

---

## Validation Rules During Transition

Until confidence is high:
- Compare old and new pipeline outputs
- Confirm image tags and deploy targets match
- Run stage deployments from GitHub Actions before prod cutover

---

## Typical Migration Risks

| Risk | Mitigation |
|------|------------|
| Missing secret/variable mapping | Inventory all CI variables before cutover |
| Different runner behavior | Pin versions and reproduce runtime setup |
| Inconsistent branch trigger logic | Use explicit branch/event conditions |
| Permission errors to GHCR | Set `packages: write` permissions in workflow |

---

## Done Criteria for Migration

Migration is done when:
- Required services deploy via GitHub Actions
- Branch protections enforce GitHub status checks
- Production deployments use GitHub Environment approvals
- Team runbooks reference GitHub workflow IDs/links

---

[Previous: Release and Rollback Basics](./10-release-and-rollback-basics.md) | [Index](./README.md) | [Next: Adoption Roadmap](./12-adoption-roadmap.md)
