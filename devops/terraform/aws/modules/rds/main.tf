##############################################################################
# RDS PostgreSQL — private DB subnet, multi-AZ, encrypted at rest
##############################################################################

resource "aws_db_subnet_group" "this" {
  name        = "${var.project_name}-db-subnet-group"
  subnet_ids  = var.db_subnet_ids
  description = "DB subnet group for ${var.project_name} RDS PostgreSQL"

  tags = merge(var.tags, {
    Name = "${var.project_name}-db-subnet-group"
    Tier = "private-db"
  })
}

resource "aws_db_parameter_group" "postgres" {
  name        = "${var.project_name}-postgres-params"
  family      = "postgres15"
  description = "Custom parameter group for ${var.project_name} PostgreSQL 15"

  # Enforce SSL connections — no plaintext traffic on the wire
  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }

  # Reduce idle session overhead
  parameter {
    name  = "idle_in_transaction_session_timeout"
    value = "300000"   # 5 minutes in ms
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-postgres-params"
    Tier = "private-db"
  })
}

resource "aws_db_instance" "postgres" {
  identifier        = var.db_identifier
  engine            = "postgres"
  engine_version    = "15.7"
  instance_class    = var.db_instance_class
  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  multi_az          = var.multi_az

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.rds_security_group_id]
  parameter_group_name   = aws_db_parameter_group.postgres.name

  # Encryption at rest
  storage_encrypted = true
  kms_key_id        = var.kms_key_arn   # null → AWS-managed key

  # Backups — 7-day retention with a preferred window
  backup_retention_period   = var.backup_retention_days
  backup_window             = "02:00-03:00"
  maintenance_window        = "Mon:04:00-Mon:05:00"
  delete_automated_backups  = false

  # Prevent accidental deletion in production
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "${var.db_identifier}-final" : null

  # Performance Insights (enabled for non-free-tier usage)
  performance_insights_enabled = var.enable_performance_insights

  # Disable public internet access — DB is in a private subnet
  publicly_accessible = false

  # Minor version auto-upgrade within the selected engine version
  auto_minor_version_upgrade = true

  tags = merge(var.tags, {
    Name = var.db_identifier
    Tier = "private-db"
  })
}
