[Index](./README.md) | [Next: GitHub Actions Basics](./02-github-actions-basics.md)

---

# What is CI/CD?

## The Problem

Without CI/CD:
```
Developer finishes code
        |
   "Works on my machine"
        |
Manual testing, manual deploy
        |
   Bugs found in production
```

---

## Continuous Integration (CI)

**Automatically build and test code on every change.**

```
Developer pushes code
        |
   GitHub Actions
        |
Build -> Test -> Scan
        |
   Pass or Fail
```

**Goal:** Catch bugs early, ensure code quality.

---

## Continuous Delivery (CD)

**Automatically prepare code for deployment.**

```
CI passes
    |
Build Docker image
    |
Push to registry
    |
Deploy to dev/stage
    |
Manual approval -> Deploy to production
```

---

## Continuous Deployment

**Automatically deploy to production (no manual gate).**

```
CI passes -> Deploy to staging -> Auto-deploy to production
```

We recommend **Continuous Delivery** (with manual approval for prod) to start.

---

## The Full Picture

```
Code -> Build -> Test -> Scan -> Package -> Deploy -> Monitor
  |                                            |
  -------------- Feedback Loop ----------------
```

| Phase | CI | CD |
|-------|----|----|
| Build | Yes | |
| Test | Yes | |
| Security Scan | Yes | |
| Package (Docker) | | Yes |
| Deploy | | Yes |
| Release | | Yes |

---

## Why CI/CD Matters

| Without CI/CD | With CI/CD |
|---------------|------------|
| Manual testing | Automated testing |
| "It works locally" | Same build everywhere |
| Deploy on Friday (bad idea) | Deploy anytime safely |
| Bugs in production | Bugs caught early |
| Hours of manual work | Minutes of automation |

---

[Index](./README.md) | [Next: GitHub Actions Basics](./02-github-actions-basics.md)
