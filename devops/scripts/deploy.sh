#!/bin/bash
# Deployment script for CommunityBoard
# Usage: ./deploy.sh [environment]

set -e

ENVIRONMENT=${1:-development}
echo "Deploying CommunityBoard to $ENVIRONMENT..."

# Build and start containers
docker-compose down
docker-compose build --no-cache
docker-compose up -d

echo "Waiting for services to start..."
sleep 10

# Health check
curl -f http://localhost:8080/api-docs || echo "Backend health check failed!"
echo ""
echo "Deployment complete!"
echo "Backend:  http://localhost:8080/swagger-ui.html"
echo "Frontend: http://localhost:3000"
