##############################################################################
# locals.tf — project-wide derived values and common tags
# All modules consume these via the root var.tags or explicit inputs.
##############################################################################

locals {
  # -------------------------------------------------------------------------
  # Naming & identity
  # -------------------------------------------------------------------------
  project     = var.project_name # "community-board"
  environment = var.environment  # "prod" | "staging" | "dev"
  name_prefix = "${local.project}-${local.environment}"

  # -------------------------------------------------------------------------
  # Common resource tags applied to every resource via merge(local.common_tags, {...})
  # -------------------------------------------------------------------------
  common_tags = {
    Project     = local.project
    Environment = local.environment
    Team        = "Team4"
    ManagedBy   = "terraform"
    Repository  = "communityBoard_Team4"
  }

  # -------------------------------------------------------------------------
  # Network addressing — sourced from variables so tfvars controls the layout.
  #
  # Default layout (var defaults / tfvars):
  #   10.30.0.0/24   — public-1   (ALB node, NAT GW)
  #   10.30.1.0/24   — public-2   (ALB node — AZ b)
  #   10.30.10.0/24  — private-app-1  (Frontend + Backend — AZ a)
  #   10.30.11.0/24  — private-app-2  (Frontend + Backend — AZ b)
  #   10.30.20.0/24  — private-db-1   (RDS primary — AZ a)
  #   10.30.21.0/24  — private-db-2   (RDS Multi-AZ standby — AZ b)
  # -------------------------------------------------------------------------
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  db_subnet_cidrs      = var.db_subnet_cidrs

  # -------------------------------------------------------------------------
  # Application port map — single source of truth referred to in docs/scripts
  # -------------------------------------------------------------------------
  port = {
    http     = 80
    https    = 443
    backend  = 8080 # Spring Boot
    postgres = 5432
    ssh      = 22
  }

  # -------------------------------------------------------------------------
  # RDS identifier (must be lower-case, ≤63 chars, hyphens allowed)
  # -------------------------------------------------------------------------
  rds_identifier = "${local.name_prefix}-postgres"
  db_name        = "communityboard"
}
