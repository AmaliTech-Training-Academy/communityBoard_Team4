##############################################################################
# variables.tf — all input variables for the Community Board infrastructure
##############################################################################

# ---------------------------------------------------------------------------
# Provider / global
# ---------------------------------------------------------------------------
variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "eu-west-1"
}

variable "project_name" {
  description = "Short project name used as a prefix for all resource names"
  type        = string
  default     = "community-board"
}

variable "environment" {
  description = "Deployment environment: prod | staging | dev"
  type        = string
  default     = "prod"

  validation {
    condition     = contains(["prod", "staging", "dev"], var.environment)
    error_message = "environment must be one of: prod, staging, dev."
  }
}

# ---------------------------------------------------------------------------
# Networking
# ---------------------------------------------------------------------------
variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.30.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDRs for public subnets (ALB + NAT GW) — one per AZ"
  type        = list(string)
  default     = ["10.30.0.0/24", "10.30.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDRs for private app subnets (Frontend + Backend) — one per AZ"
  type        = list(string)
  default     = ["10.30.10.0/24", "10.30.11.0/24"]
}

variable "db_subnet_cidrs" {
  description = "CIDRs for private DB subnets (RDS) — one per AZ"
  type        = list(string)
  default     = ["10.30.20.0/24", "10.30.21.0/24"]
}

variable "admin_cidrs" {
  description = "CIDR list allowed SSH / ops access (keep minimal)"
  type        = list(string)
  # No default — must be explicitly provided per environment
}

# ---------------------------------------------------------------------------
# Monitoring EC2
# ---------------------------------------------------------------------------
variable "monitoring_instance_type" {
  description = "Instance type for Monitoring host (Prometheus + Grafana + Alertmanager)"
  type        = string
  default     = "t3.medium"
}

# ---------------------------------------------------------------------------
# ECS Fargate — task sizing and desired counts
# ---------------------------------------------------------------------------
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

variable "image_tag" {
  description = "Container image tag to deploy (CI/CD overrides this per build)"
  type        = string
  default     = "latest"
}

# ---------------------------------------------------------------------------
# RDS PostgreSQL
# ---------------------------------------------------------------------------
variable "db_username" {
  description = "Master database username"
  type        = string
  sensitive   = true
  default     = "cbadmin"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Initial allocated storage for RDS in GB"
  type        = number
  default     = 20
}

variable "db_multi_az" {
  description = "Enable RDS Multi-AZ for high availability (recommended for prod)"
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "Number of days to retain RDS automated backups"
  type        = number
  default     = 7
}

# ---------------------------------------------------------------------------
# ECR
# ---------------------------------------------------------------------------
variable "ecr_repository_name" {
  description = "ECR repository name for application container images"
  type        = string
  default     = "community-board"
}
