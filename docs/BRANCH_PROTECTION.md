# Branch Protection Rules Setup

## Overview
Branch protection rules ensure code quality by requiring reviews and CI checks before merging.

## GitHub Repository Settings

### 1. Navigate to Branch Protection
1. Go to repository **Settings** → **Branches**
2. Click **Add rule** or edit existing rule for `main` branch

### 2. Configure Protection Rules

#### Required Settings:
- **Branch name pattern**: `main`
- ✅ **Require a pull request before merging**
  - ✅ **Require approvals**: 1 (minimum)
  - ✅ **Dismiss stale PR approvals when new commits are pushed**
  - ✅ **Require review from code owners** (if CODEOWNERS file exists)
- ✅ **Require status checks to pass before merging**
  - ✅ **Require branches to be up to date before merging**
  - **Status checks**: Select CI workflows (e.g., "CI", "Build", "Test")
- ✅ **Require conversation resolution before merging**
- ✅ **Restrict pushes that create files larger than 100 MB**

#### Optional Settings:
- ✅ **Require signed commits**
- ✅ **Include administrators** (applies rules to admins too)
- ✅ **Allow force pushes** (❌ recommended to disable)
- ✅ **Allow deletions** (❌ recommended to disable)

### 3. Additional Branch Rules
Create similar rules for:
- `develop` branch (if using GitFlow)
- `release/*` branches
- `hotfix/*` branches

## CODEOWNERS File
Create `.github/CODEOWNERS`:
```
# Global owners
* @team-lead @senior-dev

# Backend code
/backend/ @backend-team @team-lead

# Frontend code
/frontend/ @frontend-team @team-lead

# DevOps and CI
/devops/ @devops-team
/.github/ @devops-team
/docker-compose.yml @devops-team

# Documentation
/docs/ @team-lead
README.md @team-lead
```

## Enforcement
- All commits to `main` must go through pull requests
- PRs require at least 1 approval from code owners
- All CI checks must pass (build, test, lint)
- Conversations must be resolved before merge

## Benefits
- Prevents direct pushes to main branch
- Ensures code review process
- Maintains CI/CD pipeline integrity
- Reduces bugs in production code
