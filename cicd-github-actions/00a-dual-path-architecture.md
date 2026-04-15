[Index](./README.md) | [Back: IDP Architecture](./00-idp-architecture.md) | [Azure VMs](./00b-ephemeral-azure-vm.md) | [Azure ACI](./00c-ephemeral-azure-aci.md)

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
+----+-----+      +-----------+      +-----------+      +-------------+      +-----------+
| dev      | ---> | Security  | ---> |  Build    | ---> | Push GHCR   | ---> | Deploy    |
| branch   |      | Scan      |      |  + Test   |      | :dev-<sha>  |      | DEV VM    |
+----------+      +-----------+      +-----------+      +-------------+      +-----+-----+
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
  # Stage 0: Security Scan (secrets detection)
  # ============================================
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for thorough scan

      - name: Gitleaks - Detect hardcoded secrets
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITLEAKS_LICENSE: ${{ secrets.GITLEAKS_LICENSE }}  # Optional: for premium features

      - name: TruffleHog - Deep secrets scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: ${{ github.event.repository.default_branch }}
          head: HEAD
          extra_args: --only-verified

  # ============================================
  # Stage 1: Build and Push (all branches)
  # ============================================
  build:
    needs: security-scan  # Block build if secrets detected
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

  # ============================================
  # EPHEMERAL AZURE ENVIRONMENTS (OPTIONAL)
  # ============================================
  # Uncomment the sections below to use Azure ephemeral environments
  # instead of persistent OVH VMs. Choose ONE option:
  #   - Option A: Azure VMs with Docker pre-installed
  #   - Option B: Azure Container Instances (ACI) - serverless
  #
  # Benefits:
  #   - Pay only when environments are running
  #   - Fresh environment for each deployment
  #   - No maintenance of persistent VMs
  #   - Auto-cleanup prevents resource sprawl
  # ============================================

  # ------------------------------------------
  # OPTION A: Ephemeral Azure VMs with Docker
  # ------------------------------------------
  # Creates a full VM with Docker for complex testing scenarios
  # (multiple containers, volumes, network simulation)
  #
  # deploy-dev-azure-vm:
  #   needs: build
  #   if: github.ref == 'refs/heads/dev'
  #   runs-on: ubuntu-latest
  #   environment: dev
  #   outputs:
  #     vm_ip: ${{ steps.create-vm.outputs.vm_ip }}
  #     resource_group: ${{ steps.create-vm.outputs.resource_group }}
  #   steps:
  #     - uses: actions/checkout@v4
  #
  #     - name: Azure Login
  #       uses: azure/login@v2
  #       with:
  #         creds: ${{ secrets.AZURE_CREDENTIALS }}
  #
  #     - name: Create ephemeral resource group
  #       id: create-vm
  #       run: |
  #         RG_NAME="rg-ephemeral-dev-${{ github.run_id }}"
  #         VM_NAME="vm-dev-${{ github.run_id }}"
  #
  #         # Create resource group with auto-delete tag
  #         az group create \
  #           --name $RG_NAME \
  #           --location eastus \
  #           --tags environment=dev ephemeral=true created=${{ github.run_id }} expires=$(date -d '+4 hours' -Iseconds)
  #
  #         # Create VM with Docker pre-installed (using cloud-init)
  #         az vm create \
  #           --resource-group $RG_NAME \
  #           --name $VM_NAME \
  #           --image Ubuntu2204 \
  #           --size Standard_B2s \
  #           --admin-username azureuser \
  #           --ssh-key-values "${{ secrets.AZURE_VM_SSH_PUB_KEY }}" \
  #           --custom-data @.azure/cloud-init-docker.yml \
  #           --public-ip-sku Standard \
  #           --output json
  #
  #         # Get public IP
  #         VM_IP=$(az vm show -d -g $RG_NAME -n $VM_NAME --query publicIps -o tsv)
  #
  #         echo "vm_ip=$VM_IP" >> $GITHUB_OUTPUT
  #         echo "resource_group=$RG_NAME" >> $GITHUB_OUTPUT
  #
  #     - name: Wait for VM to be ready
  #       run: |
  #         echo "Waiting for Docker to be ready on VM..."
  #         for i in {1..30}; do
  #           if ssh -o StrictHostKeyChecking=no azureuser@${{ steps.create-vm.outputs.vm_ip }} "docker --version" 2>/dev/null; then
  #             echo "VM is ready!"
  #             break
  #           fi
  #           echo "Attempt $i/30 - waiting..."
  #           sleep 10
  #         done
  #
  #     - name: Deploy application
  #       run: |
  #         ssh -o StrictHostKeyChecking=no azureuser@${{ steps.create-vm.outputs.vm_ip }} << 'EOF'
  #           # Login to GHCR
  #           echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
  #
  #           # Pull and run the image
  #           docker pull ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }}
  #           docker run -d -p 8080:8080 \
  #             -e ENV=dev \
  #             --name app \
  #             ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }}
  #         EOF
  #
  #     - name: Run smoke tests
  #       run: |
  #         sleep 15
  #         curl --fail --retry 5 --retry-delay 5 \
  #           http://${{ steps.create-vm.outputs.vm_ip }}:8080/health
  #
  # deploy-qa-azure-vm:
  #   needs: build
  #   if: github.ref == 'refs/heads/stage'
  #   runs-on: ubuntu-latest
  #   environment: qa
  #   outputs:
  #     vm_ip: ${{ steps.create-vm.outputs.vm_ip }}
  #     resource_group: ${{ steps.create-vm.outputs.resource_group }}
  #   steps:
  #     - uses: actions/checkout@v4
  #
  #     - name: Azure Login
  #       uses: azure/login@v2
  #       with:
  #         creds: ${{ secrets.AZURE_CREDENTIALS }}
  #
  #     - name: Create ephemeral resource group
  #       id: create-vm
  #       run: |
  #         RG_NAME="rg-ephemeral-qa-${{ github.run_id }}"
  #         VM_NAME="vm-qa-${{ github.run_id }}"
  #
  #         az group create \
  #           --name $RG_NAME \
  #           --location eastus \
  #           --tags environment=qa ephemeral=true created=${{ github.run_id }} expires=$(date -d '+8 hours' -Iseconds)
  #
  #         az vm create \
  #           --resource-group $RG_NAME \
  #           --name $VM_NAME \
  #           --image Ubuntu2204 \
  #           --size Standard_B2ms \
  #           --admin-username azureuser \
  #           --ssh-key-values "${{ secrets.AZURE_VM_SSH_PUB_KEY }}" \
  #           --custom-data @.azure/cloud-init-docker.yml \
  #           --public-ip-sku Standard
  #
  #         VM_IP=$(az vm show -d -g $RG_NAME -n $VM_NAME --query publicIps -o tsv)
  #         echo "vm_ip=$VM_IP" >> $GITHUB_OUTPUT
  #         echo "resource_group=$RG_NAME" >> $GITHUB_OUTPUT
  #
  #     - name: Wait for VM and deploy
  #       run: |
  #         sleep 60  # Wait for cloud-init
  #         ssh -o StrictHostKeyChecking=no azureuser@${{ steps.create-vm.outputs.vm_ip }} << 'EOF'
  #           echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
  #           docker pull ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }}
  #           docker run -d -p 8080:8080 -e ENV=qa --name app \
  #             ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }}
  #         EOF
  #
  #     - name: Run E2E tests
  #       run: |
  #         npm ci
  #         npm run test:e2e -- --baseUrl=http://${{ steps.create-vm.outputs.vm_ip }}:8080

  # ------------------------------------------
  # OPTION B: Azure Container Instances (ACI)
  # ------------------------------------------
  # Lightweight, serverless - best for simple container deployments
  # No VM management, pay per second of execution
  #
  # deploy-dev-aci:
  #   needs: build
  #   if: github.ref == 'refs/heads/dev'
  #   runs-on: ubuntu-latest
  #   environment: dev
  #   outputs:
  #     container_ip: ${{ steps.deploy-aci.outputs.container_ip }}
  #     resource_group: ${{ steps.deploy-aci.outputs.resource_group }}
  #   steps:
  #     - name: Azure Login
  #       uses: azure/login@v2
  #       with:
  #         creds: ${{ secrets.AZURE_CREDENTIALS }}
  #
  #     - name: Deploy to ACI
  #       id: deploy-aci
  #       run: |
  #         RG_NAME="rg-aci-dev-${{ github.run_id }}"
  #         CONTAINER_NAME="aci-dev-${{ github.run_id }}"
  #
  #         # Create resource group
  #         az group create \
  #           --name $RG_NAME \
  #           --location eastus \
  #           --tags environment=dev ephemeral=true expires=$(date -d '+4 hours' -Iseconds)
  #
  #         # Deploy container directly (no VM needed!)
  #         az container create \
  #           --resource-group $RG_NAME \
  #           --name $CONTAINER_NAME \
  #           --image ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }} \
  #           --registry-login-server ghcr.io \
  #           --registry-username ${{ github.actor }} \
  #           --registry-password ${{ secrets.GITHUB_TOKEN }} \
  #           --cpu 1 \
  #           --memory 1.5 \
  #           --ports 8080 \
  #           --ip-address Public \
  #           --environment-variables ENV=dev \
  #           --restart-policy OnFailure
  #
  #         # Get container IP
  #         CONTAINER_IP=$(az container show \
  #           --resource-group $RG_NAME \
  #           --name $CONTAINER_NAME \
  #           --query ipAddress.ip -o tsv)
  #
  #         echo "container_ip=$CONTAINER_IP" >> $GITHUB_OUTPUT
  #         echo "resource_group=$RG_NAME" >> $GITHUB_OUTPUT
  #
  #     - name: Run smoke tests
  #       run: |
  #         sleep 30
  #         curl --fail --retry 10 --retry-delay 5 \
  #           http://${{ steps.deploy-aci.outputs.container_ip }}:8080/health
  #
  # deploy-qa-aci:
  #   needs: build
  #   if: github.ref == 'refs/heads/stage'
  #   runs-on: ubuntu-latest
  #   environment: qa
  #   outputs:
  #     container_ip: ${{ steps.deploy-aci.outputs.container_ip }}
  #     resource_group: ${{ steps.deploy-aci.outputs.resource_group }}
  #   steps:
  #     - uses: actions/checkout@v4
  #
  #     - name: Azure Login
  #       uses: azure/login@v2
  #       with:
  #         creds: ${{ secrets.AZURE_CREDENTIALS }}
  #
  #     - name: Deploy to ACI
  #       id: deploy-aci
  #       run: |
  #         RG_NAME="rg-aci-qa-${{ github.run_id }}"
  #         CONTAINER_NAME="aci-qa-${{ github.run_id }}"
  #
  #         az group create \
  #           --name $RG_NAME \
  #           --location eastus \
  #           --tags environment=qa ephemeral=true expires=$(date -d '+8 hours' -Iseconds)
  #
  #         az container create \
  #           --resource-group $RG_NAME \
  #           --name $CONTAINER_NAME \
  #           --image ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ needs.build.outputs.image_tag }} \
  #           --registry-login-server ghcr.io \
  #           --registry-username ${{ github.actor }} \
  #           --registry-password ${{ secrets.GITHUB_TOKEN }} \
  #           --cpu 2 \
  #           --memory 4 \
  #           --ports 8080 \
  #           --ip-address Public \
  #           --environment-variables ENV=qa
  #
  #         CONTAINER_IP=$(az container show -g $RG_NAME -n $CONTAINER_NAME --query ipAddress.ip -o tsv)
  #         echo "container_ip=$CONTAINER_IP" >> $GITHUB_OUTPUT
  #         echo "resource_group=$RG_NAME" >> $GITHUB_OUTPUT
  #
  #     - name: Run E2E tests
  #       run: |
  #         sleep 30
  #         npm ci
  #         npm run test:e2e -- --baseUrl=http://${{ steps.deploy-aci.outputs.container_ip }}:8080

  # ------------------------------------------
  # CLEANUP: Manual Destroy Job
  # ------------------------------------------
  # Trigger manually via workflow_dispatch to destroy specific environment
  #
  # destroy-ephemeral:
  #   if: github.event_name == 'workflow_dispatch'
  #   runs-on: ubuntu-latest
  #   steps:
  #     - name: Azure Login
  #       uses: azure/login@v2
  #       with:
  #         creds: ${{ secrets.AZURE_CREDENTIALS }}
  #
  #     - name: Destroy resource group
  #       run: |
  #         # Input from workflow_dispatch
  #         RG_NAME="${{ github.event.inputs.resource_group }}"
  #
  #         if [ -z "$RG_NAME" ]; then
  #           echo "No resource group specified"
  #           exit 1
  #         fi
  #
  #         echo "Destroying resource group: $RG_NAME"
  #         az group delete --name $RG_NAME --yes --no-wait
  #         echo "Deletion initiated for $RG_NAME"

  # ------------------------------------------
  # CLEANUP: Scheduled Resource Cleanup
  # ------------------------------------------
  # Runs on schedule to clean up expired ephemeral resources
  # Add this as a separate workflow file: .github/workflows/cleanup-ephemeral.yml
  #
  # cleanup-expired:
  #   runs-on: ubuntu-latest
  #   steps:
  #     - name: Azure Login
  #       uses: azure/login@v2
  #       with:
  #         creds: ${{ secrets.AZURE_CREDENTIALS }}
  #
  #     - name: Find and delete expired resources
  #       run: |
  #         echo "Scanning for expired ephemeral resources..."
  #
  #         # Find all resource groups tagged as ephemeral
  #         EXPIRED_GROUPS=$(az group list \
  #           --query "[?tags.ephemeral=='true' && tags.expires < '$(date -Iseconds)'].name" \
  #           -o tsv)
  #
  #         if [ -z "$EXPIRED_GROUPS" ]; then
  #           echo "No expired resources found"
  #           exit 0
  #         fi
  #
  #         for RG in $EXPIRED_GROUPS; do
  #           echo "Deleting expired resource group: $RG"
  #           az group delete --name $RG --yes --no-wait
  #         done
  #
  #         echo "Cleanup initiated for all expired resources"
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

## Ephemeral Azure Environments (Optional)

As an alternative to persistent OVH VMs, you can use ephemeral Azure environments that are created on-demand and destroyed after use.

### Option Comparison

| Aspect | Option A: Azure VMs | Option B: ACI (Containers) |
|--------|---------------------|----------------------------|
| **Use Case** | Complex setups (multi-container, volumes) | Simple container deployments |
| **Startup Time** | ~2-3 minutes | ~30 seconds |
| **Cost** | Higher (full VM) | Lower (pay per second) |
| **Flexibility** | Full VM access, SSH, Docker Compose | Single container only |
| **Best For** | Integration tests, debugging | Smoke tests, quick validation |

### Architecture with Ephemeral Environments

```text
+----------+      +-----------+      +---------------+      +------------------+
| Build    | ---> | Push GHCR | ---> | Create Azure  | ---> | Run Tests        |
|          |      | :dev-<sha>|      | VM or ACI     |      | (smoke/e2e)      |
+----------+      +-----------+      +-------+-------+      +--------+---------+
                                             |                       |
                                             v                       v
                                     +-------+-------+      +--------+---------+
                                     | ephemeral=true|      | Cleanup (manual  |
                                     | expires=+4hrs |      | or scheduled)    |
                                     +---------------+      +------------------+
```

### Cloud-Init for VMs (Option A)

Create `.azure/cloud-init-docker.yml`:

```yaml
#cloud-config
package_update: true
packages:
  - docker.io
  - docker-compose-plugin
runcmd:
  - systemctl enable docker
  - systemctl start docker
  - usermod -aG docker azureuser
```

### Separate Cleanup Workflow

Create `.github/workflows/cleanup-ephemeral.yml`:

```yaml
name: Cleanup Ephemeral Resources

on:
  schedule:
    - cron: '0 */4 * * *'  # Every 4 hours
  workflow_dispatch:
    inputs:
      resource_group:
        description: 'Specific resource group to delete (optional)'
        required: false
        type: string

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Delete specific resource group (manual trigger)
        if: github.event.inputs.resource_group != ''
        run: |
          az group delete --name ${{ github.event.inputs.resource_group }} --yes --no-wait

      - name: Cleanup expired resources (scheduled)
        if: github.event.inputs.resource_group == ''
        run: |
          EXPIRED=$(az group list \
            --query "[?tags.ephemeral=='true' && tags.expires < '$(date -Iseconds)'].name" \
            -o tsv)

          for RG in $EXPIRED; do
            echo "Deleting: $RG"
            az group delete --name $RG --yes --no-wait
          done
```

---

## Security Scanning Layer

The pipeline includes a mandatory security scan that runs **before** any build or deployment. This prevents accidental exposure of sensitive credentials.

### What Gets Detected

| Tool | Detects | Behavior |
|------|---------|----------|
| **Gitleaks** | API keys, tokens, passwords, private keys, AWS/GCP/Azure credentials | Scans commit history and current files |
| **TruffleHog** | Verified secrets (actively checks if credentials are valid) | Deep scan with entropy analysis |

### Common Secrets Caught

- AWS Access Keys (`AKIA...`)
- GitHub Personal Access Tokens (`ghp_...`)
- Private RSA/SSH keys (`-----BEGIN RSA PRIVATE KEY-----`)
- Database connection strings with passwords
- API keys (Stripe, Twilio, SendGrid, etc.)
- JWT secrets and signing keys
- `.env` files accidentally committed

### Handling False Positives

Create a `.gitleaks.toml` file in your repo root:

```toml
# .gitleaks.toml
[allowlist]
description = "Allowlisted patterns"
paths = [
  '''test/fixtures/.*''',
  '''.*_test\.go''',
]
regexes = [
  '''EXAMPLE_API_KEY''',
  '''test-secret-.*''',
]
```

### If Secrets Are Detected

1. **Pipeline blocks** — build will not proceed
2. Review the Gitleaks/TruffleHog output in Actions logs
3. **Rotate the exposed credential immediately**
4. Remove from git history using `git filter-branch` or BFG Repo-Cleaner
5. Add pattern to `.gitleaks.toml` if false positive

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
| `GITLEAKS_LICENSE` | Repository | (Optional) Gitleaks Enterprise license for premium features |

**For Ephemeral Azure Environments (Optional):**

| Secret | Scope | Purpose |
|--------|-------|---------|
| `AZURE_CREDENTIALS` | Repository | Azure Service Principal JSON (see below) |
| `AZURE_VM_SSH_PUB_KEY` | Repository | SSH public key for VM access (Option A only) |

**Creating `AZURE_CREDENTIALS`:**

```bash
# Create service principal with Contributor role
az ad sp create-for-rbac \
  --name "github-actions-ephemeral" \
  --role Contributor \
  --scopes /subscriptions/<SUBSCRIPTION_ID> \
  --sdk-auth

# Copy the JSON output to GitHub secret AZURE_CREDENTIALS
```

---

## Next Steps

1. Set up OVH VMs for DEV and QA **OR** configure Azure ephemeral environments
2. Install Docker and docker-compose on VMs (if using persistent VMs)
3. Configure GitHub environments and secrets
4. Create generic Dockerfile with ENV-based config
5. **Add `.gitleaks.toml` for false positive handling (if needed)**
6. Implement the minimal workflow above
7. **Audit existing repo for secrets before enabling security scan**
8. Add monitoring/alerting to your VMs

**For Ephemeral Azure Setup (Optional):**

9. Create Azure Service Principal and add `AZURE_CREDENTIALS` secret
10. Create `.azure/cloud-init-docker.yml` (if using VMs)
11. Uncomment preferred ephemeral deployment option (VM or ACI)
12. Create `.github/workflows/cleanup-ephemeral.yml` for scheduled cleanup
13. Test cleanup workflow with manual trigger before enabling schedule

---

[Index](./README.md) | [Back: IDP Architecture](./00-idp-architecture.md) | [Azure VMs](./00b-ephemeral-azure-vm.md) | [Azure ACI](./00c-ephemeral-azure-aci.md)
