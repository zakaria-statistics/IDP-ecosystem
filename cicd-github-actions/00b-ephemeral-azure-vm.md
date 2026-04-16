[Index](./README.md) | [Back: Dual-Path Architecture](./00a-dual-path-architecture.md) | [Next: ACI Approach](./00c-ephemeral-azure-aci.md)

---

# Ephemeral Azure VMs for Dev/QA

This document details the **Azure VM approach** for ephemeral development and QA environments. VMs provide full control, Docker Compose support, and SSH access for debugging.

---

## When to Use Azure VMs

| Scenario | VM Recommended |
|----------|----------------|
| Multi-container setups (app + db + redis + queue) | ✅ Yes |
| Need Docker Compose | ✅ Yes |
| SSH debugging required | ✅ Yes |
| Volume mounts and persistent storage during tests | ✅ Yes |
| Complex networking (multiple services) | ✅ Yes |
| Simple single-container deployment | ❌ Use ACI instead |
| Cost-sensitive, short-lived tests | ❌ Use ACI instead |

---

## Architecture Overview

```text
+------------------+
|   GitHub Actions |
+--------+---------+
         |
         | 1. Create Resource Group
         | 2. Provision VM
         | 3. Cloud-init installs Docker
         v
+------------------+     +---------------------------+
|  Azure Resource  |     |      Ephemeral VM         |
|  Group           |     +---------------------------+
|                  |     | Ubuntu 22.04              |
| rg-ephemeral-    |     | Docker Engine             |
|   dev-<run_id>   | --> | Docker Compose            |
|                  |     | Your app containers       |
| Tags:            |     |   - app                   |
|  ephemeral=true  |     |   - postgres              |
|  expires=+4hrs   |     |   - redis                 |
+------------------+     +---------------------------+
         |
         | 4. Deploy via SSH
         | 5. Run tests
         | 6. Cleanup (manual/scheduled)
         v
+------------------+
|  Delete Resource |
|  Group           |
+------------------+
```

---

## Prerequisites

### 1. Azure Service Principal

Create a service principal with Contributor access:

```bash
# Login to Azure
az login

# Create service principal
az ad sp create-for-rbac \
  --name "github-actions-ephemeral-vms" \
  --role Contributor \
  --scopes /subscriptions/<YOUR_SUBSCRIPTION_ID> \
  --sdk-auth
```

Output (save as `AZURE_CREDENTIALS` secret):

```json
{
  "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "clientSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "activeDirectoryEndpointUrl": "https://login.microsoftonline.com",
  "resourceManagerEndpointUrl": "https://management.azure.com/",
  "activeDirectoryGraphResourceId": "https://graph.windows.net/",
  "sqlManagementEndpointUrl": "https://management.core.windows.net:8443/",
  "galleryEndpointUrl": "https://gallery.azure.com/",
  "managementEndpointUrl": "https://management.core.windows.net/"
}
```

### 2. SSH Key Pair

Generate an SSH key pair for VM access:

```bash
# Generate key pair
ssh-keygen -t rsa -b 4096 -f ~/.ssh/azure-ephemeral -N ""

# View public key (add to AZURE_VM_SSH_PUB_KEY secret)
cat ~/.ssh/azure-ephemeral.pub

# View private key (add to AZURE_VM_SSH_KEY secret)
cat ~/.ssh/azure-ephemeral
```

### 3. GitHub Secrets

| Secret | Value |
|--------|-------|
| `AZURE_CREDENTIALS` | Full JSON from service principal creation |
| `AZURE_VM_SSH_PUB_KEY` | Contents of `azure-ephemeral.pub` |
| `AZURE_VM_SSH_KEY` | Contents of `azure-ephemeral` (private key) |

---

## Cloud-Init Configuration

Create `.azure/cloud-init-docker.yml` to bootstrap VMs with Docker:

```yaml
#cloud-config
package_update: true
package_upgrade: true

packages:
  - apt-transport-https
  - ca-certificates
  - curl
  - gnupg
  - lsb-release
  - jq

runcmd:
  # Install Docker
  - curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
  - echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
  - apt-get update
  - apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

  # Configure Docker
  - systemctl enable docker
  - systemctl start docker
  - usermod -aG docker azureuser

  # Create app directory
  - mkdir -p /opt/app
  - chown azureuser:azureuser /opt/app

  # Signal readiness
  - touch /tmp/cloud-init-complete

# Write docker-compose template
write_files:
  - path: /opt/app/docker-compose.yml
    permissions: '0644'
    content: |
      services:
        app:
          image: ${APP_IMAGE:-ghcr.io/your-org/your-app:latest}
          ports:
            - "8080:8080"
          environment:
            - DATABASE_URL=postgresql://user:pass@db:5432/app
            - REDIS_URL=redis://redis:6379
            - ENV=${ENV:-dev}
          depends_on:
            db:
              condition: service_healthy
            redis:
              condition: service_healthy
          healthcheck:
            test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
            interval: 10s
            timeout: 5s
            retries: 5
          db:
            image: postgres:15-alpine
            environment:
              POSTGRES_USER: user
              POSTGRES_PASSWORD: pass
              POSTGRES_DB: app
            healthcheck:
              test: ["CMD-SHELL", "pg_isready -U user -d app"]
              interval: 5s
              timeout: 3s
              retries: 5
          redis:
            image: redis:7-alpine
            healthcheck:
              test: ["CMD", "redis-cli", "ping"]
              interval: 5s
              timeout: 3s
              retries: 5
```

---

## GitHub Actions Workflow

### Main Pipeline: `.github/workflows/dev-qa-azure-vm.yml`

```yaml
name: Dev/QA Pipeline (Azure VMs)

on:
  push:
    branches: [dev, stage]
  pull_request:
    branches: [dev, stage]
  workflow_dispatch:
    inputs:
      destroy_rg:
        description: 'Resource group to destroy (for manual cleanup)'
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
  # Deploy to DEV (Ephemeral Azure VM)
  # ============================================
  deploy-dev:
    needs: build
    if: github.ref == 'refs/heads/dev'
    runs-on: ubuntu-latest
    environment: dev
    outputs:
      vm_ip: ${{ steps.create-vm.outputs.vm_ip }}
      resource_group: ${{ steps.create-vm.outputs.rg_name }}
    steps:
      - uses: actions/checkout@v4

      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Create ephemeral VM
        id: create-vm
        run: |
          RG_NAME="rg-ephemeral-dev-${{ github.run_id }}"
          VM_NAME="vm-dev-${{ github.run_id }}"
          LOCATION="eastus"

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

          echo "Creating VM: $VM_NAME"
          az vm create \
            --resource-group $RG_NAME \
            --name $VM_NAME \
            --image Ubuntu2204 \
            --size Standard_B2s \
            --admin-username azureuser \
            --ssh-key-values "${{ secrets.AZURE_VM_SSH_PUB_KEY }}" \
            --custom-data .azure/cloud-init-docker.yml \
            --public-ip-sku Standard \
            --nsg-rule SSH \
            --output none

          # Open port 8080 for the application
          az vm open-port \
            --resource-group $RG_NAME \
            --name $VM_NAME \
            --port 8080 \
            --priority 1010

          # Get public IP
          VM_IP=$(az vm show -d -g $RG_NAME -n $VM_NAME --query publicIps -o tsv)

          echo "vm_ip=$VM_IP" >> $GITHUB_OUTPUT
          echo "rg_name=$RG_NAME" >> $GITHUB_OUTPUT
          echo "VM created with IP: $VM_IP"

      - name: Setup SSH key
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.AZURE_VM_SSH_KEY }}" > ~/.ssh/azure-ephemeral
          chmod 600 ~/.ssh/azure-ephemeral

      - name: Wait for cloud-init to complete
        run: |
          echo "Waiting for VM to be ready..."
          for i in {1..60}; do
            if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
               -i ~/.ssh/azure-ephemeral \
               azureuser@${{ steps.create-vm.outputs.vm_ip }} \
               "test -f /tmp/cloud-init-complete && docker --version" 2>/dev/null; then
              echo "VM is ready!"
              exit 0
            fi
            echo "Attempt $i/60 - waiting 10s..."
            sleep 10
          done
          echo "Timeout waiting for VM"
          exit 1

      - name: Deploy application stack
        env:
          VM_IP: ${{ steps.create-vm.outputs.vm_ip }}
          IMAGE: ${{ needs.build.outputs.full_image }}
        run: |
          ssh -o StrictHostKeyChecking=no -i ~/.ssh/azure-ephemeral azureuser@$VM_IP << EOF
            set -e
            cd /opt/app

            # Login to GHCR
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin

            # Set environment variables
            export APP_IMAGE=$IMAGE
            export ENV=dev

            # Update docker-compose with actual image
            sed -i "s|\\\${APP_IMAGE:-.*}|$IMAGE|g" docker-compose.yml

            # Pull and start services
            docker compose pull
            docker compose up -d

            # Show running containers
            docker compose ps
          EOF

      - name: Wait for application health
        run: |
          echo "Waiting for application to be healthy..."
          sleep 15
          for i in {1..20}; do
            if curl -sf http://${{ steps.create-vm.outputs.vm_ip }}:8080/health; then
              echo "Application is healthy!"
              exit 0
            fi
            echo "Attempt $i/20 - waiting..."
            sleep 5
          done
          echo "Application health check failed"
          exit 1

      - name: Run smoke tests
        run: |
          echo "Running smoke tests against http://${{ steps.create-vm.outputs.vm_ip }}:8080"

          # Health check
          curl -sf http://${{ steps.create-vm.outputs.vm_ip }}:8080/health

          # Add more smoke tests here
          # curl -sf http://${{ steps.create-vm.outputs.vm_ip }}:8080/api/status

      - name: Output environment info
        run: |
          echo "## DEV Environment Ready" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Property | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|----------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| **VM IP** | ${{ steps.create-vm.outputs.vm_ip }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Resource Group** | ${{ steps.create-vm.outputs.rg_name }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **App URL** | http://${{ steps.create-vm.outputs.vm_ip }}:8080 |" >> $GITHUB_STEP_SUMMARY
          echo "| **SSH Access** | \`ssh azureuser@${{ steps.create-vm.outputs.vm_ip }}\` |" >> $GITHUB_STEP_SUMMARY
          echo "| **Expires** | $(date -u -d '+4 hours' '+%Y-%m-%d %H:%M UTC') |" >> $GITHUB_STEP_SUMMARY

  # ============================================
  # Deploy to QA (Ephemeral Azure VM)
  # ============================================
  deploy-qa:
    needs: build
    if: github.ref == 'refs/heads/stage'
    runs-on: ubuntu-latest
    environment: qa
    outputs:
      vm_ip: ${{ steps.create-vm.outputs.vm_ip }}
      resource_group: ${{ steps.create-vm.outputs.rg_name }}
    steps:
      - uses: actions/checkout@v4

      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Create ephemeral VM
        id: create-vm
        run: |
          RG_NAME="rg-ephemeral-qa-${{ github.run_id }}"
          VM_NAME="vm-qa-${{ github.run_id }}"
          LOCATION="eastus"

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

          # QA gets a larger VM for heavier testing
          az vm create \
            --resource-group $RG_NAME \
            --name $VM_NAME \
            --image Ubuntu2204 \
            --size Standard_B2ms \
            --admin-username azureuser \
            --ssh-key-values "${{ secrets.AZURE_VM_SSH_PUB_KEY }}" \
            --custom-data .azure/cloud-init-docker.yml \
            --public-ip-sku Standard \
            --nsg-rule SSH \
            --output none

          az vm open-port -g $RG_NAME -n $VM_NAME --port 8080 --priority 1010

          VM_IP=$(az vm show -d -g $RG_NAME -n $VM_NAME --query publicIps -o tsv)
          echo "vm_ip=$VM_IP" >> $GITHUB_OUTPUT
          echo "rg_name=$RG_NAME" >> $GITHUB_OUTPUT

      - name: Setup SSH key
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.AZURE_VM_SSH_KEY }}" > ~/.ssh/azure-ephemeral
          chmod 600 ~/.ssh/azure-ephemeral

      - name: Wait for cloud-init
        run: |
          for i in {1..60}; do
            if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
               -i ~/.ssh/azure-ephemeral \
               azureuser@${{ steps.create-vm.outputs.vm_ip }} \
               "test -f /tmp/cloud-init-complete" 2>/dev/null; then
              echo "VM ready!"
              exit 0
            fi
            sleep 10
          done
          exit 1

      - name: Deploy application
        env:
          VM_IP: ${{ steps.create-vm.outputs.vm_ip }}
          IMAGE: ${{ needs.build.outputs.full_image }}
        run: |
          ssh -o StrictHostKeyChecking=no -i ~/.ssh/azure-ephemeral azureuser@$VM_IP << EOF
            cd /opt/app
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
            sed -i "s|\\\${APP_IMAGE:-.*}|$IMAGE|g" docker-compose.yml
            export ENV=qa
            docker compose pull
            docker compose up -d
          EOF

      - name: Wait for health
        run: |
          sleep 20
          curl --fail --retry 10 --retry-delay 5 \
            http://${{ steps.create-vm.outputs.vm_ip }}:8080/health

      - name: Run E2E tests
        run: |
          npm ci
          npm run test:e2e -- --baseUrl=http://${{ steps.create-vm.outputs.vm_ip }}:8080

      - name: Output environment info
        run: |
          echo "## QA Environment Ready" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Property | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|----------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| **VM IP** | ${{ steps.create-vm.outputs.vm_ip }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Resource Group** | ${{ steps.create-vm.outputs.rg_name }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **App URL** | http://${{ steps.create-vm.outputs.vm_ip }}:8080 |" >> $GITHUB_STEP_SUMMARY
          echo "| **Expires** | $(date -u -d '+8 hours' '+%Y-%m-%d %H:%M UTC') |" >> $GITHUB_STEP_SUMMARY

  # ============================================
  # Manual Destroy (workflow_dispatch)
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
          echo "Destroying resource group: $RG_NAME"

          # Verify it's an ephemeral resource group
          EPHEMERAL=$(az group show --name $RG_NAME --query "tags.ephemeral" -o tsv 2>/dev/null || echo "")

          if [[ "$EPHEMERAL" != "true" ]]; then
            echo "ERROR: Resource group $RG_NAME is not tagged as ephemeral. Refusing to delete."
            exit 1
          fi

          az group delete --name $RG_NAME --yes --no-wait
          echo "Deletion initiated for $RG_NAME"
```

---

## Cleanup Workflow

Create `.github/workflows/cleanup-azure-vms.yml`:

```yaml
name: Cleanup Ephemeral Azure VMs

on:
  schedule:
    # Run every 2 hours
    - cron: '0 */2 * * *'
  workflow_dispatch:
    inputs:
      dry_run:
        description: 'Dry run (list but do not delete)'
        required: false
        type: boolean
        default: false
      max_age_hours:
        description: 'Delete resources older than X hours (overrides expires tag)'
        required: false
        type: number

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Azure Login
        uses: azure/login@v2
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Find and cleanup expired VMs
        run: |
          echo "Scanning for expired ephemeral resources..."
          NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
          DRY_RUN="${{ github.event.inputs.dry_run }}"
          MAX_AGE="${{ github.event.inputs.max_age_hours }}"

          # Get all ephemeral resource groups
          GROUPS=$(az group list \
            --query "[?tags.ephemeral=='true'].{name:name, expires:tags.expires, env:tags.environment, created:tags.run_id}" \
            -o json)

          echo "Found ephemeral resource groups:"
          echo "$GROUPS" | jq -r '.[] | "  - \(.name) (env: \(.env), expires: \(.expires))"'

          # Filter expired ones
          EXPIRED=$(echo "$GROUPS" | jq -r --arg now "$NOW" \
            '[.[] | select(.expires != null and .expires < $now)] | .[].name')

          if [ -z "$EXPIRED" ]; then
            echo "No expired resources found"
            exit 0
          fi

          echo ""
          echo "Expired resource groups to delete:"
          echo "$EXPIRED"

          if [[ "$DRY_RUN" == "true" ]]; then
            echo ""
            echo "DRY RUN - No resources will be deleted"
            exit 0
          fi

          # Delete expired groups
          for RG in $EXPIRED; do
            echo "Deleting: $RG"
            az group delete --name $RG --yes --no-wait
          done

          echo ""
          echo "Cleanup complete. Deletion initiated for all expired resources."

      - name: Generate cleanup report
        run: |
          echo "## Cleanup Report" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "Run time: $(date -u '+%Y-%m-%d %H:%M UTC')" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY

          # List remaining ephemeral resources
          REMAINING=$(az group list \
            --query "[?tags.ephemeral=='true'].{name:name, expires:tags.expires, env:tags.environment}" \
            -o table)

          echo "### Remaining Ephemeral Resources" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          echo "$REMAINING" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
```

---

## VM Size Recommendations

| Environment | VM Size | vCPUs | RAM | Cost/hr* | Use Case |
|-------------|---------|-------|-----|----------|----------|
| DEV | Standard_B2s | 2 | 4 GB | ~$0.04 | Basic smoke tests |
| QA | Standard_B2ms | 2 | 8 GB | ~$0.08 | E2E tests, load tests |
| QA (heavy) | Standard_B4ms | 4 | 16 GB | ~$0.16 | Performance testing |

*Approximate costs, varies by region

---

## Debugging Ephemeral VMs

### SSH into Running VM

```bash
# From workflow output or Azure portal
ssh azureuser@<VM_IP>

# View application logs
cd /opt/app
docker compose logs -f

# Check container status
docker compose ps

# Restart services
docker compose restart
```

### View Cloud-Init Logs

```bash
# Check cloud-init status
cloud-init status

# View cloud-init output
sudo cat /var/log/cloud-init-output.log

# Check if Docker installed correctly
docker --version
docker compose version
```

---

## Cost Optimization

1. **Short expiry times**: DEV = 4 hours, QA = 8 hours max
2. **Auto-shutdown**: VMs are deleted, not stopped (no storage costs)
3. **Right-size VMs**: Use B-series (burstable) for intermittent workloads
4. **Region selection**: Choose cheapest region for non-production
5. **Spot instances** (optional): Add `--priority Spot` for up to 90% savings

### Using Spot Instances

```bash
az vm create \
  --resource-group $RG_NAME \
  --name $VM_NAME \
  --image Ubuntu2204 \
  --size Standard_B2s \
  --priority Spot \
  --eviction-policy Delete \
  --max-price 0.02 \
  # ... rest of options
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| VM creation timeout | Check Azure service health, try different region |
| Cloud-init never completes | SSH in and check `/var/log/cloud-init-output.log` |
| Docker not starting | Verify cloud-init script, check systemd logs |
| Cannot pull from GHCR | Verify GITHUB_TOKEN has `packages:read` permission |
| SSH connection refused | Wait longer for VM boot, check NSG rules |
| Health check fails | Check container logs, verify port 8080 is exposed |

---

## Security Considerations

1. **SSH keys**: Rotate keys regularly, use separate keys per environment
2. **NSG rules**: Only open required ports (22, 8080)
3. **Secrets**: Never log secrets, use GitHub secrets exclusively
4. **Network isolation**: Consider VNet for production-like testing
5. **Image scanning**: Scan Docker images before deployment

---

[Index](./README.md) | [Back: Dual-Path Architecture](./00a-dual-path-architecture.md) | [Next: ACI Approach](./00c-ephemeral-azure-aci.md)
