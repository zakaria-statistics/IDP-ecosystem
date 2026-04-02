[Index](./README.md) | [Next: GitHub Actions Basics](./02-github-actions-basics.md)

---

# What is CI/CD and Why Now

## Why This Deck Exists

We are moving from a GitLab-first CI mindset to a GitHub Actions-first model.

Goal:
- Keep the workflow simple and reliable
- Standardize delivery for Spring, Next.js, and Python services
- Build the foundation of our Internal Developer Platform (IDP)

---

## Current Pain Without Standard CI/CD

```text
Different pipelines per repo
    ->
Different quality gates
    ->
Hard to know release readiness
    ->
Risky production deployments
```

Typical symptoms:
- "Works on my machine"
- Manual deploy steps hidden in personal notes
- Unclear ownership during release incidents

---

## CI: Continuous Integration

CI means every change is automatically validated.

```text
Commit / Pull Request
    ->
Build + Lint + Test + Basic Security Checks
    ->
Pass or fail fast
```

CI objective:
- Detect problems early
- Prevent bad merges into `dev`, `stage`, `main`

---

## CD: Continuous Delivery (Our Target)

CD means software is always in a deployable state.

```text
CI pass
  -> build docker image
  -> push image to ghcr.io
  -> deploy to dev/stage automatically
  -> deploy to prod with manual approval
```

For now we use:
- Automatic deploy to `dev` and `stage`
- Manual gate for `prod`

---

## Continuous Deployment (Later, Not Now)

Continuous deployment removes the production approval gate.

```text
CI pass -> auto deploy all the way to production
```

We are intentionally not starting here because:
- We need stronger observability first
- We need stable rollback routines first

---

## End-to-End Delivery Picture

```text
Code -> CI Checks -> Docker Image -> Registry -> Deploy -> Monitor
  ^                                                       |
  +------------------------- feedback --------------------+
```

| Phase | Main Owner | Output |
|------|------------|--------|
| CI Checks | Developers | Validated commit |
| Packaging | CI platform | Immutable image |
| Deployment | Tech lead + platform | Running release |
| Monitoring | Whole team | Incident signal + learning |

---

## What "Good" Looks Like

Within the team, "good CI/CD" means:
- Pipeline status is mandatory for merge
- Same image runs in all environments
- Production deploy is auditable and reversible
- Release process is clear for developers, seniors, and tech leads

---

[Index](./README.md) | [Next: GitHub Actions Basics](./02-github-actions-basics.md)
