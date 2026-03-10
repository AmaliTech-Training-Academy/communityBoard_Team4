##############################################################################
# modules/secrets/outputs.tf
##############################################################################

# ── Secrets Manager ARNs — referenced in IAM allow-policies ──────────────────

output "db_credentials_secret_arn" {
  description = "ARN of the Secrets Manager secret holding the RDS credentials JSON"
  value       = aws_secretsmanager_secret.db_credentials.arn
}

output "jwt_secret_arn" {
  description = "ARN of the Secrets Manager secret holding the JWT signing key"
  value       = aws_secretsmanager_secret.jwt_secret.arn
}

# ── SSM path prefix — useful for scripts and documentation ───────────────────

output "ssm_path_prefix" {
  description = "SSM Parameter Store path prefix for all project parameters"
  value       = "/${var.project_name}/${var.environment}"
}

output "ssm_spring_datasource_url_name" {
  description = "SSM parameter name for the pre-assembled Spring Boot datasource URL"
  value       = aws_ssm_parameter.spring_datasource_url.name
}

output "spring_datasource_url_ssm_arn" {
  description = "ARN of the SSM parameter holding the pre-assembled JDBC URL (injected as SPRING_DATASOURCE_URL)"
  value       = aws_ssm_parameter.spring_datasource_url.arn
}

output "ssm_grafana_password_name" {
  description = "SSM parameter name for the Grafana admin password"
  value       = aws_ssm_parameter.grafana_admin_password.name
}
