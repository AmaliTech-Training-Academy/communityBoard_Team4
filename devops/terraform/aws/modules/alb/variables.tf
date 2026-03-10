variable "project_name" {
  description = "Project name prefix for resource naming"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the ALB will be deployed"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs across which the ALB is spread (minimum 2 for HA)"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "Security group ID to attach to the ALB"
  type        = string
}

variable "frontend_instance_id" {
  description = "EC2 instance ID of the Frontend host to register in the Frontend TG"
  type        = string
}

variable "backend_instance_id" {
  description = "EC2 instance ID of the Backend host to register in the Backend TG"
  type        = string
}



variable "enable_deletion_protection" {
  description = "Prevent accidental deletion of the ALB"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Common tags to apply to all ALB resources"
  type        = map(string)
}
