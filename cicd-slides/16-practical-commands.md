[← Previous](./15-iac-config-management.md) | [📋 Index](./README.md) | [Next →](./17-pipeline-patterns.md)

---

# Practical CI/CD Commands

## Docker Commands

```bash
# Build image with tag
docker build -t myapp:1.4.2 .
docker build -t myapp:$CI_COMMIT_SHORT_SHA .

# Tag for registry
docker tag myapp:1.4.2 registry.example.com/myapp:1.4.2

# Push to registry
docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
docker push registry.example.com/myapp:1.4.2

# Run container
docker run -d -p 8080:8080 --name myapp myapp:1.4.2

# Cleanup
docker system prune -af  # Remove unused images
```

---

## Kubectl Commands

```bash
# Context & config
kubectl config use-context production
kubectl config get-contexts

# Deployments
kubectl get deployments -n production
kubectl rollout status deployment/myapp -n production
kubectl rollout undo deployment/myapp -n production    # ROLLBACK!
kubectl rollout history deployment/myapp -n production

# Pods & logs
kubectl get pods -n production
kubectl logs -f deployment/myapp -n production
kubectl exec -it pod/myapp-xxx -- /bin/sh

# Apply manifests
kubectl apply -f deployment.yaml
kubectl delete -f deployment.yaml
```

---

## Helm Commands

```bash
# Install / Upgrade
helm upgrade --install myapp ./chart \
  --namespace production \
  --set image.tag=1.4.2 \
  --values values-prod.yaml \
  --wait --timeout=600s

# Rollback
helm rollback myapp 1 -n production   # Rollback to revision 1
helm history myapp -n production      # See revisions

# Uninstall
helm uninstall myapp -n production

# Debug
helm template myapp ./chart --values values.yaml  # Preview manifests
helm get values myapp -n production               # See current values
```

---

## Terraform Commands

```bash
# Init & plan
terraform init
terraform plan -out=tfplan
terraform plan -var="environment=staging"

# Apply
terraform apply tfplan
terraform apply -auto-approve  # Skip confirmation (CI/CD)

# State management
terraform state list
terraform state show aws_instance.app
terraform import aws_instance.app i-1234567890

# Destroy
terraform destroy -target=aws_instance.app  # Specific resource
```

---

## Git Commands for CI/CD

```bash
# Get info for tagging/versioning
git rev-parse --short HEAD              # abc1234
git describe --tags --always            # v1.4.2-3-gabc1234
git log -1 --format='%H'                # Full SHA

# Tagging releases
git tag -a v1.4.2 -m "Release v1.4.2"
git push origin v1.4.2

# Check changes (for conditional builds)
git diff --name-only HEAD~1             # Changed files
git diff --name-only $CI_MERGE_REQUEST_DIFF_BASE_SHA HEAD
```

---

## Useful CI/CD Script Patterns

### Wait for deployment
```bash
kubectl rollout status deployment/myapp -n production --timeout=300s
```

### Health check
```bash
for i in {1..30}; do
  if curl -sf https://myapp.example.com/health; then
    echo "App is healthy"
    exit 0
  fi
  sleep 10
done
echo "Health check failed"
exit 1
```

### Notify Slack
```bash
curl -X POST "$SLACK_WEBHOOK_URL" \
  -H 'Content-Type: application/json' \
  -d "{\"text\":\"Deployed $CI_COMMIT_SHORT_SHA to production\"}"
```

---

## Environment Variables in CI/CD

| Variable | Source | Example |
|----------|--------|---------|
| `CI_COMMIT_SHORT_SHA` | GitLab CI | `abc1234` |
| `CI_COMMIT_REF_SLUG` | GitLab CI | `feature-auth` |
| `CI_ENVIRONMENT_NAME` | GitLab CI | `production` |
| `DOCKER_IMAGE` | Custom | `registry/app:tag` |
| `KUBE_CONTEXT` | Custom | `prod-cluster` |

```yaml
variables:
  DOCKER_IMAGE: ${CI_REGISTRY_IMAGE}:${CI_COMMIT_SHORT_SHA}
```


---

[← Previous](./15-iac-config-management.md) | [📋 Index](./README.md) | [Next →](./17-pipeline-patterns.md)
