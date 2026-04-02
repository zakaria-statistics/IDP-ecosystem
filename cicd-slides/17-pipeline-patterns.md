[← Previous](./16-practical-commands.md) | [📋 Index](./README.md) | Next →

---

# Pipeline Patterns & Examples

## Review Apps (Ephemeral Environments)

From `gitlab-ci.yml`:

```yaml
deploy_review:
  stage: deploy-review
  environment:
    name: review/$CI_COMMIT_REF_SLUG
    url: https://${CI_COMMIT_REF_SLUG}.review.example.com
    on_stop: stop_review
    auto_stop_in: 7 days
  script:
    # Create namespace per PR
    - kubectl create namespace review-$CI_COMMIT_REF_SLUG \
        --dry-run=client -o yaml | kubectl apply -f -

    # Deploy with Helm
    - helm upgrade --install review-$CI_COMMIT_REF_SLUG ./helm/app \
        --namespace review-$CI_COMMIT_REF_SLUG \
        --set image.tag=$CI_COMMIT_SHORT_SHA \
        --set ingress.host=$CI_COMMIT_REF_SLUG.review.example.com \
        --wait --timeout=300s
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
```

---

## Cleanup Job

```yaml
stop_review:
  stage: cleanup
  environment:
    name: review/$CI_COMMIT_REF_SLUG
    action: stop
  script:
    - helm uninstall review-$CI_COMMIT_REF_SLUG \
        --namespace review-$CI_COMMIT_REF_SLUG || true
    - kubectl delete namespace review-$CI_COMMIT_REF_SLUG \
        --ignore-not-found=true
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
      when: manual
```

---

## Scheduled Cleanup of Stale Environments

```yaml
cleanup_stale_reviews:
  stage: cleanup
  script:
    - |
      for ns in $(kubectl get namespaces -o name | grep 'review-'); do
        ns_name=$(echo $ns | sed 's|namespace/||')
        age=$(kubectl get namespace $ns_name \
          -o jsonpath='{.metadata.creationTimestamp}')

        # Delete if older than 7 days
        if [ $(date -d "$age" +%s) -lt $(date -d "7 days ago" +%s) ]; then
          echo "Cleaning up stale: $ns_name"
          helm uninstall ${ns_name#review-} --namespace $ns_name || true
          kubectl delete namespace $ns_name --ignore-not-found=true
        fi
      done
  rules:
    - if: $CI_PIPELINE_SOURCE == "schedule"  # Cron trigger
```

---

## Docker Build with Caching

```yaml
docker_build:
  stage: build
  image: docker:24
  services:
    - docker:24-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY

    # Pull previous image for cache
    - docker pull $CI_REGISTRY_IMAGE:latest || true

    # Build with cache
    - docker build
        --cache-from $CI_REGISTRY_IMAGE:latest
        -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
        -t $CI_REGISTRY_IMAGE:latest
        .

    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
    - docker push $CI_REGISTRY_IMAGE:latest
```

---

## Conditional Jobs

```yaml
# Only run on specific file changes
db_migration:
  script:
    - npm run db:migrate
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
      changes:
        - migrations/**/*
        - db/**/*

# Only for stage → main promotion
full_e2e_tests:
  script:
    - npm run test:e2e:full
  rules:
    - if: $CI_MERGE_REQUEST_TARGET_BRANCH_NAME == "main"

# Manual approval gate
deploy_production:
  script:
    - helm upgrade --install app ./chart
  when: manual
  allow_failure: false
```

---

## Secrets Management Pattern

```yaml
variables:
  # Reference GitLab CI/CD variables (masked)
  DATABASE_URL: $PROD_DATABASE_URL

deploy:
  script:
    # Create K8s secret from CI variables
    - kubectl create secret generic app-secrets \
        --from-literal=database-url="$DATABASE_URL" \
        --from-literal=api-key="$API_KEY" \
        --dry-run=client -o yaml | kubectl apply -f -

    # Deploy app (references secret in deployment.yaml)
    - helm upgrade --install myapp ./chart
```

---

## Multi-Environment Pipeline

```yaml
.deploy_template: &deploy_template
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context $KUBE_CONTEXT
    - helm upgrade --install app ./chart \
        --namespace $NAMESPACE \
        --set image.tag=$CI_COMMIT_SHORT_SHA \
        --values ./values-$ENV.yaml \
        --wait

deploy_dev:
  <<: *deploy_template
  variables:
    KUBE_CONTEXT: dev-cluster
    NAMESPACE: dev
    ENV: dev
  rules:
    - if: $CI_COMMIT_BRANCH == "dev"

deploy_staging:
  <<: *deploy_template
  variables:
    KUBE_CONTEXT: staging-cluster
    NAMESPACE: staging
    ENV: staging
  rules:
    - if: $CI_COMMIT_BRANCH == "stage"

deploy_production:
  <<: *deploy_template
  variables:
    KUBE_CONTEXT: prod-cluster
    NAMESPACE: production
    ENV: production
  when: manual
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
```

---

## Pipeline Visualization

```
┌──────────────────────────────────────────────────────────────────┐
│                    FULL GITLAB CI PIPELINE                       │
├─────────┬─────────┬─────────┬─────────┬─────────┬───────────────┤
│validate │  build  │  test   │security │ deploy  │   cleanup     │
├─────────┼─────────┼─────────┼─────────┼─────────┼───────────────┤
│commit   │npm build│lint     │SAST     │review   │stop_review    │
│lint     │docker   │unit     │secrets  │staging  │cleanup_stale  │
│         │build    │integ    │deps     │prod     │               │
└─────────┴─────────┴─────────┴─────────┴─────────┴───────────────┘
```

---

## Key Takeaways

1. **Ephemeral environments** = namespace per PR + auto-cleanup
2. **Conditional jobs** = run only when needed (file changes, branch)
3. **Templates** = DRY pipeline with YAML anchors
4. **Secrets** = CI variables → K8s secrets, never in Git
5. **Manual gates** = `when: manual` for production safety


---

[← Previous](./16-practical-commands.md) | [📋 Index](./README.md) | Next →
