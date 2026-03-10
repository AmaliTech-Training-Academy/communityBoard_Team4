data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

##############################################################################
# Monitoring role — Prometheus + Grafana + Alertmanager host
# Permissions: SSM (session manager) + CloudWatch read (metrics/logs scraping)
##############################################################################
resource "aws_iam_role" "monitoring" {
  name               = "${var.project_name}-monitoring-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = merge(var.tags, { Role = "monitoring" })
}

resource "aws_iam_role_policy_attachment" "monitoring_ssm" {
  role       = aws_iam_role.monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "monitoring_cloudwatch" {
  statement {
    sid    = "CloudWatchReadMetrics"
    effect = "Allow"
    actions = [
      "cloudwatch:GetMetricData",
      "cloudwatch:ListMetrics",
      "cloudwatch:GetMetricStatistics",
      "logs:DescribeLogGroups",
      "logs:DescribeLogStreams",
      "logs:GetLogEvents",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "monitoring_cloudwatch" {
  name   = "${var.project_name}-monitoring-cw-read"
  role   = aws_iam_role.monitoring.id
  policy = data.aws_iam_policy_document.monitoring_cloudwatch.json
}

resource "aws_iam_instance_profile" "monitoring" {
  name = "${var.project_name}-monitoring-instance-profile"
  role = aws_iam_role.monitoring.name

  tags = merge(var.tags, { Role = "monitoring" })
}

##############################################################################
# App role — shared by Frontend and Backend EC2 instances
# Permissions: ECR read (pull images), SSM (session manager), CloudWatch logs
##############################################################################
resource "aws_iam_role" "app" {
  name               = "${var.project_name}-app-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = merge(var.tags, { Role = "app" })
}

resource "aws_iam_role_policy_attachment" "app_ecr_readonly" {
  role       = aws_iam_role.app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "app_ssm" {
  role       = aws_iam_role.app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "app_cloudwatch" {
  statement {
    sid    = "CloudWatchLogsWrite"
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:DescribeLogStreams",
    ]
    resources = ["arn:aws:logs:*:*:log-group:/app/${var.project_name}/*"]
  }
}

resource "aws_iam_role_policy" "app_cloudwatch" {
  name   = "${var.project_name}-app-cw-logs"
  role   = aws_iam_role.app.id
  policy = data.aws_iam_policy_document.app_cloudwatch.json
}

resource "aws_iam_instance_profile" "app" {
  name = "${var.project_name}-app-instance-profile"
  role = aws_iam_role.app.name

  tags = merge(var.tags, { Role = "app" })
}

##############################################################################
# App secrets access — Secrets Manager + SSM Parameter Store
# Scoped to /<project>/<environment>/* so each env is isolated.
##############################################################################

data "aws_iam_policy_document" "app_secrets" {
  statement {
    sid    = "SecretsManagerRead"
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
    ]
    resources = [
      "arn:aws:secretsmanager:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:secret:/${var.project_name}/${var.environment}/*",
    ]
  }

  statement {
    sid    = "SSMParameterRead"
    effect = "Allow"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParametersByPath",
    ]
    resources = [
      "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/${var.project_name}/${var.environment}/*",
    ]
  }

  # Allow KMS Decrypt when SSM SecureString or Secrets Manager uses the AWS
  # managed key — constrained to the two services so it doesn't act as a
  # wildcard KMS grant.
  statement {
    sid    = "KMSDecryptViaServices"
    effect = "Allow"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values = [
        "ssm.${data.aws_region.current.name}.amazonaws.com",
        "secretsmanager.${data.aws_region.current.name}.amazonaws.com",
      ]
    }
  }
}

resource "aws_iam_role_policy" "app_secrets" {
  name   = "${var.project_name}-app-secrets-read"
  role   = aws_iam_role.app.id
  policy = data.aws_iam_policy_document.app_secrets.json
}
