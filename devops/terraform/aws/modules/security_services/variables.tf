variable "project_name" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "environment" {
  type = string
}

variable "ses_sender_email" {
  description = "SES sender email identity for application emails"
  type        = string

  validation {
    condition     = can(regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", var.ses_sender_email)) && !endswith(lower(var.ses_sender_email), "@example.com") && !startswith(lower(var.ses_sender_email), "no-reply@example")
    error_message = "ses_sender_email must be a real, active sender address verified in SES."
  }
}

variable "tags" {
  type = map(string)
}
