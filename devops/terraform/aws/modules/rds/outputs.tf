output "db_endpoint" {
  description = "RDS instance endpoint (host:port)"
  value       = aws_db_instance.postgres.endpoint
}

output "db_host" {
  description = "RDS instance hostname only (without port)"
  value       = aws_db_instance.postgres.address
}

output "db_port" {
  description = "RDS instance port"
  value       = aws_db_instance.postgres.port
}

output "db_name" {
  description = "Name of the initial database"
  value       = aws_db_instance.postgres.db_name
}

output "db_identifier" {
  description = "RDS instance identifier"
  value       = aws_db_instance.postgres.identifier
}
