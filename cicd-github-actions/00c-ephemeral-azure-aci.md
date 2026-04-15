[Index](./README.md) | [Back: Azure VM Approach](./00b-ephemeral-azure-vm.md) | [Next: Dual-Path Architecture](./00a-dual-path-architecture.md)

---

# Ephemeral Azure Container Instances (ACI) for Dev/QA

This document details the **Azure Container Instances approach** for ephemeral environments. ACI provides serverless containers with fast startup times and pay-per-second billing.

---

## When to Use ACI

| Scenario | ACI Recommended |
|----------|-----------------|
| Simple single-container deployment | ✅ Yes |
| Fast iteration (< 30s startup) | ✅ Yes |
| Cost-sensitive environments | ✅ Yes |
| Quick smoke tests | ✅ Yes |
| No SSH debugging needed | ✅ Yes |
| Multi-container with Docker Compose | ❌ Use VMs instead |
| Complex networking requirements | ❌ Use VMs instead |
| Persistent volumes during tests | ❌ Use VMs instead |

---

## Architecture Overview

```text
+------------------+
|   GitHub Actions |
+--------+---------+
         |
         | 1. Create Resource Group
         | 2. Create Container Instance
         v
+------------------+     +---------------------------+
|  Azure Resource  |     |   Azure Container Instance|
|  Group           |     +---------------------------+
|                  |     | Your app image            |
| rg-aci-dev-      | --> | Pulled from GHCR          |
|   <run_id>       |     | Public IP assigned        |
|                  |     | Port 8080 exposed         |
| Tags:            |     +---------------------------+
|  ephemeral=true  |              |
|  expires=+4hrs   |              | ~30 seconds
+------------------+              v
         |               +---------------------------+
         |               |   Container Running       |
         |               |   Ready for tests         |
         |               +---------------------------+
         |
         | 3. Run tests
         | 4. Cleanup
         v
+------------------+
|  Delete Resource |
|  Group           |
+------------------+
```

---

## Prerequisites

### 1. Azure Service Principal

```bash
az ad sp create-for-rbac \
  --name "github-actions-aci" \
  --role Contributor \
  --scopes /subscriptions/<YOUR_SUBSCRIPTION_ID> \
  --sdk-auth
```

### 2. GitHub Secrets

| Secret | Value |
|--------|-------|
| `AZURE_CREDENTIALS` | Full JSON from service principal |
| `GITHUB_TOKEN` | Automatic (used for GHCR access) |

**Note**: ACI doesn't require SSH keys - no VM access needed!

---

## GitHub Actions Workflow

### Main Pipeline: `.github/workflows/dev-qa-azure-aci.yml`

```yaml
name: Dev/QA Pipeline (Azure ACI)

on:
  push:
    branches: [dev, stage]
  pull_request:
    branches: [dev, stage]
  workflow_dispatch:
    inputs:
      destroy_rg:
        description: 'Resource group to destroy'
        required: false
        type: string

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ============================================
  # Security Scan
  # ============================================
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Gitleaks - Detect secrets
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  # ============================================
  # Build and Push
  # ============================================
  build:
    needs: security-scan
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    outputs:
      image_tag: ${{ steps.set-tag.outputs.tag }}
      full_image: ${{ steps.set-tag.outputs.full_image }}
    steps:
      - uses: actions/checkout@v4

      - name: Set image tag
        id: set-tag
        run: |
          if [[ "${{ github.ref }}" == "refs/heads/dev" ]]; then
            TAG="dev-${{ github.sha }}"
          elif [[ "${{ github.ref }}" == "refs/heads/stage" ]]; then
            TAG="stage-${{ github.sha }}"
          else
            TAG="pr-${{ github.sha }}"
          fi
          echo "tag=$TAG" >> $GITHUB_OUTPUT
          echo "full_image=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:$TAG" >> $GITHUB_OUTPUT

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
          tags: ${{ steps.set-tag.outputs.full_image }}

  # ============================================
  # Deploy to DEV (ACI)
  # ============================================
  deploy-dev:
    needs: build
    if: github.ref == 'refs/heads/dev'
    runs-on: ubuntu-latest
    environment: dev
    outputs:
      container_ip: ${{ steps.deploy.outputs.container_ip }}
      container_fqdn: ${{ steps.deploy.outputs.container_fqdn }}
      resource_group: ${{ steps.deploy.outputs.rg_name }}
    steps:
      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Deploy to ACI
        id: deploy
        run: |
          RG_NAME="rg-aci-dev-${{ github.run_id }}"
          CONTAINER_NAME="aci-dev-${{ github.run_id }}"
          LOCATION="eastus"
          DNS_LABEL="dev-${{ github.run_id }}"

          echo "Creating resource group: $RG_NAME"
          az group create \
            --name $RG_NAME \
            --location $LOCATION \
            --tags \
              environment=dev \
              ephemeral=true \
              created_by=github-actions \
              run_id=${{ github.run_id }} \
              repo=${{ github.repository }} \
              expires=$(date -u -d '+4 hours' '+%Y-%m-%dT%H:%M:%SZ')

          echo "Creating container instance: $CONTAINER_NAME"
          az container create \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --image ${{ needs.build.outputs.full_image }} \
            --registry-login-server ghcr.io \
            --registry-username ${{ github.actor }} \
            --registry-password ${{ secrets.GITHUB_TOKEN }} \
            --cpu 1 \
            --memory 1.5 \
            --ports 8080 \
            --ip-address Public \
            --dns-name-label $DNS_LABEL \
            --environment-variables \
              ENV=dev \
              LOG_LEVEL=debug \
            --restart-policy OnFailure \
            --output none

          # Wait for container to be running
          echo "Waiting for container to start..."
          az container show \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --query "instanceView.state" \
            --output tsv

          # Get container details
          CONTAINER_IP=$(az container show \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --query "ipAddress.ip" -o tsv)

          CONTAINER_FQDN=$(az container show \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --query "ipAddress.fqdn" -o tsv)

          echo "container_ip=$CONTAINER_IP" >> $GITHUB_OUTPUT
          echo "container_fqdn=$CONTAINER_FQDN" >> $GITHUB_OUTPUT
          echo "rg_name=$RG_NAME" >> $GITHUB_OUTPUT
          echo "container_name=$CONTAINER_NAME" >> $GITHUB_OUTPUT

          echo "Container deployed:"
          echo "  IP: $CONTAINER_IP"
          echo "  FQDN: $CONTAINER_FQDN"

      - name: Wait for container health
        run: |
          echo "Waiting for application to be healthy..."
          CONTAINER_IP="${{ steps.deploy.outputs.container_ip }}"

          for i in {1..30}; do
            if curl -sf "http://$CONTAINER_IP:8080/health" > /dev/null 2>&1; then
              echo "Application is healthy!"
              exit 0
            fi
            echo "Attempt $i/30 - waiting 5s..."
            sleep 5
          done

          echo "Health check failed. Checking container logs..."
          az container logs \
            --resource-group ${{ steps.deploy.outputs.rg_name }} \
            --name aci-dev-${{ github.run_id }}
          exit 1

      - name: Run smoke tests
        run: |
          CONTAINER_IP="${{ steps.deploy.outputs.container_ip }}"
          echo "Running smoke tests against http://$CONTAINER_IP:8080"

          # Health endpoint
          curl -sf "http://$CONTAINER_IP:8080/health" | jq .

          # API status (customize based on your app)
          # curl -sf "http://$CONTAINER_IP:8080/api/status" | jq .

      - name: Output environment info
        run: |
          echo "## DEV Environment (ACI) Ready" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Property | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|----------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| **Container IP** | ${{ steps.deploy.outputs.container_ip }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **FQDN** | ${{ steps.deploy.outputs.container_fqdn }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Resource Group** | ${{ steps.deploy.outputs.rg_name }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **App URL** | http://${{ steps.deploy.outputs.container_ip }}:8080 |" >> $GITHUB_STEP_SUMMARY
          echo "| **Expires** | $(date -u -d '+4 hours' '+%Y-%m-%d %H:%M UTC') |" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "### View Logs" >> $GITHUB_STEP_SUMMARY
          echo '```bash' >> $GITHUB_STEP_SUMMARY
          echo "az container logs -g ${{ steps.deploy.outputs.rg_name }} -n aci-dev-${{ github.run_id }}" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY

  # ============================================
  # Deploy to QA (ACI)
  # ============================================
  deploy-qa:
    needs: build
    if: github.ref == 'refs/heads/stage'
    runs-on: ubuntu-latest
    environment: qa
    outputs:
      container_ip: ${{ steps.deploy.outputs.container_ip }}
      resource_group: ${{ steps.deploy.outputs.rg_name }}
    steps:
      - uses: actions/checkout@v4

      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Deploy to ACI
        id: deploy
        run: |
          RG_NAME="rg-aci-qa-${{ github.run_id }}"
          CONTAINER_NAME="aci-qa-${{ github.run_id }}"
          LOCATION="eastus"
          DNS_LABEL="qa-${{ github.run_id }}"

          az group create \
            --name $RG_NAME \
            --location $LOCATION \
            --tags \
              environment=qa \
              ephemeral=true \
              created_by=github-actions \
              run_id=${{ github.run_id }} \
              repo=${{ github.repository }} \
              expires=$(date -u -d '+8 hours' '+%Y-%m-%dT%H:%M:%SZ')

          # QA gets more resources
          az container create \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --image ${{ needs.build.outputs.full_image }} \
            --registry-login-server ghcr.io \
            --registry-username ${{ github.actor }} \
            --registry-password ${{ secrets.GITHUB_TOKEN }} \
            --cpu 2 \
            --memory 4 \
            --ports 8080 \
            --ip-address Public \
            --dns-name-label $DNS_LABEL \
            --environment-variables \
              ENV=qa \
              LOG_LEVEL=info \
            --restart-policy OnFailure

          CONTAINER_IP=$(az container show -g $RG_NAME -n $CONTAINER_NAME --query "ipAddress.ip" -o tsv)
          echo "container_ip=$CONTAINER_IP" >> $GITHUB_OUTPUT
          echo "rg_name=$RG_NAME" >> $GITHUB_OUTPUT

      - name: Wait for health
        run: |
          sleep 30
          curl --fail --retry 15 --retry-delay 5 \
            "http://${{ steps.deploy.outputs.container_ip }}:8080/health"

      - name: Run E2E tests
        run: |
          npm ci
          npm run test:e2e -- --baseUrl=http://${{ steps.deploy.outputs.container_ip }}:8080

      - name: Output environment info
        run: |
          echo "## QA Environment (ACI) Ready" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Property | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|----------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| **Container IP** | ${{ steps.deploy.outputs.container_ip }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Resource Group** | ${{ steps.deploy.outputs.rg_name }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **App URL** | http://${{ steps.deploy.outputs.container_ip }}:8080 |" >> $GITHUB_STEP_SUMMARY
          echo "| **Expires** | $(date -u -d '+8 hours' '+%Y-%m-%d %H:%M UTC') |" >> $GITHUB_STEP_SUMMARY

  # ============================================
  # Manual Destroy
  # ============================================
  destroy:
    if: github.event_name == 'workflow_dispatch' && github.event.inputs.destroy_rg != ''
    runs-on: ubuntu-latest
    steps:
      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Destroy resource group
        run: |
          RG_NAME="${{ github.event.inputs.destroy_rg }}"

          # Safety check
          EPHEMERAL=$(az group show --name $RG_NAME --query "tags.ephemeral" -o tsv 2>/dev/null || echo "")
          if [[ "$EPHEMERAL" != "true" ]]; then
            echo "ERROR: $RG_NAME is not tagged as ephemeral"
            exit 1
          fi

          az group delete --name $RG_NAME --yes --no-wait
          echo "Deletion initiated for $RG_NAME"
```

---

## Multi-Container with ACI (Container Groups)

For applications needing multiple containers (app + sidecar), use container groups:

```yaml
      - name: Deploy container group
        run: |
          az container create \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --image ${{ needs.build.outputs.full_image }} \
            --registry-login-server ghcr.io \
            --registry-username ${{ github.actor }} \
            --registry-password ${{ secrets.GITHUB_TOKEN }} \
            --cpu 2 \
            --memory 4 \
            --ports 8080 \
            --ip-address Public \
            --environment-variables ENV=dev
```

### Using YAML for Container Groups

For complex setups, use a YAML definition:

```yaml
# .azure/container-group.yml
apiVersion: '2021-10-01'
location: eastus
name: app-container-group
properties:
  containers:
    - name: app
      properties:
        image: ghcr.io/your-org/your-app:latest
        ports:
          - port: 8080
        resources:
          requests:
            cpu: 1
            memoryInGb: 1.5
        environmentVariables:
          - name: ENV
            value: dev
          - name: REDIS_HOST
            value: localhost
    - name: redis
      properties:
        image: redis:7-alpine
        ports:
          - port: 6379
        resources:
          requests:
            cpu: 0.5
            memoryInGb: 0.5
  osType: Linux
  ipAddress:
    type: Public
    ports:
      - port: 8080
        protocol: TCP
  restartPolicy: OnFailure
tags:
  ephemeral: 'true'
  environment: dev
```

Deploy with:

```bash
az container create \
  --resource-group $RG_NAME \
  --file .azure/container-group.yml
```

---

## Cleanup Workflow

Create `.github/workflows/cleanup-azure-aci.yml`:

```yaml
name: Cleanup Ephemeral ACI

on:
  schedule:
    - cron: '0 */2 * * *'  # Every 2 hours
  workflow_dispatch:
    inputs:
      dry_run:
        description: 'Dry run mode'
        type: boolean
        default: false

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Cleanup expired containers
        run: |
          NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
          DRY_RUN="${{ github.event.inputs.dry_run }}"

          echo "Current time: $NOW"
          echo "Scanning for expired ACI resource groups..."

          # Find expired ephemeral groups
          EXPIRED=$(az group list \
            --query "[?tags.ephemeral=='true' && tags.expires < '$NOW'].name" \
            -o tsv)

          if [ -z "$EXPIRED" ]; then
            echo "No expired resources found"
            exit 0
          fi

          echo "Expired resource groups:"
          echo "$EXPIRED"

          if [[ "$DRY_RUN" == "true" ]]; then
            echo "DRY RUN - skipping deletion"
            exit 0
          fi

          for RG in $EXPIRED; do
            echo "Deleting: $RG"
            az group delete --name $RG --yes --no-wait
          done

      - name: Report
        run: |
          echo "## ACI Cleanup Report" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY

          ACTIVE=$(az group list \
            --query "[?tags.ephemeral=='true' && starts_with(name, 'rg-aci-')].{Name:name, Expires:tags.expires, Env:tags.environment}" \
            -o table)

          echo "### Active ACI Environments" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          echo "$ACTIVE" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
```

---

## ACI Resource Limits

| Resource | Minimum | Maximum | Default |
|----------|---------|---------|---------|
| CPU cores | 0.1 | 4 | 1 |
| Memory (GB) | 0.1 | 16 | 1.5 |
| Containers per group | 1 | 60 | 1 |
| Ports per container | 1 | 5 | - |

### Recommended Configurations

| Environment | CPU | Memory | Estimated Cost/hr |
|-------------|-----|--------|-------------------|
| DEV (smoke) | 1 | 1.5 GB | ~$0.05 |
| QA (e2e) | 2 | 4 GB | ~$0.12 |
| QA (load) | 4 | 8 GB | ~$0.25 |

---

## Debugging ACI Containers

### View Container Logs

```bash
# Stream logs
az container logs \
  --resource-group rg-aci-dev-12345 \
  --name aci-dev-12345 \
  --follow

# Get recent logs
az container logs \
  --resource-group rg-aci-dev-12345 \
  --name aci-dev-12345 \
  --tail 100
```

### Execute Commands in Container

```bash
# Open interactive shell
az container exec \
  --resource-group rg-aci-dev-12345 \
  --name aci-dev-12345 \
  --exec-command "/bin/sh"

# Run specific command
az container exec \
  --resource-group rg-aci-dev-12345 \
  --name aci-dev-12345 \
  --exec-command "env"
```

### Check Container Events

```bash
az container show \
  --resource-group rg-aci-dev-12345 \
  --name aci-dev-12345 \
  --query "containers[0].instanceView.events" \
  -o table
```

---

## ACI vs VM Comparison

| Feature | ACI | Azure VM |
|---------|-----|----------|
| **Startup time** | ~30 seconds | ~2-3 minutes |
| **Cost model** | Per-second billing | Per-hour billing |
| **Min cost** | ~$0.00001/sec | ~$0.04/hr |
| **SSH access** | No (use `az container exec`) | Yes |
| **Docker Compose** | No | Yes |
| **Multi-container** | Yes (container groups) | Yes |
| **Persistent storage** | Azure Files mount | Full disk |
| **Max resources** | 4 CPU, 16 GB RAM | Unlimited |
| **Network control** | Basic | Full VNet support |
| **Best for** | Quick tests, CI/CD | Complex setups, debugging |

---

## Advanced: ACI with Azure Files (Persistent Storage)

```yaml
      - name: Create storage for ACI
        run: |
          STORAGE_ACCOUNT="staci${{ github.run_id }}"
          SHARE_NAME="appdata"

          # Create storage account
          az storage account create \
            --resource-group $RG_NAME \
            --name $STORAGE_ACCOUNT \
            --sku Standard_LRS

          # Get storage key
          STORAGE_KEY=$(az storage account keys list \
            --resource-group $RG_NAME \
            --account-name $STORAGE_ACCOUNT \
            --query "[0].value" -o tsv)

          # Create file share
          az storage share create \
            --name $SHARE_NAME \
            --account-name $STORAGE_ACCOUNT

          # Create container with mounted volume
          az container create \
            --resource-group $RG_NAME \
            --name $CONTAINER_NAME \
            --image $IMAGE \
            --azure-file-volume-account-name $STORAGE_ACCOUNT \
            --azure-file-volume-account-key $STORAGE_KEY \
            --azure-file-volume-share-name $SHARE_NAME \
            --azure-file-volume-mount-path /data \
            # ... other options
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Container stuck in "Waiting" | Check image name, registry credentials |
| "ImagePullFailed" | Verify GHCR token has `read:packages` scope |
| Container keeps restarting | Check logs for crash reason, increase resources |
| Cannot connect to port | Verify port is exposed in both container and ACI config |
| DNS name conflict | Use unique DNS label per run (`--dns-name-label`) |
| "QuotaExceeded" | Request quota increase or use different region |

### Common Log Checks

```bash
# Container state
az container show -g $RG -n $NAME --query "instanceView.state"

# Container events (errors)
az container show -g $RG -n $NAME --query "containers[0].instanceView.events[-3:]"

# Full container details
az container show -g $RG -n $NAME
```

---

## Security Considerations

1. **Registry auth**: GHCR token is short-lived, passed via `--registry-password`
2. **No SSH**: Reduces attack surface vs VMs
3. **Isolated network**: Each container group gets its own IP
4. **Secrets**: Use Azure Key Vault for sensitive env vars in production
5. **No root by default**: Containers run as non-root unless specified

### Using Azure Key Vault for Secrets

```bash
az container create \
  --resource-group $RG_NAME \
  --name $CONTAINER_NAME \
  --image $IMAGE \
  --secure-environment-variables \
    DB_PASSWORD=${{ secrets.DB_PASSWORD }} \
    API_KEY=${{ secrets.API_KEY }}
```

---

[Index](./README.md) | [Back: Azure VM Approach](./00b-ephemeral-azure-vm.md) | [Next: Dual-Path Architecture](./00a-dual-path-architecture.md)
