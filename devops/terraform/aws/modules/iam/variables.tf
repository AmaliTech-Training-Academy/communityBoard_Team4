variable "project_name" {
  type = string
}

variable "environment" {
  description = "Deployment environment — used to scope IAM policy resource paths"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository in org/repo format (e.g. amalitech/communityBoard_Team4). Used to scope the OIDC trust policy so only this repo can assume the GitHub Actions role."
  type        = string
}

variable "tags" {
  type = map(string)
}
