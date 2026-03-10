#!/bin/bash
# ECS rolling deploy for CommunityBoard
# Usage: ./deploy.sh <image-tag> [environment]
# Example: ./deploy.sh abc1234 prod
#
# Pre-requisites:
#   - AWS CLI configured with sufficient permissions
#   - jq installed
#   - IMAGE_TAG passed as first argument (typically the Git SHA from CI/CD)

set -euo pipefail

IMAGE_TAG=${1:?"Usage: $0 <image-tag> [environment]"}
ENVIRONMENT=${2:-prod}
AWS_REGION=${AWS_REGION:-eu-west-1}
PROJECT=community-board
ECR_REPO=$(aws ecr describe-repositories \
  --repository-names "${PROJECT}" \
  --region "${AWS_REGION}" \
  --query 'repositories[0].repositoryUri' \
  --output text 2>/dev/null || true)

if [[ -z "${ECR_REPO}" || "${ECR_REPO}" == "None" ]]; then
  echo "[deploy] ERROR: ECR repository '${PROJECT}' not found in ${AWS_REGION}."
  echo "         Run 'terraform apply' first to create the registry."
  exit 1
fi

echo "[deploy] Environment : ${ENVIRONMENT}"
echo "[deploy] Image tag   : ${IMAGE_TAG}"
echo "[deploy] ECR repo    : ${ECR_REPO}"

# Authenticate Docker to ECR
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REPO}"

# Build and push frontend image
echo "[deploy] Building + pushing frontend..."
docker build -t "${ECR_REPO}:frontend-${IMAGE_TAG}" -f frontend/Dockerfile frontend/
docker push "${ECR_REPO}:frontend-${IMAGE_TAG}"

# Build and push backend image
echo "[deploy] Building + pushing backend..."
docker build -t "${ECR_REPO}:backend-${IMAGE_TAG}" -f backend/Dockerfile backend/
docker push "${ECR_REPO}:backend-${IMAGE_TAG}"

# Force new ECS deployment (ECS pulls the latest task definition; CI/CD
# should update the task definition image tag via Terraform or aws ecs
# register-task-definition before calling this script)
echo "[deploy] Triggering ECS rolling deploy..."
aws ecs update-service \
  --cluster "${PROJECT}-cluster" \
  --service "${PROJECT}-frontend" \
  --force-new-deployment \
  --region "${AWS_REGION}" > /dev/null

aws ecs update-service \
  --cluster "${PROJECT}-cluster" \
  --service "${PROJECT}-backend" \
  --force-new-deployment \
  --region "${AWS_REGION}" > /dev/null

# Wait for services to stabilise
echo "[deploy] Waiting for frontend service to stabilise..."
aws ecs wait services-stable \
  --cluster "${PROJECT}-cluster" \
  --services "${PROJECT}-frontend" \
  --region "${AWS_REGION}"

echo "[deploy] Waiting for backend service to stabilise..."
aws ecs wait services-stable \
  --cluster "${PROJECT}-cluster" \
  --services "${PROJECT}-backend" \
  --region "${AWS_REGION}"

ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names "${PROJECT}-alb" \
  --query 'LoadBalancers[0].DNSName' \
  --output text \
  --region "${AWS_REGION}")

echo ""
echo "[deploy] Done!  App: http://${ALB_DNS}"
