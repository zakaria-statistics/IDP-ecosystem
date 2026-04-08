[Index](./README.md) | [Back: IDP Architecture](./00-idp-architecture.md)

---

# Dual-Path Architecture: Your Infra vs Client Infra

This document breaks down the two deployment paths in our IDP, focusing on the **pre-release validation** workflow using your own infrastructure.

---

## Overview

```text
+------------------+
|   Source Code    |
+--------+---------+
         |
+--------v---------+
|  GitHub Actions  |
|  build + test    |
+--------+---------+
         |
+--------v---------+
|      GHCR        |
| (Docker images)  |
+--------+---------+
         |
         +--------------------+---------------------+
         |                                          |
+--------v-------------+               +------------v------------+
|   YOUR INFRA PATH    |               |   CLIENT INFRA PATH     |
|   (Pre-release)      |               |   (Delivery)            |
+----------------------+               +-------------------------+
| - You own & manage   |               | - Client owns & manages |
| - Dev/QA validation  |               | - You deliver artifacts |
| - OVH VMs + Docker   |               | - Azure K8s / Rancher   |
| - Tests before ship  |               | - Client deploys        |
+----------------------+               +-------------------------+
```

---

## Branch Strategy

| Branch | Maps To | Deploys To | Purpose |
|--------|---------|------------|---------|
| `feature/*` | PR to `dev` | — | Development work |
| `dev` | DEV environment | Your DEV VM | Early validation, smoke tests |
| `stage` | QA environment | Your QA VM | Integration, e2e testing |
| `main` | Release | GHCR (`:latest`, `:vX.Y.Z`) | Client-ready artifacts |

**Promotion path:** `feature/*` → `dev` → `stage` → `main`

---

## Path Distinction

| Aspect | Your Infra Path | Client Infra Path |
|--------|-----------------|-------------------|
| **Purpose** | Pre-release validation | Final delivery |
| **Ownership** | You manage everything | Client manages infra |
| **Environments** | DEV, QA | Client's dev/stage/prod |
| **Infrastructure** | OVH VMs, Docker Compose | Azure K8s via Rancher |
| **Access** | Full SSH/admin access | Limited or none (deliver artifacts only) |
| **When** | Every commit/PR | After QA passes, release tags |
| **Outcome** | Validated, tested image | Client pulls and deploys |

---

## Your Infra Path (Focus: Dev/QA)

### Infrastructure Stack

```text
+--------------------------------------------------+
|                  OVH Cloud / VMs                 |
+--------------------------------------------------+
|                                                  |
|  +-------------------+    +-------------------+  |
|  |     DEV VM        |    |      QA VM        |  |
|  +-------------------+    +-------------------+  |
|  | Docker Engine     |    | Docker Engine     |  |
|  | docker-compose    |    | docker-compose    |  |
|  |                   |    |                   |  |
|  | Services:         |    | Services:         |  |
|  |  - app container  |    |  - app container  |  |
|  |  - db (postgres)  |    |  - db (postgres)  |  |
|  |  - redis/cache    |    |  - redis/cache    |  |
|  +-------------------+    +-------------------+  |
|                                                  |
+--------------------------------------------------+
```

### Environment Purposes

| Environment | Purpose | Deployed From | Tests Run |
|-------------|---------|---------------|-----------|
| **DEV** | Early validation, smoke tests | `dev` branch, `:dev-<sha>` tag | Smoke, basic health |
| **QA** | Full integration, e2e testing | `stage` branch, `:stage-<sha>` tag | Integration, e2e, regression |

---

## Minimal Workflow (Dev/QA Focus)

### Flow Diagram

```text
feature/* ──────────────────────────────────────────────────────────────
     │
     │ PR
     ▼
+----+-----+      +-----------+      +-------------+      +-----------+
| dev      | ---> |  Build    | ---> | Push GHCR   | ---> | Deploy    |
| branch   |      |  + Test   |      | :dev-<sha>  |      | DEV VM    |
+----------+      +-----------+      +-------------+      +-----+-----+
                                                                │
                                                          Smoke Test
                                                                │
     ┌──────────────────────────────────────────────────────────┘
     │ Merge to stage (after DEV validated)
     ▼
+----+-----+      +-----------+      +-------------+      +-----------+
| stage    | ---> |  Build    | ---> | Push GHCR   | ---> | Deploy    |
| branch   |      |  + Test   |      | :stage-<sha>|      | QA VM     |
+----------+      +-----------+      +-------------+      +-----+-----+
                                                                │
                                                          E2E Tests
                                                                │
     ┌──────────────────────────────────────────────────────────┘
     │ Merge to main (after QA validated)
     ▼
+----+-----+      +---------------+
| main     | ---> | Tag :release  | ---> Ready for Client
| branch   |      | :v1.x, :latest|
+----------+      +---------------+
```

### GitHub Actions Workflow

```yaml
# .github/workflows/dev-qa-pipeline.yml
name: Dev/QA Pipeline

on:
  push:
    branches: [dev, stage, main]
  pull_request:
    branches: [dev, stage]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ============================================
  # Stage 1: Build and Push (all branches)
  # ============================================
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    outputs:
      image_tag: ${{ steps.set-tag.outputs.tag }}
    steps:
      - uses: actions/checkout@v4

      - name: Set image tag based on branch
        id: set-tag
        run: |
          if [[ "${{ github.ref }}" == "refs/heads/dev" ]]; then
            echo "tag=dev-${{ github.sha }}" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" == "refs/heads/stage" ]]; then
            echo "tag=stage-${{ github.sha }}" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" == "refs/heads/main" ]]; then
            echo "tag=release-${{ github.sha }}" >> $GITHUB_OUTPUT
          else
            echo "tag=pr-${{ github.sha }}" >> $GITHUB_OUTPUT
          fi

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.set-tag.outputs.tag }}

  # ============================================
  # Stage 2: Deploy to DEV (dev branch only)
  # ============================================
  deploy-dev:
    needs: build
    if: github.ref == 'refs/heads/dev'
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - name: Deploy to DEV VM
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEV_VM_HOST }}
          username: ${{ secrets.VM_USER }}
          key: ${{ secrets.VM_SSH_KEY }}
          script: |
            cd /opt/app
            export IMAGE_TAG=${{ needs.build.outputs.image_tag }}
            docker compose pull
            docker compose up -d --remove-orphans

      - name: Smoke test
        run: |
          sleep 10
          curl --fail --retry 5 --retry-delay 5 \
            http://${{ secrets.DEV_VM_HOST }}:8080/health

  # ============================================
  # Stage 3: Deploy to QA (stage branch only)
  # ============================================
  deploy-qa:
    needs: build
    if: github.ref == 'refs/heads/stage'
    runs-on: ubuntu-latest
    environment: qa
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to QA VM
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.QA_VM_HOST }}
          username: ${{ secrets.VM_USER }}
          key: ${{ secrets.VM_SSH_KEY }}
          script: |
            cd /opt/app
            export IMAGE_TAG=${{ needs.build.outputs.image_tag }}
            docker compose pull
            docker compose up -d --remove-orphans

      - name: Run E2E tests
        run: |
          npm ci
          npm run test:e2e -- --baseUrl=http://${{ secrets.QA_VM_HOST }}:8080

  # ============================================
  # Stage 4: Promote to Release (main only)
  # ============================================
  promote-release:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Promote to release tags
        run: |
          docker pull ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }}
          docker tag ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }} \
                     ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          docker tag ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }} \
                     ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:v${{ github.run_number }}
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:v${{ github.run_number }}
```

---

## Docker Compose for Your VMs

```yaml
# docker-compose.yml (on DEV VM: IMAGE_TAG=dev-<sha>, on QA VM: IMAGE_TAG=stage-<sha>)
services:
  app:
    image: ghcr.io/your-org/your-app:${IMAGE_TAG:-latest}
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgresql://user:pass@db:5432/app
      - REDIS_URL=redis://redis:6379
      - ENV=${ENV:-dev}
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
      POSTGRES_DB: app
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user -d app"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

---

## Client Infra Path (Delivery Only)

After your image passes QA and is tagged for release:

```text
+------------------+
| GHCR :release    |
| :latest, :v1.2.3 |
+--------+---------+
         |
         v
+------------------+     +-------------------------+
| Client pulls     | --> | Client's K8s / Rancher  |
| from GHCR        |     | dev / stage / prod      |
+------------------+     +-------------------------+
         |
         v
   You provide:
   - Release notes
   - Migration guide (if needed)
   - Helm chart / manifests (optional)
```

### What You Deliver to Client

| Artifact | Location | Notes |
|----------|----------|-------|
| Docker image | `ghcr.io/your-org/app:v1.2.3` | Generic, ENV-configured |
| Release notes | GitHub Release | What changed, breaking changes |
| Deployment guide | `docs/deployment.md` | ENV vars, dependencies |
| Helm chart (optional) | `charts/` directory | If client uses Helm |

---

## Tag Strategy Summary

```text
Branch              Image Tag              Environment         Purpose
────────────────────────────────────────────────────────────────────────
dev branch     -->  :dev-<sha>        -->  Your DEV VM    -->  Early validation
stage branch   -->  :stage-<sha>      -->  Your QA VM     -->  E2E / Integration
main branch    -->  :v1.2.3, :latest  -->  GHCR release   -->  Client-ready
```

### Branch Flow

```text
feature/* ──► dev ──► stage ──► main
              │        │         │
              ▼        ▼         ▼
            DEV VM   QA VM    GHCR :release
```

---

## Required GitHub Configuration

### Environments

Create these environments in GitHub repo settings:

| Environment | Used By Branch | Protection Rules |
|-------------|----------------|------------------|
| `dev` | `dev` | None (auto-deploy) |
| `qa` | `stage` | Optional: require approval |

### Secrets

| Secret | Scope | Purpose |
|--------|-------|---------|
| `DEV_VM_HOST` | `dev` environment | DEV VM IP/hostname |
| `QA_VM_HOST` | `qa` environment | QA VM IP/hostname |
| `VM_USER` | Repository | SSH username (shared) |
| `VM_SSH_KEY` | Repository | SSH private key (shared) |

---

## Next Steps

1. Set up OVH VMs for DEV and QA
2. Install Docker and docker-compose on VMs
3. Configure GitHub environments and secrets
4. Create generic Dockerfile with ENV-based config
5. Implement the minimal workflow above
6. Add monitoring/alerting to your VMs

---

[Index](./README.md) | [Back: IDP Architecture](./00-idp-architecture.md)
