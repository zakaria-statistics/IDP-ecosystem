[Previous: What is CI/CD](./01-what-is-cicd.md) | [Index](./README.md) | [Next: Pipeline Stages](./03-pipeline-stages.md)

---

# GitHub Actions Basics

## Key Concepts

| Term | Definition |
|------|------------|
| **Workflow** | An automated process (YAML file) |
| **Event** | What triggers the workflow (push, PR, etc.) |
| **Job** | A set of steps that run on the same runner |
| **Step** | Individual task within a job |
| **Action** | Reusable unit of code (from marketplace or custom) |
| **Runner** | Server that runs your workflows |

---

## Workflow File Location

```
your-repo/
  .github/
    workflows/
      ci.yml          # Your CI workflow
      deploy.yml      # Your deploy workflow
      ...
```

Workflows live in `.github/workflows/` as YAML files.

---

## Basic Workflow Structure

```yaml
name: CI Pipeline

on:                          # Events that trigger
  push:
    branches: [main, dev]
  pull_request:
    branches: [main, dev]

jobs:                        # Jobs to run
  build:
    runs-on: ubuntu-latest   # Runner type
    steps:                   # Steps in the job
      - uses: actions/checkout@v4
      - name: Run tests
        run: npm test
```

---

## Common Triggers (Events)

```yaml
on:
  # On push to specific branches
  push:
    branches: [main, dev, stage]

  # On pull request
  pull_request:
    branches: [main]

  # Manual trigger
  workflow_dispatch:

  # Scheduled (cron)
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
```

---

## Jobs and Steps

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # Use a pre-built action
      - uses: actions/checkout@v4

      # Run a command
      - name: Install dependencies
        run: npm install

      # Run multiple commands
      - name: Test
        run: |
          npm run lint
          npm test
```

---

## Job Dependencies

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Building..."

  test:
    needs: build              # Runs after build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Testing..."

  deploy:
    needs: [build, test]      # Runs after both
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploying..."
```

---

## Environment Variables & Secrets

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    env:
      NODE_ENV: production    # Job-level env var
    steps:
      - name: Deploy
        env:
          API_KEY: ${{ secrets.API_KEY }}  # From secrets
        run: |
          echo "Deploying to $NODE_ENV"
```

**Secrets:** Settings -> Secrets and variables -> Actions

---

## Useful Built-in Variables

| Variable | Value |
|----------|-------|
| `github.sha` | Commit SHA |
| `github.ref_name` | Branch or tag name |
| `github.event_name` | Event type (push, pull_request) |
| `github.repository` | Owner/repo name |
| `github.actor` | User who triggered |

```yaml
- run: echo "Deploying ${{ github.sha }}"
```

---

## Common Actions

| Action | Purpose |
|--------|---------|
| `actions/checkout@v4` | Clone your repo |
| `actions/setup-node@v4` | Setup Node.js |
| `actions/setup-java@v4` | Setup Java/JDK |
| `actions/setup-python@v5` | Setup Python |
| `docker/build-push-action@v5` | Build & push Docker |
| `docker/login-action@v3` | Login to registry |

---

## Workflow Visualization

```
                    Workflow (ci.yml)
                          |
         -----------------+------------------
         |                |                 |
      [build]          [lint]           [test]
         |                |                 |
         -----------------+-----------------
                          |
                     [deploy]
```

Jobs can run in **parallel** or **sequentially** (with `needs:`).

---

[Previous: What is CI/CD](./01-what-is-cicd.md) | [Index](./README.md) | [Next: Pipeline Stages](./03-pipeline-stages.md)
