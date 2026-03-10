variable "project_name" {
  description = "Project name prefix for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment (prod/staging/dev) — used in log group names and task env vars"
  type        = string
}

# ── Networking ────────────────────────────────────────────────────────────────
variable "app_subnet_ids" {
  description = "Subnet IDs for ECS Fargate task ENIs (public subnets + assign_public_ip=true avoids NAT charges)"
  type        = list(string)
}

variable "public_subnet_id" {
  description = "Public subnet ID for the Monitoring EC2 host"
  type        = string
}

# ── Security Groups ────────────────────────────────────────────────────────────
variable "frontend_security_group_id" {
  description = "Security group attached to Frontend Fargate task ENIs"
  type        = string
}

variable "backend_security_group_id" {
  description = "Security group attached to Backend Fargate task ENIs"
  type        = string
}

variable "monitoring_security_group_id" {
  description = "Security group for the Monitoring EC2 host"
  type        = string
}

# ── IAM ────────────────────────────────────────────────────────────────────────
variable "ecs_task_execution_role_arn" {
  description = "ARN of the ECS task execution role (used by ECS agent to pull images, write logs, inject secrets)"
  type        = string
}

variable "ecs_task_role_arn" {
  description = "ARN of the ECS task role (assumed by the running container code)"
  type        = string
}

variable "monitoring_instance_profile_name" {
  description = "IAM instance profile name for the Monitoring EC2 host"
  type        = string
}

# ── Container Images ──────────────────────────────────────────────────────────
variable "frontend_image" {
  description = "Full ECR image URI for the Frontend container"
  type        = string
}

variable "backend_image" {
  description = "Full ECR image URI for the Backend container"
  type        = string
}

# ── ECS Task Sizing ────────────────────────────────────────────────────────────
variable "frontend_cpu" {
  description = "Fargate CPU units for Frontend task (256 = 0.25 vCPU)"
  type        = number
  default     = 256
}

variable "frontend_memory" {
  description = "Fargate memory (MB) for Frontend task"
  type        = number
  default     = 512
}

variable "backend_cpu" {
  description = "Fargate CPU units for Backend task (512 = 0.5 vCPU)"
  type        = number
  default     = 512
}

variable "backend_memory" {
  description = "Fargate memory (MB) for Backend task"
  type        = number
  default     = 1024
}

variable "frontend_desired_count" {
  description = "Desired number of Frontend Fargate tasks"
  type        = number
  default     = 1
}

variable "backend_desired_count" {
  description = "Desired number of Backend Fargate tasks"
  type        = number
  default     = 1
}

# ── ALB Target Groups ──────────────────────────────────────────────────────────
variable "frontend_tg_arn" {
  description = "ARN of the ALB target group for the Frontend ECS service"
  type        = string
}

variable "backend_tg_arn" {
  description = "ARN of the ALB target group for the Backend ECS service"
  type        = string
}

# ── Secrets ────────────────────────────────────────────────────────────────────
variable "spring_datasource_url_ssm_arn" {
  description = "ARN of the SSM parameter holding the pre-assembled JDBC URL for SPRING_DATASOURCE_URL"
  type        = string
}

variable "db_credentials_secret_arn" {
  description = "Secrets Manager ARN for RDS credentials JSON (injected into Backend container)"
  type        = string
}

variable "jwt_secret_arn" {
  description = "Secrets Manager ARN for the JWT signing key (injected into Backend container)"
  type        = string
}

# ── Monitoring EC2 ─────────────────────────────────────────────────────────────
variable "ami_id" {
  description = "AMI ID for the Monitoring EC2 instance"
  type        = string
}

variable "key_name" {
  description = "EC2 key pair name for SSH access to the Monitoring host"
  type        = string
}

variable "monitoring_instance_type" {
  description = "EC2 instance type for Monitoring host (Prometheus + Grafana + Alertmanager)"
  type        = string
  default     = "t3.medium"
}

variable "tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
}
