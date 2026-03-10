variable "project_name" {
  description = "Project name prefix for resource naming"
  type        = string
}

variable "db_identifier" {
  description = "RDS instance identifier (lower-case, hyphens allowed)"
  type        = string
}

variable "db_name" {
  description = "Name of the initial PostgreSQL database"
  type        = string
  default     = "communityboard"
}

variable "db_username" {
  description = "Master DB username"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Master DB password — pass via TF_VAR or a secrets manager; never commit"
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 20
}

variable "multi_az" {
  description = "Enable Multi-AZ deployment for high availability"
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  description = "Number of days to retain automated backups (0 to disable)"
  type        = number
  default     = 7
}

variable "deletion_protection" {
  description = "Prevent accidental deletion of the RDS instance"
  type        = bool
  default     = true
}

variable "enable_performance_insights" {
  description = "Enable RDS Performance Insights"
  type        = bool
  default     = false
}

variable "kms_key_arn" {
  description = "KMS key ARN for RDS storage encryption (null = AWS-managed key)"
  type        = string
  default     = null
}

variable "db_subnet_ids" {
  description = "List of private DB subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "rds_security_group_id" {
  description = "Security group ID to attach to the RDS instance"
  type        = string
}

variable "tags" {
  description = "Common tags to apply to all RDS resources"
  type        = map(string)
}
