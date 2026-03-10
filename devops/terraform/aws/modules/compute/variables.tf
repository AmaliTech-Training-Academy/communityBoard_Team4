variable "project_name" {
  description = "Project name prefix for resource naming"
  type        = string
}

variable "ami_id" {
  description = "AMI ID for all EC2 instances"
  type        = string
}

variable "public_subnet_id" {
  description = "Public subnet ID for Monitoring host"
  type        = string
}

variable "private_app_subnet_ids" {
  description = "Private app subnet IDs for Frontend and Backend instances"
  type        = list(string)
}

variable "key_name" {
  description = "EC2 key pair name for SSH access"
  type        = string
}

variable "ssh_key_fingerprint" {
  description = "SSH public key fingerprint"
  type        = string
}

variable "monitoring_security_group_id" {
  description = "Security group ID for the Monitoring host (Prometheus + Grafana + Alertmanager)"
  type        = string
}

variable "frontend_security_group_id" {
  description = "Security group ID for Frontend instances"
  type        = string
}

variable "backend_security_group_id" {
  description = "Security group ID for Backend instances"
  type        = string
}

variable "monitoring_instance_profile_name" {
  description = "IAM instance profile name for the Monitoring host"
  type        = string
}

variable "app_instance_profile_name" {
  description = "IAM instance profile name for Frontend/Backend app instances"
  type        = string
}

variable "frontend_instance_type" {
  description = "EC2 instance type for Frontend (Nginx) hosts"
  type        = string
  default     = "t3.small"
}

variable "backend_instance_type" {
  description = "EC2 instance type for Backend (Spring Boot) hosts"
  type        = string
  default     = "t3.small"
}

variable "monitoring_instance_type" {
  description = "EC2 instance type for Monitoring host (Prometheus + Grafana + Alertmanager)"
  type        = string
  default     = "t3.medium"
}

variable "tags" {
  description = "Common tags to apply to all EC2 instances"
  type        = map(string)
}
