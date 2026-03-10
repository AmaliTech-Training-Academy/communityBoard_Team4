output "monitoring_instance_profile_name" {
  description = "IAM instance profile name for the Monitoring host"
  value       = aws_iam_instance_profile.monitoring.name
}

output "app_instance_profile_name" {
  description = "IAM instance profile name shared by Frontend and Backend app hosts"
  value       = aws_iam_instance_profile.app.name
}
