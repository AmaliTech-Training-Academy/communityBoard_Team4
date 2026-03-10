output "frontend_instance_id" {
  description = "EC2 instance ID of the Frontend host"
  value       = aws_instance.frontend.id
}

output "backend_instance_id" {
  description = "EC2 instance ID of the Backend host"
  value       = aws_instance.backend.id
}

output "frontend_private_ip" {
  description = "Private IP of the Frontend EC2 instance"
  value       = aws_instance.frontend.private_ip
}

output "backend_private_ip" {
  description = "Private IP of the Backend EC2 instance"
  value       = aws_instance.backend.private_ip
}

output "monitoring_public_ip" {
  description = "Public IP of the Monitoring host"
  value       = aws_instance.monitoring.public_ip
}

output "monitoring_public_dns" {
  description = "Public DNS of the Monitoring host"
  value       = aws_instance.monitoring.public_dns
}
