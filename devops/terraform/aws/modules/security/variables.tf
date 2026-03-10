variable "project_name" {
  description = "Project name prefix for resource naming"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC where security groups will be created"
  type        = string
}

variable "admin_cidrs" {
  description = "Allowed CIDRs for SSH / ops access (ops, CI/CD hosts)"
  type        = list(string)
}

variable "tags" {
  description = "Common tags to apply to all security groups"
  type        = map(string)
}
