[Previous: Pipeline Stages](./03-pipeline-stages.md) | [Index](./README.md) | [Next: Environment Strategy](./05-environment-strategy.md)

---

# Artifact Management

## What is an Artifact?

A **built, versioned, deployable package** of your application.

For us, this means **Docker images**.

```
Git Commit ----> CI Build ----> Docker Image ----> Deploy
    |                              |
 (source code)              (runnable thing)
```

**Production runs Docker images, not Git commits directly.**

---

## GitHub Container Registry (ghcr.io)

Our Docker images live in GitHub Container Registry:

```
ghcr.io/OWNER/IMAGE_NAME:TAG
```

Example:
```
ghcr.io/beebay/backend-api:1.2.0
ghcr.io/beebay/frontend-app:1.2.0
ghcr.io/beebay/ai-service:1.2.0
```

**Benefits:** Integrated with GitHub, same access control, free for public repos.

---

## Immutable Versioned Images

**Golden Rule:** Once built, never modify. Create new version instead.

```
Good:
  myapp:1.4.1  -> stable release
  myapp:1.4.2  -> new feature
  myapp:1.4.3  -> hotfix

Bad:
  myapp:latest  -> what version is this??
  myapp:1.4.1   -> rebuilt with different code (broken!)
```

---

## Tagging Strategy

```bash
# Semantic version (for releases)
ghcr.io/beebay/api:1.4.2

# Git SHA (for traceability)
ghcr.io/beebay/api:a1b2c3d

# Branch-based (for dev/testing)
ghcr.io/beebay/api:dev
ghcr.io/beebay/api:stage

# Avoid for production:
ghcr.io/beebay/api:latest   # What version??
```

---

## Building Docker in GitHub Actions

```yaml
jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: |
            ghcr.io/${{ github.repository }}:${{ github.sha }}
            ghcr.io/${{ github.repository }}:latest
```

---

## Image Labels for Traceability

Add metadata to your Docker images:

```dockerfile
# In your Dockerfile
LABEL org.opencontainers.image.source=https://github.com/beebay/api
LABEL org.opencontainers.image.revision=$GIT_SHA
LABEL org.opencontainers.image.created=$BUILD_DATE
```

Every deployed image should answer:
- What Git commit built this?
- When was it built?
- What pipeline built it?

---

## Build Once, Deploy Many

```
                 +---> Dev Environment
                 |
Git Commit       |
    |            |
  Build     Same Image ---> Stage Environment
    |            |
Docker Image     |
    |            +---> Prod Environment
  Push to
  ghcr.io

Never rebuild for different environments!
Use environment variables for config differences.
```

---

## Docker Compose for Local Dev

```yaml
# docker-compose.yml
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgres://db:5432/app

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"

  db:
    image: postgres:15
```

Same Dockerfile used locally and in CI.

---

[Previous: Pipeline Stages](./03-pipeline-stages.md) | [Index](./README.md) | [Next: Environment Strategy](./05-environment-strategy.md)
