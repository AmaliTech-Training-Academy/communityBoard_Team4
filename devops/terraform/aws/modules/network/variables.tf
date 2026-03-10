variable "project_name" {
  description = "Project name prefix for resource naming"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ, used by ALB + NAT GW)"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private app subnets (one per AZ, used by Frontend + Backend)"
  type        = list(string)
}

variable "db_subnet_cidrs" {
  description = "CIDR blocks for private DB subnets (one per AZ, used by RDS)"
  type        = list(string)
}

variable "availability_zones" {
  description = "List of AZ names — must match the count of each subnet list"
  type        = list(string)
}

variable "tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
}
