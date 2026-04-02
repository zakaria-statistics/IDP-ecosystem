[← Previous](./14-golden-rules.md) | [📋 Index](./README.md) | [Next →](./16-practical-commands.md)

---

# Infrastructure as Code & Config Management

## IaC Philosophy

**Treat infrastructure like application code:**
- Version controlled
- Code reviewed
- Tested
- Reproducible

```
Manual CLI clicks  →  Scripted  →  Declarative IaC
   (fragile)         (better)      (best)
```

---

## Tool Landscape

```
┌─────────────────────────────────────────────────────────────────┐
│                    IaC & CONFIG TOOLS                           │
├─────────────────┬─────────────────┬─────────────────────────────┤
│  PROVISIONING   │   CONFIGURATION │      ORCHESTRATION          │
│                 │                 │                             │
│  Terraform      │   Ansible       │   Kubernetes                │
│  Pulumi         │   Chef          │   Helm                      │
│  CloudFormation │   Puppet        │   Kustomize                 │
│  OpenTofu       │   SaltStack     │   ArgoCD                    │
└─────────────────┴─────────────────┴─────────────────────────────┘
```

---

## Terraform — Infrastructure Provisioning

**What:** Create cloud resources (VMs, networks, databases, K8s clusters)

```hcl
# main.tf
resource "aws_instance" "app_server" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t3.medium"

  tags = {
    Name        = "app-server-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_db_instance" "postgres" {
  engine         = "postgres"
  engine_version = "15"
  instance_class = "db.t3.micro"
}
```

---

## Terraform Workflow

```bash
# Initialize (download providers)
terraform init

# Preview changes
terraform plan

# Apply changes
terraform apply

# Destroy resources
terraform destroy
```

**In CI/CD:**
```yaml
deploy_infra:
  script:
    - terraform init
    - terraform plan -out=tfplan
    - terraform apply tfplan
```

---

## Ansible — Configuration Management

**What:** Configure servers, install software, manage state

```yaml
# playbook.yml
- hosts: app_servers
  become: yes
  tasks:
    - name: Install Docker
      apt:
        name: docker.io
        state: present

    - name: Start Docker service
      service:
        name: docker
        state: started
        enabled: yes

    - name: Deploy application
      docker_container:
        name: myapp
        image: "myapp:{{ app_version }}"
        state: started
        ports:
          - "8080:8080"
```

---

## Ansible Workflow

```bash
# Run playbook
ansible-playbook -i inventory.yml playbook.yml

# With variables
ansible-playbook playbook.yml -e "app_version=1.4.2"

# Dry run (check mode)
ansible-playbook playbook.yml --check
```

**In CI/CD:**
```yaml
configure_servers:
  script:
    - ansible-playbook -i inventory/$ENV.yml deploy.yml \
        -e "app_version=$CI_COMMIT_SHORT_SHA"
```

---

## Helm — Kubernetes Package Manager

**What:** Template and deploy K8s applications

```yaml
# values.yaml
image:
  repository: myapp
  tag: "1.4.2"

replicas: 3

resources:
  limits:
    cpu: 500m
    memory: 512Mi

ingress:
  enabled: true
  host: myapp.example.com
```

---

## When to Use What

| Tool | Use For |
|------|---------|
| **Terraform** | Cloud resources (VMs, VPCs, RDS, EKS) |
| **Ansible** | Server config, app deployment (non-K8s) |
| **Helm** | Kubernetes app packaging & deployment |
| **Kustomize** | K8s manifest patching per environment |
| **ArgoCD** | GitOps continuous deployment to K8s |

---

## IaC in CI/CD Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                     FULL PIPELINE                           │
├─────────────┬─────────────┬─────────────┬──────────────────┤
│  Terraform  │   Docker    │    Helm     │    ArgoCD        │
│  (infra)    │   (build)   │  (deploy)   │    (sync)        │
├─────────────┼─────────────┼─────────────┼──────────────────┤
│ Create EKS  │ Build image │ Install app │ Reconcile state  │
│ Create RDS  │ Push to ECR │ to cluster  │ from Git         │
└─────────────┴─────────────┴─────────────┴──────────────────┘
```


---

[← Previous](./14-golden-rules.md) | [📋 Index](./README.md) | [Next →](./16-practical-commands.md)
