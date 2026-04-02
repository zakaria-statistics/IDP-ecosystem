[Previous: Environment Strategy](./05-environment-strategy.md) | [Index](./README.md) | [Next: Golden Rules](./07-golden-rules.md)

---

# Workflow Examples

## Example Set Goal

These examples implement a simple, production-usable baseline:
- PR quality checks
- Docker image build/push to GHCR
- Branch-based deployment to dev/stage/prod

---

## 1) PR Checks (`pull_request`)

```yaml
name: pr-checks

on:
  pull_request:
    branches: [dev, stage, main]

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "run lint + unit tests"
```

Use this workflow to block low-quality merges early.

---

## 2) Build and Push Image (`push`)

```yaml
name: build-image

on:
  push:
    branches: [dev, stage, main]

permissions:
  contents: read
  packages: write

jobs:
  image:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: ghcr.io/${{ github.repository }}:sha-${{ github.sha }}
```

---

## 3) Branch-Based Deploy

```yaml
name: deploy

on:
  workflow_run:
    workflows: ["build-image"]
    types: [completed]

jobs:
  deploy-dev:
    if: github.event.workflow_run.head_branch == 'dev'
    runs-on: ubuntu-latest
    environment: development
    steps:
      - run: echo "deploy to dev"

  deploy-stage:
    if: github.event.workflow_run.head_branch == 'stage'
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - run: echo "deploy to stage"

  deploy-prod:
    if: github.event.workflow_run.head_branch == 'main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - run: echo "deploy to prod (approval required)"
```

---

## 4) Spring Boot Quality Job

```yaml
jobs:
  spring-ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B clean verify
```

---

## 5) Next.js Quality Job

```yaml
jobs:
  next-ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
      - run: npm ci
      - run: npm run lint
      - run: npm test -- --ci
      - run: npm run build
```

---

## 6) Python Quality Job

```yaml
jobs:
  python-ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: pip
      - run: pip install -r requirements.txt
      - run: pip install pytest ruff
      - run: ruff check .
      - run: pytest
```

---

## 7) Reusable Workflow Call

`repo A` can call shared CI logic from a platform repo:

```yaml
jobs:
  shared-ci:
    uses: beebay/platform-workflows/.github/workflows/service-ci.yml@main
    with:
      runtime: node
      run_tests: true
    secrets: inherit
```

This supports IDP-style standardization with local flexibility.

---

## 8) Deployment to Kubernetes (Conceptual)

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - run: echo "authenticate to cluster"
      - run: echo "set image tag in helm values"
      - run: echo "helm upgrade --install ..."
      - run: echo "run smoke checks"
```

In production, keep this behind environment approval.

---

## 9) Path-Based Builds for Mono Repos

```yaml
on:
  pull_request:
    paths:
      - "backend/**"
      - "frontend/**"
      - "ai/**"
```

This prevents unnecessary jobs and keeps feedback faster.

---

## 10) Caching Quick Reference

```yaml
# Node
- uses: actions/setup-node@v4
  with:
    cache: npm

# Java
- uses: actions/setup-java@v4
  with:
    cache: maven

# Python
- uses: actions/setup-python@v5
  with:
    cache: pip
```

---

[Previous: Environment Strategy](./05-environment-strategy.md) | [Index](./README.md) | [Next: Golden Rules](./07-golden-rules.md)
