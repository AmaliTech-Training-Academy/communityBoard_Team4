# Deployment Flow — Community Board

This document covers the complete lifecycle from a developer pushing code to a live application running on AWS, including infrastructure provisioning, container builds, ECS deploys, and versioned releases.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [One-Time Bootstrap](#2-one-time-bootstrap)
3. [Repository Secrets & Variables](#3-repository-secrets--variables)
4. [Branch Strategy & Environment Mapping](#4-branch-strategy--environment-mapping)
5. [Pipeline Chain](#5-pipeline-chain)
   - [Stage 1 — CI](#stage-1--ci-ciyml)
   - [Stage 2 — Docker Build & Push](#stage-2--docker-build--push-dockeryml)
   - [Stage 3 — ECS Deploy](#stage-3--ecs-deploy-deployyml)
   - [Stage 4 — Release](#stage-4--release-releaseyml)
6. [Infrastructure Pipeline (Terraform)](#6-infrastructure-pipeline-terraformyml)
7. [Infrastructure Modules](#7-infrastructure-modules)
8. [Secrets & Configuration at Runtime](#8-secrets--configuration-at-runtime)
9. [Manual Deploy Script](#9-manual-deploy-script)
10. [Rollback Procedures](#10-rollback-procedures)
11. [Cost Profile](#11-cost-profile)
12. [Troubleshooting Quick-Reference](#12-troubleshooting-quick-reference)

---

## 1. Architecture Overview

```
Internet
   │
   ▼
Application Load Balancer  (public subnets — eu-west-1a, eu-west-1b)
   │
   ├── /* ──────────────────────▶ community-board-frontend  (Nginx :80, ECS Fargate)
   │
   └── /api/* ──────────────────▶ community-board-backend   (Spring Boot :8080, ECS Fargate)
                                          │
                                          ▼
                                  RDS PostgreSQL 15.7
                                  (private DB subnets, encrypted)

                          Monitoring EC2 t3.micro  (Grafana + Prometheus, public subnet)
```

**Key design decisions:**

| Decision | Rationale |
|----------|-----------|
| ECS Fargate (not EC2) | No node management; scales to zero cost when idle |
| FARGATE_SPOT weight 4 : FARGATE weight 1 | ~70% savings on compute; falls back to on-demand automatically |
| No NAT Gateway | ECS tasks run in public subnets with `assign_public_ip = true`; saves ~$32/month |
| ECR `IMMUTABLE` tags | Only SHA-tagged images; prevents silent overwrites, guarantees deploy reproducibility |
| GitHub Actions OIDC | No long-lived AWS access keys stored as secrets |
| S3 + DynamoDB state backend | Encrypted, versioned, lock-protected Terraform state; shared across the team |

---

## 2. One-Time Bootstrap

These steps are performed **once** to create the prerequisite AWS resources that the CI/CD pipelines depend on.

### 2a. Create the Terraform State Backend

The bootstrap module creates an S3 bucket (KMS-encrypted, versioned) and a DynamoDB lock table. It uses a **local** state file because it has nothing to store its own state in yet.

```bash
cd devops/terraform/bootstrap

# Edit variables if needed (bucket name, region, project name)
# bootstrap/variables.tf defaults:
#   state_bucket_name = "community-board-tf-state"
#   lock_table_name   = "community-board-tf-locks"
#   aws_region        = "eu-west-1"

terraform init
terraform apply
```

Resources created:
- `aws_s3_bucket.tf_state` — `community-board-tf-state` (KMS-encrypted, versioned, public-access blocked)
- `aws_kms_key.tf_state` — dedicated key for state encryption with 7-day deletion window and automatic rotation
- `aws_dynamodb_table.tf_lock` — `community-board-tf-locks` (PAY_PER_REQUEST, `LockID` hash key)

> **Keep** `devops/terraform/bootstrap/terraform.tfstate` — it is the only record of the bootstrap resources. Do not delete it.

### 2b. Create `terraform.tfvars`

Copy the example and fill in values:

```bash
cd devops/terraform/aws
cp terraform.tfvars.example terraform.tfvars
# terraform.tfvars is gitignored — never commit it
```

Required values:

| Variable | Example | Notes |
|----------|---------|-------|
| `aws_region` | `eu-west-1` | |
| `environment` | `prod` | `prod` / `staging` / `dev` |
| `admin_cidrs` | `["203.0.113.5/32"]` | IPs allowed SSH to monitoring host |
| `db_username` | `communityboard` | RDS master username |
| `github_repo` | `my-org/communityBoard_Team4` | Used to scope the OIDC trust policy |
| `image_tag` | `abc1234` | Overridden by CI/CD on every deploy |
| `ecr_repository_name` | `community-board` | Must match `ECR_REPO` in workflows |

### 2c. First Terraform Apply

This provisions all infrastructure (VPC, ECS, RDS, ALB, ECR, IAM, etc.):

```bash
cd devops/terraform/aws

terraform init \
  -backend-config="bucket=community-board-tf-state" \
  -backend-config="key=community-board/aws/terraform.tfstate" \
  -backend-config="region=eu-west-1" \
  -backend-config="dynamodb_table=community-board-tf-locks" \
  -backend-config="encrypt=true"

terraform plan
terraform apply
```

After the first apply, capture the outputs:

```bash
terraform output github_actions_role_arn   # → set as AWS_ROLE_ARN secret
terraform output app_url                   # → application URL via ALB
terraform output ecr_repository_url        # → container registry URL
```

### 2d. Configure GitHub Secrets

In the repository: **Settings → Secrets and variables → Actions**

| Secret | Value | Where to get it |
|--------|-------|----------------|
| `AWS_ROLE_ARN` | `arn:aws:iam::<account>:role/community-board-github-actions` | `terraform output github_actions_role_arn` |
| `AWS_ACCOUNT_ID` | 12-digit account number | AWS console or `aws sts get-caller-identity` |
| `TF_VARS` | Full contents of `terraform.tfvars` | Local file — paste verbatim |

### 2e. Create GitHub Environments

In **Settings → Environments**:

| Environment | Required Reviewers | Purpose |
|-------------|-------------------|---------|
| `production` | Yes — add team leads | Gates `terraform apply`, ECS deploy to main, and releases |
| `staging` | No | Auto-deploys from develop branch |

---

## 3. Repository Secrets & Variables

### Secrets (encrypted)

| Secret | Used by | Description |
|--------|---------|-------------|
| `AWS_ROLE_ARN` | All AWS workflows | OIDC IAM role ARN — no static keys |
| `AWS_ACCOUNT_ID` | `docker.yml`, `deploy.yml` | Constructs the ECR URL (`<account>.dkr.ecr.<region>.amazonaws.com`) |
| `TF_VARS` | `terraform.yml` | Full `terraform.tfvars` contents, reconstructed at runtime |

### Runtime Secrets (in AWS, not GitHub)

These never touch GitHub. They are generated by Terraform and stored in AWS:

| AWS Service | Path / Name | Contents |
|-------------|-------------|----------|
| Secrets Manager | `/community-board/db-credentials` | `{"username":"...","password":"..."}` |
| Secrets Manager | `/community-board/jwt-secret` | JWT signing key (64 chars) |
| SSM Parameter Store | `/community-board/spring-datasource-url` | Full JDBC URL |
| Secrets Manager | `/community-board/grafana-admin-password` | Grafana dashboard password |

ECS tasks fetch these at container startup via the task execution role — the application code never needs to handle secrets directly.

---

## 4. Branch Strategy & Environment Mapping

```
develop ──────────────────────────────────────── staging environment
   │
   │  Pull Request
   ▼
main ─────────────────────────────────────────── production environment (gated)
   │
   └── semver tag (vX.Y.Z) created automatically after each production deploy
```

| Branch | CI runs | Docker build | ECS deploy target | Release created |
|--------|---------|-------------|-------------------|-----------------|
| `develop` | ✅ | ✅ (push only) | staging | ❌ |
| `main` | ✅ | ✅ (push only) | production | ✅ (patch auto) |
| `feature/*` / PR | ✅ (tests + tf plan) | ❌ | ❌ | ❌ |

---

## 5. Pipeline Chain

The four application workflows are chained sequentially via `workflow_run`. No workflow advances until the previous one succeeds.

```
[push to main/develop]
        │
        ▼
┌───────────────┐
│   CI          │  ci.yml
│  ─────────── │  backend-test (Maven + JaCoCo)
│  backend-test │  frontend-test (Vitest + build)
│  frontend-test│  terraform-validate (fmt/init/validate)
│  tf-validate  │
└───────┬───────┘
        │ workflow_run (success, push only)
        ▼
┌───────────────┐
│ Docker Build  │  docker.yml
│ & Push        │  frontend and backend in parallel
│               │  SHA-tagged images pushed to ECR
└───────┬───────┘
        │ workflow_run (success)
        ▼
┌───────────────┐
│ Deploy        │  deploy.yml
│               │  frontend → backend (sequential)
│               │  ECS rolling update + stability wait
│               │  ⚠️  production requires reviewer approval
└───────┬───────┘
        │ workflow_run (success, main only)
        ▼
┌───────────────┐
│ Release       │  release.yml
│               │  semver bump (auto patch or manual)
│               │  changelog from git log
│               │  annotated tag + GitHub Release
└───────────────┘
```

---

### Stage 1 — CI (`ci.yml`)

**Triggered by:** push or pull request to `main` / `develop`

**Concurrency:** `ci-<ref>` with `cancel-in-progress: true` — stale CI runs on force-push are cancelled.

#### `backend-test` job

1. Starts a sidecar `postgres:15` service container.
2. Checks out code, sets up JDK 17 (Temurin) with Maven cache.
3. Runs `mvn clean verify` — compiles, runs unit + integration tests, enforces JaCoCo coverage thresholds.
4. Uploads `jacoco.xml` to Codecov (informational; threshold is enforced by the Maven jacoco:check goal, not Codecov).

Environment variables injected for the test run:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/communityboard
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

#### `frontend-test` job

1. Checks out code, sets up Node.js 20 with npm cache.
2. `npm ci` — clean install from lockfile.
3. `npm run lint` — ESLint (non-blocking; surfaces issues in PR without blocking merge).
4. `npm run test:ci` — Vitest with coverage report written to `frontend/coverage/lcov.info`.
5. `npm run build` — production Vite build to confirm the bundle compiles cleanly.
6. Uploads coverage to Codecov.

#### `terraform-validate` job

1. `terraform fmt -check -recursive` — fails if any `.tf` file is not canonically formatted.
2. `terraform init -backend=false` — resolves modules without connecting to the state backend.
3. `terraform validate` — checks syntax and references.

> This job does **not** hold state locks and uses no AWS credentials. It only confirms the HCL is valid.

---

### Stage 2 — Docker Build & Push (`docker.yml`)

**Triggered by:** `workflow_run` on CI success (push to `main` or `develop` only — PRs do not trigger builds).

**Concurrency:** `docker-<branch>` with `cancel-in-progress: false` — in-progress image pushes are never cancelled.

#### Authentication

Uses GitHub Actions OIDC — no static AWS keys:

```
GitHub Actions runner
  → STS AssumeRoleWithWebIdentity
  → aws-actions/configure-aws-credentials@v4
  → community-board-github-actions IAM role
  → ECR GetAuthorizationToken
```

The OIDC trust is scoped to `repo:<org>/communityBoard_Team4:ref:refs/heads/*`.

#### Build matrix

Frontend and backend are built in **parallel** (`fail-fast: false` — both complete even if one fails):

| Matrix item | Context | Dockerfile |
|-------------|---------|-----------|
| `frontend` | `./frontend` | `./frontend/Dockerfile` |
| `backend` | `./backend` | `./backend/Dockerfile` |

#### Image tagging

Tags follow the pattern `<service>-<7-char-sha>`:

```
<account>.dkr.ecr.eu-west-1.amazonaws.com/community-board:frontend-abc1234
<account>.dkr.ecr.eu-west-1.amazonaws.com/community-board:backend-abc1234
```

ECR is configured `IMMUTABLE` — pushing the same tag twice is rejected. This guarantees that a tag always refers to exactly one image digest.

#### Layer cache

Docker Buildx layer cache is stored in GitHub Actions Cache, scoped per service (`scope=frontend`, `scope=backend`). This typically saves 60-80% of build time after the first run.

---

### Stage 3 — ECS Deploy (`deploy.yml`)

**Triggered by:** `workflow_run` on Docker Build & Push success.

**Concurrency:** `deploy-<branch>` with `cancel-in-progress: false` — running deploys are never interrupted.

#### `prepare` job

Runs first, without an environment gate, to resolve deployment context before requesting approval:

```
workflow_run trigger → infer SHA tag and environment from head_branch
  main    branch → image_tag=<sha7>, environment=production
  develop branch → image_tag=<sha7>, environment=staging

workflow_dispatch → accept explicit image_tag and environment inputs
```

Outputs: `image_tag`, `environment`, `ecr_url` — passed to the `deploy` job via `needs.prepare.outputs`.

#### `deploy` job

The `deploy` job runs the actual ECS deploy and is where the environment gate applies:

- **`staging`** — no required reviewers; auto-deploys.
- **`production`** — requires approval from a configured reviewer in the GitHub Environment before the runner starts.

Deploys **frontend before backend** (`max-parallel: 1`) to ensure users always hit a consistent API version during the rollout window.

For each service:

1. **Download current task definition** — `aws ecs describe-task-definition --query taskDefinition` writes the current revision to a local JSON file.
2. **Render updated task definition** — `aws-actions/amazon-ecs-render-task-definition@v1` replaces just the container image URI in the JSON, leaving all other settings (CPU, memory, port mappings, secrets ARNs, log config) unchanged.
3. **Register + deploy** — `aws-actions/amazon-ecs-deploy-task-definition@v2` registers the modified JSON as a new task definition revision and calls `UpdateService` on the ECS service. The action then polls until the service reaches a **stable** state (all desired tasks healthy) or the 15-minute wait timeout expires.

ECS performs a **rolling update**: new tasks are started before old ones are drained. The ALB only routes traffic to tasks that pass health checks, so zero-downtime deploys are guaranteed as long as at least one task is healthy.

Health check endpoints:

| Service | Health check | Expected |
|---------|-------------|---------|
| Frontend | `wget -q -O /dev/null http://localhost/` | HTTP 200 |
| Backend | `wget -q -O /dev/null http://localhost:8080/actuator/health` | HTTP 200 |

---

### Stage 4 — Release (`release.yml`)

**Triggered by:** `workflow_run` on Deploy success (main only) **or** `workflow_dispatch` with explicit bump type.

**No AWS credentials required** — this is a pure GitHub operation.

#### Process

1. **Find latest semver tag** — `git tag --sort=-version:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1`. Defaults to `v0.0.0` if no tags exist yet.
2. **Compute next version** — splits major/minor/patch, increments according to bump type (`patch` for automatic releases; `patch`/`minor`/`major` for manual dispatch).
3. **Generate changelog** — `git log <prev>..HEAD --pretty=format:"- %s (%h)" --no-merges`. Captures all non-merge commit subjects since the last release.
4. **Create annotated tag** — `git tag -a vX.Y.Z -m "Release vX.Y.Z"` pushed with the `GITHUB_TOKEN`.
5. **Publish GitHub Release** — `softprops/action-gh-release@v2` creates a release on the new tag with the changelog as the body.

#### Manual release

To trigger a minor or major release instead of the automatic patch bump:

```
GitHub → Actions → Release → Run workflow
  bump: minor   (or major)
```

---

## 6. Infrastructure Pipeline (`terraform.yml`)

The Terraform workflow manages the AWS infrastructure lifecycle independently of the application pipeline. Changes to `devops/terraform/aws/**` trigger it.

**Concurrency:** `terraform-state` with `cancel-in-progress: false` — only one Terraform operation runs against the state at a time, preventing lock contention.

### State Backend

| Component | Value |
|-----------|-------|
| S3 bucket | `community-board-tf-state` |
| State key | `community-board/aws/terraform.tfstate` |
| Region | `eu-west-1` |
| DynamoDB lock table | `community-board-tf-locks` |
| Encryption | KMS — `alias/community-board-tf-state` |

Backend configuration is passed as `-backend-config` CLI flags at runtime — the `backend.hcl` file is gitignored. The `TF_VARS` GitHub secret is written to `terraform.tfvars` at the start of each run to reconstruct the gitignored variables file.

### Plan job (PRs)

Triggered on every pull request targeting `main` or `develop` that touches `devops/terraform/aws/**`.

Steps:
1. Configure AWS via OIDC.
2. Write `terraform.tfvars` from the `TF_VARS` secret.
3. `terraform init` with full `-backend-config` flags.
4. `terraform fmt -check -recursive` (non-blocking — surfaced in comment).
5. `terraform validate`.
6. `terraform plan -out=tfplan.binary` — output tee'd to `plan.txt`.
7. Upload `tfplan.binary` + `.terraform.lock.hcl` as artifact (retained 3 days).
8. Post the plan output as a PR comment (upserts — replaces the old comment on re-runs using an HTML marker `<!-- tf-plan-comment -->`). Plans longer than 60,000 characters are truncated with a note pointing to the Actions log.
9. Exit non-zero if the plan returned an error (exit code 1). A plan with changes (exit code 2) is allowed to pass so the PR can be reviewed and merged.

### Apply job (push to main)

Triggered on every push to `main` that touches `devops/terraform/aws/**`. Also triggerable manually via `workflow_dispatch(apply)`.

- Gated by the **`production`** GitHub Environment — requires reviewer approval before the runner starts.
- Runs `terraform apply -auto-approve` (approval was given in GitHub Environments, so no interactive prompt is needed).
- Prints `app_url` and `github_actions_role_arn` in the GitHub Actions step summary.

> **Important:** The apply job re-plans from scratch rather than consuming the plan artifact from the PR. This ensures the apply reflects the exact state at the time of merge, not the time of the PR plan (which may be stale if other changes were merged first).

### Destroy job (manual only)

Triggered exclusively by `workflow_dispatch(destroy)`. Also gated by the `production` environment.

This is a last-resort operation for tearing down all infrastructure. It requires explicit human selection of the `destroy` action — it cannot be triggered accidentally by a push or PR.

---

## 7. Infrastructure Modules

All modules live under `devops/terraform/aws/modules/`.

| Module | Resources | Notes |
|--------|-----------|-------|
| `network` | VPC, 2× public subnets, 2× private DB subnets, IGW, route tables | No NAT gateway — ECS uses public subnets |
| `security` | ALB SG, Frontend SG, Backend SG, RDS SG, Monitoring SG | Least-privilege ingress/egress per tier |
| `iam` | ECS task execution role, ECS task role, monitoring instance profile, GitHub Actions OIDC role | GitHub role scoped to `var.github_repo` |
| `key_pair` | SSH key pair for the monitoring EC2 | Private key written to `../../keys/` (gitignored) |
| `compute` | ECS cluster, Frontend + Backend Fargate services + task defs, Monitoring EC2 | FARGATE_SPOT (weight 4) + FARGATE (weight 1) |
| `alb` | Internet-facing ALB, Frontend + Backend target groups, HTTP listener with path-based rules | `/api/*` → Backend, `/*` → Frontend |
| `rds` | PostgreSQL 15.7 DB instance, DB subnet group | Private subnets; deletion protection enabled in `prod` |
| `ecr` | Single ECR repository | `IMMUTABLE` tags; lifecycle policy removes untagged images after 1 day |
| `secrets` | Secrets Manager secrets (DB creds, JWT key, Grafana password), SSM Parameter Store (JDBC URL) | Random passwords generated by `random_password` resources |
| `security_services` | CloudTrail trail (S3 + KMS), GuardDuty detector (`enable = false` for cost) | GuardDuty can be enabled in `terraform.tfvars` |

---

## 8. Secrets & Configuration at Runtime

The backend Spring Boot container receives all sensitive config via ECS secrets injection — no values are baked into the image or environment variables in plaintext.

Task definition secrets mapping:

| Environment variable | Source | AWS Service |
|---------------------|--------|------------|
| `SPRING_DATASOURCE_URL` | `/community-board/spring-datasource-url` | SSM Parameter Store |
| `SPRING_DATASOURCE_USERNAME` | `username` key in `/community-board/db-credentials` | Secrets Manager (JSON field) |
| `SPRING_DATASOURCE_PASSWORD` | `password` key in `/community-board/db-credentials` | Secrets Manager (JSON field) |
| `JWT_SECRET` | `/community-board/jwt-secret` | Secrets Manager |

The ECS task execution role (`community-board-execution-role`) has `secretsmanager:GetSecretValue` and `ssm:GetParameters` permissions to retrieve these at container startup. The running container only sees the plaintext values injected by the ECS agent — no AWS SDK calls needed from application code.

---

## 9. Manual Deploy Script

`devops/scripts/deploy.sh` is an emergency/local deploy script that replicates the CI/CD push-and-deploy flow from a developer workstation.

```bash
# Prerequisites: AWS CLI configured, Docker daemon running, jq installed
cd /path/to/communityBoard_Team4

./devops/scripts/deploy.sh <image-tag> [environment]
# Example:
./devops/scripts/deploy.sh abc1234 prod
```

What it does:
1. Validates that the `community-board` ECR repository exists (exits with a clear error if not).
2. Authenticates Docker to ECR via `aws ecr get-login-password`.
3. Builds and pushes `frontend-<tag>` and `backend-<tag>` images.
4. Calls `aws ecs update-service --force-new-deployment` on both services.
5. Waits for both services to reach a stable state.
6. Prints the ALB DNS name.

> The script triggers a force-new-deployment using the **current** task definition. For a proper image-updating deploy from a workstation, update `terraform.tfvars` with the new `image_tag` and run `terraform apply` before calling the script, or manually update the task definition image URI first.

---

## 10. Rollback Procedures

### Application rollback — previous ECS task definition

ECS keeps all previous task definition revisions. To roll back to a known-good image:

```bash
# 1. Find the previous stable task definition revision
aws ecs list-task-definitions \
  --family-prefix community-board-frontend \
  --sort DESC \
  --region eu-west-1

# 2. Update the service to the previous revision
aws ecs update-service \
  --cluster community-board-cluster \
  --service community-board-frontend \
  --task-definition community-board-frontend:<previous-revision-number> \
  --region eu-west-1

# 3. Repeat for backend
aws ecs update-service \
  --cluster community-board-cluster \
  --service community-board-backend \
  --task-definition community-board-backend:<previous-revision-number> \
  --region eu-west-1

# 4. Wait for stability
aws ecs wait services-stable \
  --cluster community-board-cluster \
  --services community-board-frontend community-board-backend \
  --region eu-west-1
```

### Infrastructure rollback — Terraform state

Because state is versioned in S3, you can restore a previous state version:

```bash
# List versions of the state file
aws s3api list-object-versions \
  --bucket community-board-tf-state \
  --prefix community-board/aws/terraform.tfstate

# Download a specific version
aws s3api get-object \
  --bucket community-board-tf-state \
  --key community-board/aws/terraform.tfstate \
  --version-id <version-id> \
  terraform.tfstate.backup

# Review it, then move it back (with care)
aws s3 cp terraform.tfstate.backup \
  s3://community-board-tf-state/community-board/aws/terraform.tfstate
```

> Always take a snapshot of the current state before manually restoring an old one.

### GitHub Release rollback — revert tag

```bash
# Locally delete and re-push the tag to a previous SHA if needed
git tag -d vX.Y.Z
git push origin :refs/tags/vX.Y.Z
# Then re-deploy the known-good image using workflow_dispatch on deploy.yml
```

---

## 11. Cost Profile

Infrastructure running costs at typical development load (eu-west-1, ~March 2026 pricing):

| Resource | Spec | Monthly estimate |
|----------|------|-----------------|
| ALB | 1 load balancer + minimal LCUs | ~$18 |
| ECS Fargate SPOT | 2 frontend (256 CPU / 512 MB) + 2 backend (512 CPU / 1024 MB) | ~$8 |
| RDS PostgreSQL | `db.t3.micro`, 10 GB gp2, single-AZ | ~$13 |
| ECR | ~2 GB images stored | ~$0.20 |
| Monitoring EC2 | `t3.micro` (Grafana + Prometheus) | ~$8 |
| S3 (state + CloudTrail) | < 1 GB | ~$0.02 |
| Data transfer | Minimal | ~$1 |
| **Total** | | **~$48/month** |

Cost levers:
- Enable `db_multi_az = true` in production for ~$13 more (HA failover).
- Enable GuardDuty in production for ~$3-8 more (recommended).
- Add a NAT Gateway if private subnets are needed: +$32/month.

---

## 12. Troubleshooting Quick-Reference

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `terraform init` fails with "NoSuchBucket" | Bootstrap not run | Run bootstrap: `cd devops/terraform/bootstrap && terraform apply` |
| `terraform apply` fails with lock error | Another apply in progress or stale lock | Check `community-board-tf-locks` in DynamoDB and delete the `LockID` item if stale |
| Docker push fails with "tag already exists" | ECR `IMMUTABLE` rejects re-push of same SHA | Force-push a new commit (generates a new SHA tag) |
| ECS service stuck in `ACTIVATING` | Health check failing | Check CloudWatch Logs → `/ecs/community-board-frontend` or `/ecs/community-board-backend`; verify the `wget` health check URL inside the container |
| Backend returns 500 on `/api/*` | Secrets not loaded | Check ECS task stopped reason in the console; verify the execution role has `secretsmanager:GetSecretValue` and the secret ARNs in the task definition match the Terraform outputs |
| `aws-actions/configure-aws-credentials` fails with "Not authorized to perform sts:AssumeRoleWithWebIdentity" | OIDC trust mismatch | Verify `github_repo` in `terraform.tfvars` matches exactly `<org>/<repo>` (case-sensitive); re-apply Terraform |
| PR plan comment not posted | Missing `pull-requests: write` permission | Check `permissions` block in `terraform.yml`; personal-fork PRs from external contributors cannot write comments |
| Release workflow skipped | Deploy did not run on `main` | Release only fires on `workflow_run` from Deploy on the `main` branch; check the Deploy run conclusion |
