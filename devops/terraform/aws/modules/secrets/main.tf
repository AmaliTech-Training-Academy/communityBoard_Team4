##############################################################################
# modules/secrets/main.tf
#
# Two-tier secret storage strategy:
#   Secrets Manager  → highly sensitive credentials that may need rotation
#                      (DB credentials JSON blob, JWT signing key)
#   Parameter Store  → application config + lower-sensitivity credentials
#                      (JDBC URL, ALB DNS, ECR URL, Grafana admin password)
#
# All paths follow the convention:
#   /<project>/<environment>/<category>/<name>
##############################################################################

##############################################################################
# AWS Secrets Manager
##############################################################################

# RDS credentials — JSON blob matching the AWS Secrets Manager rotation schema
# so that automatic rotation (when enabled) works without code changes.
resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "/${var.project_name}/${var.environment}/db/credentials"
  description             = "RDS master credentials for ${var.project_name} (${var.environment})"
  recovery_window_in_days = var.environment == "prod" ? 7 : 0

  tags = merge(var.tags, { SecretType = "db-credentials" })
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id

  # JSON schema is compatible with the RDS Single-User rotation Lambda
  secret_string = jsonencode({
    engine   = "postgres"
    host     = var.db_host
    port     = var.db_port
    dbname   = var.db_name
    username = var.db_username
    password = var.db_password
  })
}

# JWT signing key — Spring Boot reads this at startup to sign / verify tokens
resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "/${var.project_name}/${var.environment}/app/jwt-secret"
  description             = "JWT signing key for ${var.project_name} (${var.environment})"
  recovery_window_in_days = var.environment == "prod" ? 7 : 0

  tags = merge(var.tags, { SecretType = "jwt-secret" })
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

##############################################################################
# SSM Parameter Store — application config & runtime values
##############################################################################

# ── Database connection parameters ─────────────────────────────────────────

resource "aws_ssm_parameter" "db_host" {
  name        = "/${var.project_name}/${var.environment}/db/host"
  type        = "String"
  value       = var.db_host
  description = "RDS PostgreSQL hostname"

  tags = var.tags
}

resource "aws_ssm_parameter" "db_port" {
  name        = "/${var.project_name}/${var.environment}/db/port"
  type        = "String"
  value       = tostring(var.db_port)
  description = "RDS PostgreSQL port"

  tags = var.tags
}

resource "aws_ssm_parameter" "db_name" {
  name        = "/${var.project_name}/${var.environment}/db/name"
  type        = "String"
  value       = var.db_name
  description = "Application database name"

  tags = var.tags
}

# Username stored as SecureString — never expose it as plaintext in outputs
resource "aws_ssm_parameter" "db_username" {
  name        = "/${var.project_name}/${var.environment}/db/username"
  type        = "SecureString"
  value       = var.db_username
  description = "RDS master username"

  tags = var.tags
}

# Pre-assembled JDBC URL — backend reads SPRING_DATASOURCE_URL from this param
resource "aws_ssm_parameter" "spring_datasource_url" {
  name        = "/${var.project_name}/${var.environment}/app/spring-datasource-url"
  type        = "String"
  value       = "jdbc:postgresql://${var.db_host}:${var.db_port}/${var.db_name}?sslmode=require"
  description = "Spring Boot SPRING_DATASOURCE_URL (includes RDS host + SSL flag)"

  tags = var.tags
}

# ── Infrastructure endpoints ────────────────────────────────────────────────

resource "aws_ssm_parameter" "alb_dns" {
  name        = "/${var.project_name}/${var.environment}/app/alb-dns"
  type        = "String"
  value       = var.alb_dns_name
  description = "ALB public DNS — use as the REACT_APP_API_BASE_URL base"

  tags = var.tags
}

resource "aws_ssm_parameter" "ecr_url" {
  name        = "/${var.project_name}/${var.environment}/app/ecr-url"
  type        = "String"
  value       = var.ecr_repository_url
  description = "ECR repository URL for pulling application container images"

  tags = var.tags
}

# ── Monitoring ──────────────────────────────────────────────────────────────

resource "aws_ssm_parameter" "grafana_admin_password" {
  name        = "/${var.project_name}/${var.environment}/monitoring/grafana-admin-password"
  type        = "SecureString"
  value       = var.grafana_admin_password
  description = "Grafana admin user password (GRAFANA_ADMIN_PASSWORD)"

  tags = var.tags
}
