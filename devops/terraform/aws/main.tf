terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  backend "s3" {}
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

##############################################################################
# Generated secrets — created once, stored in Secrets Manager / SSM
##############################################################################

# RDS master password: 32-char, mixed symbols safe for PostgreSQL
resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#%&*()-_=+[]{}<>:?"
}

# JWT signing key: 64-char alphanumeric (base64-safe, no special chars)
resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

# Grafana admin password: 24-char for the monitoring dashboard
resource "random_password" "grafana_admin" {
  length           = 24
  special          = true
  override_special = "!#%&*()-_=+"
}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2", "amzn2-ami-hvm-*-x86_64-gp3"]
  }
}

##############################################################################
# Network — VPC, public + private app + private DB subnets, NAT GW
##############################################################################
module "network" {
  source = "./modules/network"

  project_name         = local.project
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = local.public_subnet_cidrs
  private_subnet_cidrs = local.private_subnet_cidrs
  db_subnet_cidrs      = local.db_subnet_cidrs
  availability_zones   = slice(data.aws_availability_zones.available.names, 0, 2)
  tags                 = local.common_tags
}

##############################################################################
# Security Groups — ALB → Frontend → Backend → RDS (least-privilege)
##############################################################################
module "security" {
  source = "./modules/security"

  project_name = local.project
  vpc_id       = module.network.vpc_id
  admin_cidrs  = var.admin_cidrs
  tags         = local.common_tags
}

##############################################################################
# IAM Roles / Instance Profiles
##############################################################################
module "iam" {
  source                  = "./modules/iam"
  project_name            = local.project
  environment             = local.environment
  github_repo             = var.github_repo
  ses_sender_identity_arn = module.security_services.ses_sender_identity_arn
  tags                    = local.common_tags
}

##############################################################################
# SSH Key Pair
##############################################################################
module "key_pair" {
  source          = "./modules/key_pair"
  key_pair_name   = "${local.project}-key"
  keys_output_dir = "${abspath(path.root)}/../../keys"
  tags            = local.common_tags
}

##############################################################################
# Compute — ECS Fargate (Frontend + Backend) + Monitoring EC2
##############################################################################
module "compute" {
  source = "./modules/compute"

  project_name     = local.project
  environment      = local.environment
  aws_region       = var.aws_region
  ami_id           = data.aws_ami.amazon_linux_2.id
  public_subnet_id = module.network.public_subnet_ids[0]
  app_subnet_ids   = module.network.public_subnet_ids
  key_name         = module.key_pair.key_name

  frontend_security_group_id   = module.security.frontend_security_group_id
  backend_security_group_id    = module.security.backend_security_group_id
  monitoring_security_group_id = module.security.monitoring_security_group_id

  ecs_task_execution_role_arn      = module.iam.ecs_task_execution_role_arn
  ecs_task_role_arn                = module.iam.ecs_task_role_arn
  monitoring_instance_profile_name = module.iam.monitoring_instance_profile_name

  frontend_image = "${module.ecr.repository_url}:frontend-${var.image_tag}"
  backend_image  = "${module.ecr.repository_url}:backend-${var.image_tag}"

  frontend_tg_arn = module.alb.frontend_target_group_arn
  backend_tg_arn  = module.alb.backend_target_group_arn

  alb_dns_name     = module.alb.alb_dns_name
  ses_sender_email = var.ses_sender_email

  spring_datasource_url_ssm_arn = module.secrets.spring_datasource_url_ssm_arn
  db_credentials_secret_arn     = module.secrets.db_credentials_secret_arn
  jwt_secret_arn                = module.secrets.jwt_secret_arn

  frontend_cpu             = var.frontend_cpu
  frontend_memory          = var.frontend_memory
  backend_cpu              = var.backend_cpu
  backend_memory           = var.backend_memory
  frontend_desired_count   = var.frontend_desired_count
  backend_desired_count    = var.backend_desired_count
  monitoring_instance_type = var.monitoring_instance_type

  tags = local.common_tags
}

##############################################################################
# Application Load Balancer — internet-facing, routes /* → Frontend, /api/* → Backend
##############################################################################
module "alb" {
  source = "./modules/alb"

  project_name          = local.project
  vpc_id                = module.network.vpc_id
  public_subnet_ids     = module.network.public_subnet_ids
  alb_security_group_id = module.security.alb_security_group_id
  # Enable deletion protection only in production
  enable_deletion_protection = var.environment == "prod"

  tags = local.common_tags
}

##############################################################################
# RDS PostgreSQL — private DB subnet, encrypted, with automated backups
##############################################################################
module "rds" {
  source = "./modules/rds"

  project_name          = local.project
  db_identifier         = local.rds_identifier
  db_name               = local.db_name
  db_username           = var.db_username
  db_password           = random_password.db_password.result
  db_instance_class     = var.db_instance_class
  db_allocated_storage  = var.db_allocated_storage
  multi_az              = var.db_multi_az
  backup_retention_days = var.db_backup_retention_days
  deletion_protection   = var.environment == "prod"

  enable_performance_insights = var.environment == "prod"

  db_subnet_ids         = module.network.private_db_subnet_ids
  rds_security_group_id = module.security.rds_security_group_id

  tags = local.common_tags
}

##############################################################################
# ECR — container image registry for CI/CD pipeline
##############################################################################
module "ecr" {
  source          = "./modules/ecr"
  repository_name = var.ecr_repository_name
  tags            = local.common_tags
}

##############################################################################
# Security Services — CloudTrail + GuardDuty
##############################################################################
module "security_services" {
  source           = "./modules/security_services"
  project_name     = local.project
  aws_region       = var.aws_region
  environment      = local.environment
  ses_sender_email = var.ses_sender_email
  tags             = local.common_tags
}

##############################################################################
# Secrets — Secrets Manager (DB creds, JWT key) + SSM Parameter Store (config)
# Depends on: RDS (db_host), ALB (alb_dns_name), ECR (ecr_repository_url)
##############################################################################
module "secrets" {
  source = "./modules/secrets"

  project_name = local.project
  environment  = local.environment

  # DB credentials — password generated above, host comes from RDS after creation
  db_host     = module.rds.db_host
  db_port     = module.rds.db_port
  db_name     = local.db_name
  db_username = var.db_username
  db_password = random_password.db_password.result

  # App secrets
  jwt_secret = random_password.jwt_secret.result

  # Infrastructure endpoints
  alb_dns_name       = module.alb.alb_dns_name
  ecr_repository_url = module.ecr.repository_url

  # Monitoring
  grafana_admin_password = random_password.grafana_admin.result

  tags = local.common_tags
}
