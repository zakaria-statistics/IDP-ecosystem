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
| **DEV** | Early validation, smoke tests | `develop` branch, `:dev-<sha>` tag | Smoke, basic health |
| **QA** | Full integration, e2e testing | After DEV passes, `:qa-<sha>` tag | Integration, e2e, regression |

---

## Minimal Workflow (Dev/QA Focus)

### Flow Diagram

```text
     +-------------+
     |   PR / Push |
     +------+------+
            |
     +------v------+
     |    Build    |
     |  + Unit Test|
     +------+------+
            |
     +------v------+
     | Push to GHCR|
     | :dev-<sha>  |
     +------+------+
            |
     +------v------+
     | Deploy DEV  |
     | (OVH VM)    |
     +------+------+
            |
     +------v------+
     | Smoke Test  |
     +------+------+
            |
            | Pass?
     +------v------+
     | Deploy QA   |
     | (OVH VM)    |
     +------+------+
            |
     +------v------+
     | E2E Tests   |
     +------+------+
            |
            | Pass?
     +------v------+
     | Tag :release|
     | Push GHCR   |
     +------+------+
            |
            v
   Ready for Client
```

### GitHub Actions Workflow

```yaml
# .github/workflows/dev-qa-pipeline.yml
name: Dev/QA Pipeline

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ============================================
  # Stage 1: Build and Push
  # ============================================
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    outputs:
      image_tag: ${{ steps.meta.outputs.version }}
    steps:
      - uses: actions/checkout@v4

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=raw,value=dev-${{ github.sha }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}

  # ============================================
  # Stage 2: Deploy to DEV
  # ============================================
  deploy-dev:
    needs: build
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
            export IMAGE_TAG=dev-${{ github.sha }}
            docker compose pull
            docker compose up -d --remove-orphans

      - name: Smoke test
        run: |
          sleep 10
          curl --fail --retry 5 --retry-delay 5 \
            http://${{ secrets.DEV_VM_HOST }}:8080/health

  # ============================================
  # Stage 3: Deploy to QA
  # ============================================
  deploy-qa:
    needs: deploy-dev
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
            export IMAGE_TAG=dev-${{ github.sha }}
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
    needs: deploy-qa
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

      - name: Promote to release tag
        run: |
          docker pull ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:dev-${{ github.sha }}
          docker tag ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:dev-${{ github.sha }} \
                     ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          docker tag ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:dev-${{ github.sha }} \
                     ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:v${{ github.run_number }}
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:v${{ github.run_number }}
```

---

## Docker Compose for Your VMs

```yaml
# docker-compose.yml (on DEV/QA VMs)
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
Branch/Event          Image Tag              Environment
─────────────────────────────────────────────────────────
PR / develop     -->  :dev-<sha>        -->  Your DEV VM
After DEV pass   -->  :qa-<sha>         -->  Your QA VM
main + QA pass   -->  :v1.2.3, :latest  -->  GHCR (Client-ready)
```

---

## Required GitHub Secrets

| Secret | Environment | Purpose |
|--------|-------------|---------|
| `DEV_VM_HOST` | dev | DEV VM IP/hostname |
| `QA_VM_HOST` | qa | QA VM IP/hostname |
| `VM_USER` | all | SSH username |
| `VM_SSH_KEY` | all | SSH private key |

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
