# Environment Variables Setup Guide

## Overview
Environment variables store configuration and secrets outside of code. Never commit actual `.env` files to git.

## Setup Instructions

### 1. Copy Example Files

```bash
# Backend
cp backend/.env.example backend/.env

# Frontend
cp frontend/.env.example frontend/.env

# Data Engineering
cp data-engineering/.env.example data-engineering/.env
```

### 2. Generate Secure Values

**JWT Secret (256-bit minimum):**
```bash
openssl rand -base64 32
# Output: Use this as JWT_SECRET
```

**Database Password:**
```bash
openssl rand -base64 16
```

### 3. Fill in Values

Edit each `.env` file with actual values:

```bash
# Use your preferred editor
nano backend/.env
nano frontend/.env
nano data-engineering/.env
```

## Environment Files

### Backend (.env)

| Variable | Description | Example | Required |
|----------|-------------|---------|----------|
| `SPRING_DATASOURCE_URL` | Database connection | `jdbc:postgresql://localhost:5432/communityboard` | Yes |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` | Yes |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `secure_password` | Yes |
| `JWT_SECRET` | JWT signing key | `base64-encoded-secret` | Yes |
| `JWT_EXPIRATION` | Token expiry (ms) | `86400000` (24h) | Yes |
| `SERVER_PORT` | Server port | `8080` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev`, `prod` | Yes |
| `CORS_ALLOWED_ORIGINS` | CORS origins | `http://localhost:3000` | Yes |
| `SPRING_MAIL_HOST` | SMTP host | `localhost` or `mailpit` | Yes for email features |
| `SPRING_MAIL_PORT` | SMTP port | `1025` | Yes for email features |
| `SPRING_MAIL_USERNAME` | SMTP username | `mailer@example.com` | No |
| `SPRING_MAIL_PASSWORD` | SMTP password | `app-password` | No |
| `EMAIL_VERIFICATION_ENABLED` | Require email verification after signup | `true`, `false` | No |
| `EMAIL_VERIFICATION_FROM` | Sender for verification emails | `no-reply@communityboard.local` | No |
| `EMAIL_VERIFICATION_URL` | Frontend verification page URL | `http://localhost:3000/verify-email` | No |
| `EMAIL_NOTIFICATIONS_ENABLED` | Enable category/new-post emails | `true`, `false` | No |
| `EMAIL_NOTIFICATIONS_FROM` | Sender for notification emails | `no-reply@communityboard.local` | No |
| `FRONTEND_BASE_URL` | Base frontend URL for email links | `http://localhost:3000` | No |

### Frontend (.env)

| Variable | Description | Example | Required |
|----------|-------------|---------|----------|
| `REACT_APP_API_BASE_URL` | Backend API URL | `http://localhost:8080/api/v1` | Yes |
| `REACT_APP_API_TIMEOUT` | Request timeout | `30000` (30s) | No |
| `REACT_APP_ENV` | Environment | `development`, `production` | Yes |
| `REACT_APP_ENABLE_ANALYTICS` | Enable analytics | `true`, `false` | No |

### Data Engineering (.env)

| Variable | Description | Example | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | Database host | `localhost` | Yes |
| `DB_PORT` | Database port | `5432` | Yes |
| `DB_NAME` | Database name | `communityboard` | Yes |
| `DB_USER` | Database user | `postgres` | Yes |
| `DB_PASSWORD` | Database password | `secure_password` | Yes |
| `ETL_BATCH_SIZE` | Batch size | `1000` | No |

## Docker Compose Integration

Mail catcher for local development:

```bash
# Mailpit web UI
http://localhost:8025
```

Update `docker-compose.yml` to use environment files:

```yaml
services:
  backend:
    build: ./backend
    env_file:
      - ./backend/.env
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/communityboard
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    env_file:
      - ./frontend/.env
```

## Security Best Practices

### 1. Never Commit Secrets

Add to `.gitignore`:
```
# Environment files
.env
.env.local
.env.*.local
**/.env
```

### 2. Use Different Values Per Environment

```
.env.development
.env.staging
.env.production
```

### 3. Rotate Secrets Regularly

- JWT secrets: Every 90 days
- Database passwords: Every 180 days
- API keys: Per provider policy

### 4. Use Secret Management Tools

**Production:**
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault
- Google Secret Manager

**Example with AWS Secrets Manager:**
```bash
aws secretsmanager get-secret-value \
  --secret-id communityboard/prod/jwt-secret \
  --query SecretString \
  --output text
```

## CI/CD Integration

### GitHub Secrets

Add secrets in GitHub:
1. Go to Settings → Secrets and variables → Actions
2. Add repository secrets:
   - `DB_PASSWORD`
   - `JWT_SECRET`
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`

### Use in Workflows

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        env:
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: ./deploy.sh
```

## Validation

### Check Required Variables

Create `scripts/check-env.sh`:

```bash
#!/bin/bash

check_var() {
  if [ -z "${!1}" ]; then
    echo "❌ Missing: $1"
    exit 1
  fi
  echo "✅ Found: $1"
}

# Backend
check_var "SPRING_DATASOURCE_URL"
check_var "JWT_SECRET"

# Frontend
check_var "REACT_APP_API_BASE_URL"

echo "✅ All required variables set"
```

Run before starting:
```bash
chmod +x scripts/check-env.sh
./scripts/check-env.sh
```

## Troubleshooting

### Variables Not Loading

**Backend:**
```bash
# Check if Spring Boot reads .env
java -jar app.jar --spring.config.additional-location=file:.env
```

**Frontend:**
```bash
# Variables must start with REACT_APP_
# Restart dev server after changes
npm start
```

**Docker:**
```bash
# Check container environment
docker exec backend env | grep SPRING
```

### Wrong Values

```bash
# Print all env vars (be careful with secrets)
printenv | grep REACT_APP

# Check specific variable
echo $SPRING_DATASOURCE_URL
```

## Environment-Specific Configuration

### Development
```bash
# backend/.env.development
SPRING_PROFILES_ACTIVE=dev
LOGGING_LEVEL_ROOT=DEBUG
```

### Staging
```bash
# backend/.env.staging
SPRING_PROFILES_ACTIVE=staging
LOGGING_LEVEL_ROOT=INFO
```

### Production
```bash
# backend/.env.production
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_ROOT=WARN
```

## Loading Environment Files

### Backend (Spring Boot)

Add to `application.yml`:
```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

Or use `spring-dotenv`:
```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

### Frontend (React)

Automatically loaded by Create React App. Access via:
```javascript
const apiUrl = process.env.REACT_APP_API_BASE_URL;
```

### Data Engineering (Python)

Use `python-dotenv`:
```python
from dotenv import load_dotenv
import os

load_dotenv()

db_host = os.getenv('DB_HOST')
```

## Quick Start Checklist

- [ ] Copy all `.env.example` files to `.env`
- [ ] Generate secure JWT secret
- [ ] Set database credentials
- [ ] Configure API URLs
- [ ] Verify `.env` in `.gitignore`
- [ ] Test application startup
- [ ] Add secrets to CI/CD
- [ ] Document custom variables
- [ ] Set up secret rotation schedule

## Support

Issues with environment setup:
- Slack: #devops-support
- Email: devops@amalitech.com
- Wiki: Environment Configuration Guide
