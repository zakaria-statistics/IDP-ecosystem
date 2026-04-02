[Previous: GitHub Actions Basics](./02-github-actions-basics.md) | [Index](./README.md) | [Next: Artifact Management](./04-artifact-management.md)

---

# Pipeline Stages

## Typical Pipeline Structure

```
+------------------------------------------------------------------+
|                        CI PIPELINE                                |
+----------+----------+----------+----------+----------+-----------+
|  Build   |   Lint   |   Test   |   SAST   | Secrets  |  Package  |
+----------+----------+----------+----------+----------+-----------+
                                                              |
                                                              v
+------------------------------------------------------------------+
|                        CD PIPELINE                                |
+------------------+------------------+----------------------------+
|   Deploy Dev     |  Deploy Stage    |     Deploy Prod            |
|                  |                  |   (manual approval)        |
+------------------+------------------+----------------------------+
```

---

## CI Jobs Explained

| Job | Purpose | Tools |
|-----|---------|-------|
| **Build** | Compile code, resolve deps | npm, maven, gradle |
| **Lint** | Code style & quality | ESLint, Prettier, Checkstyle |
| **Unit Test** | Test functions in isolation | Jest, JUnit, pytest |
| **SAST** | Static security analysis | CodeQL, SonarQube |
| **Secret Scan** | Find leaked credentials | Gitleaks, GitHub secret scanning |
| **Package** | Create deployable artifact | Docker build |

---

## CD Jobs Explained

| Job | Purpose | Trigger |
|-----|---------|---------|
| **Deploy Dev** | Developer testing | Automatic on `dev` branch |
| **Deploy Stage** | QA validation | Automatic on `stage` branch |
| **Deploy Prod** | Live users | Manual approval on `main` |

---

## Pipeline Per Branch Type

```yaml
feature/* -> PR to dev:
  Build -> Lint -> Unit Tests -> Security Scan

dev branch:
  Build -> Test -> Package -> Deploy to Dev

stage branch:
  Build -> Test -> Package -> Deploy to Stage

main branch:
  Build -> Test -> Package -> Manual Approval -> Deploy to Prod
```

---

## GitHub Actions: Jobs as Stages

```yaml
jobs:
  # CI Stage
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm run build

  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm run lint

  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test

  # CD Stage
  deploy:
    needs: [build, lint, test]
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploying..."
```

---

## Parallel vs Sequential

**Parallel (faster):**
```yaml
jobs:
  build:   # Starts immediately
  lint:    # Starts immediately (parallel with build)
  test:    # Starts immediately (parallel)
```

**Sequential (dependencies):**
```yaml
jobs:
  build:
  test:
    needs: build    # Waits for build
  deploy:
    needs: test     # Waits for test
```

---

## Our Recommended Pipeline

```
+-------+    +------+    +------+    +----------+
| Build | -> | Lint | -> | Test | -> | Security |
+-------+    +------+    +------+    +----------+
                                          |
                                          v
                              +---------------------+
                              | Package (Docker)    |
                              +---------------------+
                                          |
        +---------------------------------+
        |               |                 |
        v               v                 v
   [Deploy Dev]   [Deploy Stage]   [Deploy Prod]
    (auto)          (auto)        (manual approval)
```

---

[Previous: GitHub Actions Basics](./02-github-actions-basics.md) | [Index](./README.md) | [Next: Artifact Management](./04-artifact-management.md)
