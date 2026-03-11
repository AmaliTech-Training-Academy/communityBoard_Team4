output "ecs_task_execution_role_arn" {
  description = "ARN of the ECS task execution role (used by ECS agent)"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "ecs_task_role_arn" {
  description = "ARN of the ECS task role (assumed by running container code)"
  value       = aws_iam_role.app.arn
}

output "monitoring_instance_profile_name" {
  description = "IAM instance profile name for the Monitoring host"
  value       = aws_iam_instance_profile.monitoring.name
}

output "github_actions_role_arn" {
  description = "ARN of the GitHub Actions OIDC role — set this as AWS_ROLE_ARN in GitHub repository secrets"
  value       = aws_iam_role.github_actions.arn
}
