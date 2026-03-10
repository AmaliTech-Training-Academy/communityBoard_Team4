output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.this.name
}

output "ecs_cluster_arn" {
  description = "ARN of the ECS cluster"
  value       = aws_ecs_cluster.this.arn
}

output "frontend_service_name" {
  description = "Name of the Frontend ECS service"
  value       = aws_ecs_service.frontend.name
}

output "backend_service_name" {
  description = "Name of the Backend ECS service"
  value       = aws_ecs_service.backend.name
}

output "monitoring_public_ip" {
  description = "Public IP of the Monitoring EC2 host"
  value       = aws_instance.monitoring.public_ip
}

output "monitoring_public_dns" {
  description = "Public DNS of the Monitoring EC2 host"
  value       = aws_instance.monitoring.public_dns
}
