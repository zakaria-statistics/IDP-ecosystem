[Previous: What is CI/CD and Why Now](./01-what-is-cicd.md) | [Index](./README.md) | [Next: Pipeline Stages](./03-pipeline-stages.md)

---

# GitHub Actions Basics

## Core Vocabulary

| Term | Meaning |
|------|---------|
| Workflow | YAML automation file in `.github/workflows/` |
| Event | Trigger (`pull_request`, `push`, `workflow_dispatch`) |
| Job | Group of steps on one runner |
| Step | Single command or reusable action |
| Runner | Machine that executes jobs |
| Environment | Protected deploy target (dev/stage/prod) |

---

## Where Workflows Live

```text
repo/
  .github/
    workflows/
      ci.yml
      build-image.yml
      deploy.yml
      security.yml
```

Treat workflows as production code:
- Code review required
- Small changes preferred
- Owners and standards documented

---

## Minimal CI Workflow

```yaml
name: ci

on:
  pull_request:
    branches: [dev, stage, main]
  push:
    branches: [dev, stage, main]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "run build, lint, test"
```

---

## Job Dependencies (Flow Control)

```yaml
jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - run: echo "lint + tests"

  build-image:
    needs: quality
    runs-on: ubuntu-latest
    steps:
      - run: echo "docker build/push"

  deploy-dev:
    if: github.ref == 'refs/heads/dev'
    needs: build-image
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy"
```

Use `needs:` for explicit stage ordering.

---

## Trigger Strategy for Our Branch Model

```yaml
on:
  pull_request:
    branches: [dev, stage, main]
  push:
    branches: [dev, stage, main]
  workflow_dispatch:
```

Meaning:
- PR: verify quality before merge
- Push on permanent branches: build/publish/deploy by environment
- Manual trigger: controlled re-run or emergency deployment

---

## Security Baseline in Workflow Files

```yaml
permissions:
  contents: read
  packages: write

jobs:
  build:
    runs-on: ubuntu-latest
```

Baseline rules:
- Keep permissions minimal
- Use pinned major versions (`@v4`, `@v5`)
- Use secrets only through GitHub Secrets/Environments

---

## Useful GitHub Context Values

| Expression | Usage |
|-----------|-------|
| `${{ github.sha }}` | Immutable image tag |
| `${{ github.ref_name }}` | Branch-aware logic |
| `${{ github.event_name }}` | PR vs push behavior |
| `${{ github.repository }}` | Registry naming |
| `${{ github.actor }}` | Audit trail |

---

## Reuse Instead of Copy/Paste

Options:
- Reusable workflows (`workflow_call`) for shared CI logic
- Composite actions for shared scripting steps

Benefits:
- Consistency across Spring/Next/Python repos
- Faster onboarding
- Lower maintenance cost

---

## Concurrency for Safer Pipelines

```yaml
concurrency:
  group: deploy-${{ github.ref }}
  cancel-in-progress: true
```

Prevents multiple deploy jobs from racing on the same branch.

---

[Previous: What is CI/CD and Why Now](./01-what-is-cicd.md) | [Index](./README.md) | [Next: Pipeline Stages](./03-pipeline-stages.md)
