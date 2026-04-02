[Previous: Workflow Examples](./06-workflow-examples.md) | [Index](./README.md)

---

# CI/CD Golden Rules

## Pipeline Rules

1. **Pipeline must pass before merge** - no exceptions
2. **Same image for all environments** - build once, deploy many
3. **Never overwrite image tags** - immutable versions
4. **Run security scans in CI** - catch vulnerabilities early

---

## Deployment Rules

5. **Manual approval for production** - always
6. **Environment parity** - same image, different config
7. **Never hardcode secrets** - use GitHub Secrets
8. **Keep rollback path clear** - know your previous version

---

## Branch Rules

9. **Protect `main`, `stage`, `dev`** - require PR reviews
10. **CI must pass before merge** - use branch protection
11. **No direct commits to protected branches** - always PR
12. **Clean up feature branches** - delete after merge

---

## Quick Reference: Deployment Checklist

```
[ ] Tests passing
[ ] Code reviewed and approved
[ ] Environment secrets configured
[ ] Previous version tagged/known
[ ] Monitoring dashboard ready
[ ] On-call team notified (for prod)
```

---

## Quick Reference: Rollback

If something goes wrong in production:

```
1. Redeploy previous image tag     (fastest)
2. Revert Git commit + rebuild     (cleanest)
3. Fix forward with hotfix         (if rollback risky)
```

Always know your last known good version!

---

## Common Mistakes to Avoid

| Mistake | Why It's Bad | Do Instead |
|---------|--------------|------------|
| Using `latest` tag | Don't know what's deployed | Use commit SHA or semver |
| Hardcoding URLs | Breaks in other envs | Use environment variables |
| Skipping tests | Bugs in production | Always run tests |
| No approval for prod | Accidental deployments | Require manual approval |
| No secrets masking | Leaked credentials | Use GitHub Secrets |

---

## GitHub Actions Best Practices

1. **Use specific action versions** - `@v4` not `@main`
2. **Cache dependencies** - faster builds
3. **Use environments** - for approvals and secrets
4. **Limit permissions** - principle of least privilege
5. **Keep workflows DRY** - use reusable workflows

---

## Permissions Best Practice

```yaml
# Limit permissions to what's needed
permissions:
  contents: read
  packages: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
```

Don't give more permissions than necessary.

---

## Summary

| Principle | Rule |
|-----------|------|
| **Build** | Once, deploy many |
| **Test** | Always, before merge |
| **Security** | Scan in CI |
| **Deploy** | Protected branches only |
| **Production** | Manual approval required |
| **Secrets** | Never in code |
| **Rollback** | Always have a plan |

---

## Questions?

---

## Thank You!

**Key Takeaways:**
- Build once, deploy many
- Pipeline must pass before merge
- Manual approval for production
- Never hardcode secrets
- Always know your rollback path

---

## Next Steps

After mastering these basics, explore:
- Blue/Green deployments
- Canary releases
- Feature flags
- GitOps with ArgoCD
- Advanced rollback strategies

See the advanced CI/CD slides for more.

---

[Previous: Workflow Examples](./06-workflow-examples.md) | [Index](./README.md)
