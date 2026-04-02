[Previous: Environment Strategy](./05-environment-strategy.md) | [Index](./README.md) | [Next: Golden Rules](./07-golden-rules.md)

---

# Workflow Examples

## Example 1: Spring Boot CI

```yaml
# .github/workflows/spring-ci.yml
name: Spring Boot CI

on:
  push:
    branches: [main, dev, stage]
  pull_request:
    branches: [main, dev]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Run tests
        run: mvn test
```

---

## Example 2: Next.js CI

```yaml
# .github/workflows/nextjs-ci.yml
name: Next.js CI

on:
  push:
    branches: [main, dev, stage]
  pull_request:
    branches: [main, dev]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Build
        run: npm run build

      - name: Test
        run: npm test
```

---

## Example 3: Python CI

```yaml
# .github/workflows/python-ci.yml
name: Python CI

on:
  push:
    branches: [main, dev, stage]
  pull_request:
    branches: [main, dev]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: 'pip'

      - name: Install dependencies
        run: |
          pip install -r requirements.txt
          pip install pytest ruff

      - name: Lint
        run: ruff check .

      - name: Test
        run: pytest
```

---

## Example 4: Docker Build & Push

```yaml
# .github/workflows/docker.yml
name: Docker Build

on:
  push:
    branches: [main, dev, stage]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ghcr.io/${{ github.repository }}:${{ github.sha }}
            ghcr.io/${{ github.repository }}:${{ github.ref_name }}
```

---

## Example 5: Complete CI/CD Pipeline

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, dev, stage]
  pull_request:
    branches: [main, dev]

jobs:
  # ===== CI Jobs =====
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run lint

  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm test

  # ===== Build =====
  build:
    needs: [lint, test]
    runs-on: ubuntu-latest
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
          tags: ghcr.io/${{ github.repository }}:${{ github.sha }}

  # ===== CD Jobs =====
  deploy-dev:
    if: github.ref == 'refs/heads/dev'
    needs: build
    runs-on: ubuntu-latest
    environment: development
    steps:
      - run: echo "Deploying to dev..."

  deploy-stage:
    if: github.ref == 'refs/heads/stage'
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - run: echo "Deploying to stage..."

  deploy-prod:
    if: github.ref == 'refs/heads/main'
    needs: build
    runs-on: ubuntu-latest
    environment: production  # Requires approval!
    steps:
      - run: echo "Deploying to production..."
```

---

## Example 6: Security Scanning

```yaml
name: Security

on:
  push:
    branches: [main, dev]
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday

jobs:
  codeql:
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    steps:
      - uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: javascript, java

      - name: Build (if needed)
        run: npm run build

      - name: Perform Analysis
        uses: github/codeql-action/analyze@v3
```

---

## Example 7: Manual Deployment

```yaml
name: Manual Deploy

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy'
        required: true
        type: choice
        options:
          - dev
          - stage
          - prod
      image_tag:
        description: 'Image tag to deploy'
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment }}
    steps:
      - name: Deploy
        run: |
          echo "Deploying ${{ github.event.inputs.image_tag }}"
          echo "to ${{ github.event.inputs.environment }}"
```

---

## Conditional Execution Tips

```yaml
# Only on specific branch
if: github.ref == 'refs/heads/main'

# Only on PR
if: github.event_name == 'pull_request'

# Skip if commit message contains [skip ci]
if: "!contains(github.event.head_commit.message, '[skip ci]')"

# Only when specific files change
on:
  push:
    paths:
      - 'src/**'
      - 'package.json'
```

---

## Caching Dependencies

```yaml
# Node.js
- uses: actions/setup-node@v4
  with:
    cache: 'npm'

# Maven
- uses: actions/setup-java@v4
  with:
    cache: 'maven'

# Python
- uses: actions/setup-python@v5
  with:
    cache: 'pip'
```

Caching speeds up builds by reusing downloaded dependencies.

---

[Previous: Environment Strategy](./05-environment-strategy.md) | [Index](./README.md) | [Next: Golden Rules](./07-golden-rules.md)
