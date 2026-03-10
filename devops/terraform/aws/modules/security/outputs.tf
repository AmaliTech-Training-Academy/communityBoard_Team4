output "alb_security_group_id" {
  description = "Security group ID for the Application Load Balancer"
  value       = aws_security_group.alb.id
}

output "frontend_security_group_id" {
  description = "Security group ID for Frontend (Nginx) instances"
  value       = aws_security_group.frontend.id
}

output "backend_security_group_id" {
  description = "Security group ID for Backend (Spring Boot) instances"
  value       = aws_security_group.backend.id
}

output "rds_security_group_id" {
  description = "Security group ID for RDS PostgreSQL"
  value       = aws_security_group.rds.id
}

output "monitoring_security_group_id" {
  description = "Security group ID for Monitoring host (Prometheus + Grafana + Alertmanager)"
  value       = aws_security_group.monitoring.id
}
