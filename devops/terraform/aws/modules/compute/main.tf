##############################################################################
# Frontend EC2 — Nginx serving the React SPA (private app subnet)
##############################################################################
resource "aws_instance" "frontend" {
  ami                         = var.ami_id
  instance_type               = var.frontend_instance_type
  subnet_id                   = var.private_app_subnet_ids[0]
  vpc_security_group_ids      = [var.frontend_security_group_id]
  key_name                    = var.key_name
  associate_public_ip_address = false
  iam_instance_profile        = var.app_instance_profile_name

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"   # IMDSv2 enforced
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true

    tags = merge(var.tags, {
      Name = "${var.project_name}-frontend-root"
    })
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-frontend"
    Role = "frontend"
    Tier = "private-app"
  })
}

##############################################################################
# Backend EC2 — Spring Boot API server (private app subnet)
##############################################################################
resource "aws_instance" "backend" {
  ami                         = var.ami_id
  instance_type               = var.backend_instance_type
  subnet_id                   = var.private_app_subnet_ids[0]
  vpc_security_group_ids      = [var.backend_security_group_id]
  key_name                    = var.key_name
  associate_public_ip_address = false
  iam_instance_profile        = var.app_instance_profile_name

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"   # IMDSv2 enforced
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true

    tags = merge(var.tags, {
      Name = "${var.project_name}-backend-root"
    })
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-backend"
    Role = "backend"
    Tier = "private-app"
  })
}

##############################################################################
# Monitoring EC2 — Prometheus + Grafana + Alertmanager (public subnet)
##############################################################################
resource "aws_instance" "monitoring" {
  ami                         = var.ami_id
  instance_type               = var.monitoring_instance_type
  subnet_id                   = var.public_subnet_id
  vpc_security_group_ids      = [var.monitoring_security_group_id]
  key_name                    = var.key_name
  associate_public_ip_address = true
  iam_instance_profile        = var.monitoring_instance_profile_name

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"   # IMDSv2 enforced
    http_put_response_hop_limit = 1
  }

  root_block_device {
    # Prometheus TSDB needs adequate storage for metrics retention
    volume_size = 50
    volume_type = "gp3"
    encrypted   = true

    tags = merge(var.tags, {
      Name = "${var.project_name}-monitoring-root"
    })
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-monitoring"
    Role = "monitoring"
    Tier = "ops"
  })
}
