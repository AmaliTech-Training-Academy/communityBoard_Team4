##############################################################################
# Application Load Balancer — internet-facing, routes to Frontend & Backend
#
# Routing rules:
#   /api/*   → Backend target group (Spring Boot :8080)
#   /*       → Frontend target group (Nginx :80)
##############################################################################

##############################################################################
# ALB
##############################################################################
resource "aws_lb" "this" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  # Enable deletion protection in production to prevent accidental teardown
  enable_deletion_protection = var.enable_deletion_protection

  # Drop invalid HTTP headers to prevent smuggling attacks
  drop_invalid_header_fields = true

  tags = merge(var.tags, {
    Name = "${var.project_name}-alb"
    Tier = "public"
  })
}

##############################################################################
# Target Groups
##############################################################################

resource "aws_lb_target_group" "frontend" {
  name        = "${var.project_name}-frontend-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip" # Required for Fargate (tasks register by ENI IP)

  health_check {
    path                = "/"
    protocol            = "HTTP"
    port                = "traffic-port"
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200-399"
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-frontend-tg"
    Tier = "private-app"
  })
}

resource "aws_lb_target_group" "backend" {
  name        = "${var.project_name}-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip" # Required for Fargate (tasks register by ENI IP)

  health_check {
    path                = "/actuator/health"
    protocol            = "HTTP"
    port                = "traffic-port"
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-backend-tg"
    Tier = "private-app"
  })
}

##############################################################################
# HTTP Listener
# ECS Fargate registers its own tasks — no static target group attachments needed — forwards to Frontend TG by default
##############################################################################

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-http-listener"
  })
}

##############################################################################
# Listener Rules — route /api/* to Backend TG
##############################################################################

resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-api-rule"
  })
}
