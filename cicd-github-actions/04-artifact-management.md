[Previous: Pipeline Stages](./03-pipeline-stages.md) | [Index](./README.md) | [Next: Environment Strategy](./05-environment-strategy.md)

---

# Artifact Management

## What is an Artifact?

A built, versioned, deployable output.

For this platform, the primary artifact is a Docker image.

```text
Git commit -> CI checks -> Docker image -> deploy
```

Production must run images, not raw source code.

---

## GitHub Container Registry (ghcr.io)

Image naming pattern:

```text
ghcr.io/OWNER/IMAGE_NAME:TAG
```

Examples:

```text
ghcr.io/beebay/backend-api:sha-a1b2c3d
ghcr.io/beebay/frontend-app:sha-a1b2c3d
ghcr.io/beebay/ai-service:sha-a1b2c3d
```

Benefits:
- Same identity and permissions model as GitHub repos
- Easy traceability from commit to image
- Simple integration with GitHub Actions

---

## Immutable Versioned Images

Golden rule:
- Never rebuild and overwrite the same version tag with new code
- Build a new tag for every change

```text
Good:
  myapp:v1.4.1   -> stable release
  myapp:v1.4.2   -> new feature
  myapp:sha-abc1234 -> exact commit

Bad:
  myapp:latest   -> unclear content
  myapp:v1.4.1   -> rebuilt with different code
```

---

## Tagging Strategy (Recommended)

```bash
# Always generate commit tag
ghcr.io/beebay/api:sha-a1b2c3d

# Add semver tag for release commits
ghcr.io/beebay/api:v1.4.2

# Optional helper tags for non-prod
ghcr.io/beebay/api:dev
ghcr.io/beebay/api:stage
```

Production should deploy semver or SHA tags, never `latest`.

---

## Build and Push in GitHub Actions

```yaml
jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
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
          tags: |
            ghcr.io/${{ github.repository }}:sha-${{ github.sha }}
            ghcr.io/${{ github.repository }}:${{ github.ref_name }}
```

---

## Image Metadata for Traceability

```dockerfile
LABEL org.opencontainers.image.source=https://github.com/beebay/api
LABEL org.opencontainers.image.revision=$GIT_SHA
LABEL org.opencontainers.image.created=$BUILD_DATE
```

Each deployed image should answer:
- Which commit produced it
- Which pipeline produced it
- When it was produced

---

## Build Once, Deploy Many

```text
Git commit
   ->
Build image once
   ->
Deploy same image to dev, stage, prod
```

Do not rebuild for each environment.
Use configuration and secrets to vary behavior.

---

## Docker Compose as Local Contract

```yaml
services:
  backend:
    build: ./backend
  frontend:
    build: ./frontend
  ai-service:
    build: ./ai
```

Use the same Dockerfiles locally and in CI to reduce drift.

---

## Retention and Cleanup

Keep registry clean:
- Retain all release tags (`v*`)
- Retain recent SHA tags for incident debugging
- Delete stale branch tags on schedule

This keeps costs predictable and rollback history usable.

---

[Previous: Pipeline Stages](./03-pipeline-stages.md) | [Index](./README.md) | [Next: Environment Strategy](./05-environment-strategy.md)
