# Community Board — Terraform Infrastructure

AWS infrastructure for the Community Board capstone project (Team 4).
All resources are managed with Terraform and deployed to **eu-west-1**.

---

## Architecture Overview

```
Internet
    │
    ▼
┌─────────────────────────────────────────────┐
│  Application Load Balancer  (public subnet) │
│  /* → Frontend   /api/* → Backend           │
└──────────────┬──────────────┬───────────────┘
               │              │
    ┌──────────▼──┐    ┌──────▼──────────┐
    │  ECS Fargate│    │  ECS Fargate    │
    │  Frontend   │    │  Backend        │
    │  Nginx :80  │    │  Spring Boot    │
    │             │    │  :8080          │
    │  public     │    │  public subnet  │
    │  subnet     │    │  (Spot)         │
    │  (Spot)     │    └──────┬──────────┘
    └─────────────┘           │
                              ▼
                    ┌─────────────────┐
                    │  RDS PostgreSQL │
                    │  private DB     │
                    │  subnet         │
                    └─────────────────┘

    ┌─────────────────────────┐
    │  Monitoring EC2         │
    │  t3.micro  public subnet│
    │  Grafana → CloudWatch   │
    └─────────────────────────┘
```

**Key design decisions:**

| Decision | Rationale |
|---|---|
| ECS Fargate in public subnets (`assign_public_ip = true`) | Eliminates NAT Gateway (~$32/month saved); security groups still block all inbound traffic from the internet |
| `FARGATE_SPOT` (4:1 weight over on-demand) | ~70% cost reduction; frontend and backend are stateless and safe to restart |
| Grafana → CloudWatch (no Prometheus) | t3.micro is enough; ECS and RDS already emit CloudWatch metrics natively |
| Secrets Manager for credentials | DB password and JWT key are never written to disk or tfvars |
| Single-AZ RDS | Sufficient for a capstone; set `db_multi_az = true` to enable HA |

---

## Directory Structure

```
terraform/
├── bootstrap/          # One-time setup: S3 state bucket + DynamoDB lock table
│   ├── main.tf
│   └── variables.tf
└── aws/                # Main infrastructure root module
    ├── main.tf         # Root module — wires all submodules together
    ├── variables.tf    # Input variable declarations
    ├── locals.tf       # Derived values, name prefix, common tags
    ├── outputs.tf      # ALB URL, ECS cluster name, monitoring IP, etc.
    ├── terraform.tfvars          # ⚠ gitignored — copy from .example
    ├── backend.hcl               # ⚠ gitignored — copy from .example
    ├── backend.hcl.example       # Template — fill in and rename
    └── modules/
        ├── network/              # VPC, subnets, IGW, route tables
        ├── security/             # Security groups (ALB, frontend, backend, RDS, monitoring)
        ├── iam/                  # ECS task execution role, task role, monitoring EC2 role
        ├── compute/              # ECS cluster + Fargate services + Monitoring EC2
        ├── alb/                  # Internet-facing ALB, target groups, HTTP listener
        ├── rds/                  # PostgreSQL 15, encrypted, automated backups
        ├── ecr/                  # Container image registry
        ├── secrets/              # Secrets Manager (DB creds, JWT) + SSM Parameter Store
        ├── key_pair/             # SSH key pair for the Monitoring EC2 only
        └── security_services/    # CloudTrail audit log (GuardDuty disabled for cost)
```

---

## Prerequisites

| Tool | Minimum version |
|---|---|
| Terraform | 1.6.0 |
| AWS CLI | 2.x |
| Docker | 24.x (for `deploy.sh`) |

The AWS principal running Terraform needs permissions to manage: VPC, EC2, ECS, ECR, RDS, ALB, IAM, S3, Secrets Manager, SSM, CloudTrail, CloudWatch.

---

## First-Time Setup (Bootstrap)

The S3 bucket and DynamoDB lock table that store Terraform state must be created before the main stack can run. Do this **once per AWS account**.

```bash
cd devops/terraform/bootstrap
terraform init
terraform apply
```

This creates:
- S3 bucket `community-board-tf-state` (versioned, KMS-encrypted, public access blocked)
- DynamoDB table `community-board-tf-locks` (pay-per-request)

---

## Deploying the Main Stack

### 1. Configure the backend

```bash
cd devops/terraform/aws
cp backend.hcl.example backend.hcl
```

Edit `backend.hcl` and fill in your AWS account details (bucket name is already set to `community-board-tf-state`).

### 2. Configure variables

```bash
cp terraform.tfvars.example terraform.tfvars   # if an example exists, otherwise create from scratch
```

Minimum required values (already present in the repo-provided file):

```hcl
aws_region   = "eu-west-1"
project_name = "community-board"
environment  = "prod"
admin_cidrs  = ["<your-ip>/32"]   # restrict SSH + Grafana access
```

> **Never set `db_password` in tfvars.** The password is generated automatically by Terraform (`random_password`) and stored directly in AWS Secrets Manager.

### 3. Init, plan, apply

```bash
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
```

After apply, Terraform prints:

```
app_url               = "http://<alb-dns-name>"
alb_dns_name          = "<alb-dns-name>"
ecs_cluster_name      = "community-board-cluster"
frontend_service_name = "community-board-frontend"
backend_service_name  = "community-board-backend"
monitoring_public_ip  = "<ip>"
monitoring_public_dns = "<dns>"
```

---

## Deploying Application Updates

Use `deploy.sh` for rolling ECS deployments after infrastructure is provisioned.

```bash
# From the repo root
./devops/scripts/deploy.sh <git-sha> [environment]

# Example
./devops/scripts/deploy.sh abc1234 prod
```

The script:
1. Authenticates Docker to ECR
2. Builds and pushes `frontend:<tag>` and `backend:<tag>` images
3. Calls `aws ecs update-service --force-new-deployment` on both services
4. Waits for services to stabilise and prints the ALB URL

In CI/CD (GitHub Actions) the Git commit SHA is passed as the image tag automatically.

---

## Module Reference

### `network`
Creates the VPC (`10.30.0.0/16`), two public subnets (ALB + ECS tasks), two private-app subnets (reserved), two private-DB subnets (RDS), Internet Gateway, and route tables. No NAT Gateway — ECS tasks use public subnets with `assign_public_ip = true`.

### `security`
Five security groups with least-privilege rules:
- **ALB** — inbound 80 from anywhere; outbound to frontend (80) and backend (8080)
- **Frontend** — inbound 80 from ALB only; no SSH
- **Backend** — inbound 8080 from ALB and frontend; no SSH
- **RDS** — inbound 5432 from backend only
- **Monitoring** — inbound SSH (22), Grafana (3000), Prometheus (9090), Alertmanager (9093) from `admin_cidrs` only

### `iam`
- **ECS Task Execution Role** — used by the ECS agent to pull images from ECR, write logs to CloudWatch, and inject secrets from Secrets Manager at container start
- **ECS Task Role** — assumed by application code at runtime; scoped CloudWatch Logs write permission
- **Monitoring EC2 Role + Instance Profile** — SSM Session Manager + CloudWatch agent access

### `compute`
- **ECS Cluster** — Container Insights enabled; capacity providers FARGATE_SPOT (weight 4) + FARGATE (weight 1)
- **Frontend service** — Nginx on port 80; CloudWatch log group with 30-day retention
- **Backend service** — Spring Boot on port 8080; DB credentials and JWT key injected as environment variables from Secrets Manager at task start (never in plaintext)
- **Monitoring EC2** — t3.micro in a public subnet; Grafana pointed at CloudWatch; 20 GB gp3 root volume

### `alb`
Internet-facing ALB with:
- Default rule: `/*` → Frontend target group (port 80, `target_type = ip`)
- Priority rule: `/api/*` → Backend target group (port 8080, `target_type = ip`, health check at `/actuator/health`)
- `drop_invalid_header_fields = true` to prevent HTTP request smuggling

### `rds`
PostgreSQL 15.7 on `db.t3.micro`, 10 GB gp2, single-AZ, automated 7-day backups, storage encrypted at rest, `deletion_protection = true` in prod. Password is managed by Terraform and stored in Secrets Manager — never in tfvars.

### `ecr`
Single repository (`community-board`) for both frontend and backend images. CI/CD tags images as `frontend-<sha>` and `backend-<sha>`.

### `secrets`
| Store | Path | Content |
|---|---|---|
| Secrets Manager | `/<project>/<env>/db/credentials` | RDS JSON blob (engine, host, port, dbname, username, password) |
| Secrets Manager | `/<project>/<env>/app/jwt-secret` | JWT signing key |
| SSM Parameter Store | `/<project>/<env>/config/alb-dns-name` | ALB DNS name |
| SSM Parameter Store | `/<project>/<env>/config/ecr-repository-url` | ECR URL |
| SSM Parameter Store | `/<project>/<env>/monitoring/grafana-admin-password` | Grafana admin password |

### `security_services`
- **CloudTrail** — multi-region trail; logs stored in S3, transitioned to Glacier after 90 days, expired after 365 days; log file validation enabled
- **GuardDuty** — provisioned but disabled (`enable = false`) to stay within the $50/month capstone budget; re-enable before any production workload by setting `enable = true`

### `bootstrap`
One-time resources: KMS-encrypted S3 state bucket with versioning and public-access block, DynamoDB lock table.

---

## Cost Estimate (eu-west-1, prod, single-AZ)

| Resource | Spec | Est. monthly |
|---|---|---|
| ALB | 1× | ~$16 |
| ECS Fargate (frontend) | 0.25 vCPU / 0.5 GB — Spot ~70% discount | ~$2 |
| ECS Fargate (backend) | 0.5 vCPU / 1 GB — Spot ~70% discount | ~$3 |
| RDS PostgreSQL | db.t3.micro, 10 GB, single-AZ | ~$12 |
| Monitoring EC2 | t3.micro, 20 GB gp3 | ~$7 |
| CloudTrail | ~$2/100k events | ~$1 |
| ECR | first 500 MB/month free | ~$0 |
| **Total** | | **~$41/month** |

Activate GuardDuty adds ~$3/month. Enabling multi-AZ RDS roughly doubles the RDS line item.

---

## Teardown

```bash
cd devops/terraform/aws
terraform destroy
```

> RDS deletion protection is enabled in `prod` — you must set `deletion_protection = false` in the RDS module or via a targeted `terraform apply` before `destroy` will succeed.

To remove the bootstrap resources (after the main stack is gone):

```bash
cd devops/terraform/bootstrap
terraform destroy
```
