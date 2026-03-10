##############################################################################
# outputs.tf — root module outputs for the Community Board infrastructure
##############################################################################

# ── ALB ──────────────────────────────────────────────────────────────────────
output "app_url" {
  description = "Application URL via the ALB DNS name"
  value       = "http://${module.alb.alb_dns_name}"
}

output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer"
  value       = module.alb.alb_dns_name
}

output "alb_zone_id" {
  description = "Hosted zone ID of the ALB (use for Route 53 alias records when a domain is added later)"
  value       = module.alb.alb_zone_id
}

# ── Compute ───────────────────────────────────────────────────────────────────
output "frontend_private_ip" {
  description = "Private IP of the Frontend (Nginx) EC2 instance"
  value       = module.compute.frontend_private_ip
}

output "backend_private_ip" {
  description = "Private IP of the Backend (Spring Boot) EC2 instance"
  value       = module.compute.backend_private_ip
}

output "monitoring_public_ip" {
  description = "Public IP of the Monitoring host (Prometheus + Grafana + Alertmanager)"
  value       = module.compute.monitoring_public_ip
}

output "monitoring_public_dns" {
  description = "Public DNS hostname of the Monitoring host"
  value       = module.compute.monitoring_public_dns
}

# ── RDS ──────────────────────────────────────────────────────────────────────
output "db_host" {
  description = "RDS PostgreSQL hostname (without port) — use in SPRING_DATASOURCE_URL"
  value       = module.rds.db_host
}

output "db_port" {
  description = "RDS PostgreSQL port"
  value       = module.rds.db_port
}

output "db_name" {
  description = "Name of the application database"
  value       = module.rds.db_name
}

output "db_endpoint" {
  description = "Full RDS endpoint in host:port format"
  value       = module.rds.db_endpoint
  sensitive   = true
}

# ── ECR ───────────────────────────────────────────────────────────────────────
output "ecr_repository_url" {
  description = "ECR repository URL for pushing/pulling container images"
  value       = module.ecr.repository_url
}

# ── Security Services ─────────────────────────────────────────────────────────
output "cloudtrail_bucket" {
  description = "S3 bucket name for CloudTrail logs"
  value       = module.security_services.cloudtrail_bucket_name
}

output "cloudtrail_trail_arn" {
  description = "ARN of the CloudTrail trail"
  value       = module.security_services.cloudtrail_trail_arn
}

output "guardduty_detector_id" {
  description = "ID of the GuardDuty detector"
  value       = module.security_services.guardduty_detector_id
}

# ── Networking ────────────────────────────────────────────────────────────────
output "vpc_id" {
  description = "ID of the application VPC"
  value       = module.network.vpc_id
}

# ── Secrets / Parameter Store ─────────────────────────────────────────────────
output "db_credentials_secret_arn" {
  description = "Secrets Manager ARN for the RDS credentials JSON (engine/host/port/dbname/username/password)"
  value       = module.secrets.db_credentials_secret_arn
}

output "jwt_secret_arn" {
  description = "Secrets Manager ARN for the JWT signing key"
  value       = module.secrets.jwt_secret_arn
}

output "ssm_path_prefix" {
  description = "SSM Parameter Store path prefix — all app parameters live under this path"
  value       = module.secrets.ssm_path_prefix
}

output "ssm_spring_datasource_url" {
  description = "SSM parameter name holding the pre-assembled Spring Boot SPRING_DATASOURCE_URL"
  value       = module.secrets.ssm_spring_datasource_url_name
}

output "ssm_grafana_password" {
  description = "SSM parameter name for the Grafana admin password"
  value       = module.secrets.ssm_grafana_password_name
}
