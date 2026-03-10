variable "project_name" {
  type = string
}

variable "environment" {
  description = "Deployment environment — used to scope IAM policy resource paths"
  type        = string
}

variable "tags" {
  type = map(string)
}
