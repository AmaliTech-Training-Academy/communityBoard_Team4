data "aws_region" "current" {}

##############################################################################
# ECS Cluster — Fargate-powered, Container Insights enabled
##############################################################################
resource "aws_ecs_cluster" "this" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-cluster"
  })
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  # Prefer Spot for ~70% cost savings; on-demand FARGATE is the fallback
  default_capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 4
    base              = 0
  }

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 0
  }
}

##############################################################################
# CloudWatch Log Groups — 30-day retention per service
##############################################################################
resource "aws_cloudwatch_log_group" "frontend" {
  name              = "/${var.project_name}/${var.environment}/frontend"
  retention_in_days = 30
  tags = merge(var.tags, { Name = "${var.project_name}-frontend-logs", Role = "frontend" })
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/${var.project_name}/${var.environment}/backend"
  retention_in_days = 30
  tags = merge(var.tags, { Name = "${var.project_name}-backend-logs", Role = "backend" })
}

##############################################################################
# Frontend Task Definition — Nginx serving the React SPA
##############################################################################
resource "aws_ecs_task_definition" "frontend" {
  family                   = "${var.project_name}-frontend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = var.ecs_task_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = var.frontend_image
      essential = true

      portMappings = [{ containerPort = 80, protocol = "tcp" }]

      environment = [
        { name = "PORT", value = "80" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.frontend.name
          "awslogs-region"        = data.aws_region.current.name
          "awslogs-stream-prefix" = "frontend"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost/ || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 30
      }
    }
  ])

  tags = merge(var.tags, {
    Name = "${var.project_name}-frontend-task"
    Role = "frontend"
    Tier = "private-app"
  })
}

##############################################################################
# Backend Task Definition — Spring Boot API (secrets injected by ECS agent)
##############################################################################
resource "aws_ecs_task_definition" "backend" {
  family                   = "${var.project_name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = var.ecs_task_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = var.backend_image
      essential = true

      portMappings = [{ containerPort = 8080, protocol = "tcp" }]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
        { name = "SERVER_PORT",            value = "8080" }
      ]

      # Secrets injected at container start by the ECS agent — never in plaintext
      # Names match application.yml: SPRING_DATASOURCE_URL / USERNAME / PASSWORD / JWT_SECRET
      secrets = [
        { name = "SPRING_DATASOURCE_URL",      valueFrom = var.spring_datasource_url_ssm_arn },
        { name = "SPRING_DATASOURCE_USERNAME", valueFrom = "${var.db_credentials_secret_arn}:username::" },
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = "${var.db_credentials_secret_arn}:password::" },
        { name = "JWT_SECRET",                 valueFrom = var.jwt_secret_arn }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend.name
          "awslogs-region"        = data.aws_region.current.name
          "awslogs-stream-prefix" = "backend"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = merge(var.tags, {
    Name = "${var.project_name}-backend-task"
    Role = "backend"
    Tier = "private-app"
  })
}

##############################################################################
# Frontend ECS Service
##############################################################################
resource "aws_ecs_service" "frontend" {
  name            = "${var.project_name}-frontend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.frontend_desired_count

  force_new_deployment = true

  # Prefer Spot (~70% cheaper); fall back to on-demand if no Spot capacity available
  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 4
    base              = 0
  }

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets          = var.app_subnet_ids
    security_groups  = [var.frontend_security_group_id]
    assign_public_ip = true # tasks in public subnets; no NAT GW; SGs restrict all inbound to ALB only
  }

  load_balancer {
    target_group_arn = var.frontend_tg_arn
    container_name   = "frontend"
    container_port   = 80
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  # Prevent Terraform from reverting manual scaling or CI/CD rolling deploys
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-frontend-svc"
    Role = "frontend"
    Tier = "private-app"
  })
}

##############################################################################
# Backend ECS Service
##############################################################################
resource "aws_ecs_service" "backend" {
  name            = "${var.project_name}-backend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count

  force_new_deployment = true

  # Prefer Spot (~70% cheaper); fall back to on-demand if no Spot capacity available
  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 4
    base              = 0
  }

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets          = var.app_subnet_ids
    security_groups  = [var.backend_security_group_id]
    assign_public_ip = true # tasks in public subnets; no NAT GW; SGs restrict inbound to ALB/frontend only
  }

  load_balancer {
    target_group_arn = var.backend_tg_arn
    container_name   = "backend"
    container_port   = 8080
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-backend-svc"
    Role = "backend"
    Tier = "private-app"
  })
}

##############################################################################
# Monitoring EC2 — Grafana + CloudWatch Metrics (public subnet)
# t3.micro is sufficient; Prometheus removed in favour of CloudWatch-native metrics.
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
    # Grafana config + dashboard DB only; no Prometheus TSDB needed
    volume_size = 20
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
