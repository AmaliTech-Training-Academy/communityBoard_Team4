# Pull Request Validation Pipeline

## Overview
Automated PR validation ensures code quality, security, and standards compliance before merging.

## Validation Checks

### 1. PR Title Validation
**Format:** `type(scope): Description [TICKET-ID]`

**Examples:**
- ✅ `feat(auth): Add JWT refresh token [CB-123]`
- ✅ `fix(posts): Resolve null pointer exception [CB-456]`
- ❌ `updated code`
- ❌ `Fix bug`

**Allowed Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `style` - Code formatting
- `refactor` - Code restructuring
- `test` - Adding tests
- `chore` - Maintenance

### 2. Branch Naming Validation
**Format:** `prefix/TICKET-ID-description`

**Examples:**
- ✅ `feature/CB-123-add-comments`
- ✅ `bugfix/CB-456-fix-login-error`
- ✅ `hotfix/CB-789-security-patch`
- ❌ `my-feature`
- ❌ `fix-bug`

**Allowed Prefixes:**
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical production fixes
- `release/` - Release branches

### 3. Commit Message Linting
**Format:** `type(scope): description`

**Examples:**
- ✅ `feat(auth): implement refresh token rotation`
- ✅ `fix(ui): resolve button alignment issue`
- ❌ `fixed stuff`
- ❌ `WIP`

### 4. Code Coverage Thresholds
- **Backend (Java):** Minimum 80% line coverage
- **Frontend (React):** Minimum 70% line coverage

**Check locally:**
```bash
# Backend
cd backend && mvn test jacoco:report
open target/site/jacoco/index.html

# Frontend
cd frontend && npm test -- --coverage
open coverage/lcov-report/index.html
```

### 5. Security Vulnerability Scan
**Tools:**
- Trivy (filesystem scan)
- OWASP Dependency Check (backend)
- npm audit (frontend)

**Severity Levels Blocked:**
- CRITICAL
- HIGH

**Check locally:**
```bash
# Install Trivy
brew install trivy

# Run scan
trivy fs .

# Backend dependencies
cd backend && mvn dependency-check:check

# Frontend dependencies
cd frontend && npm audit
```

### 6. Code Quality Checks
**Backend:**
- Checkstyle (Google Java Style Guide)
- No compiler warnings

**Frontend:**
- ESLint (Airbnb style)
- Max 0 warnings

**Check locally:**
```bash
# Backend
cd backend && mvn checkstyle:check

# Frontend
cd frontend && npm run lint
```

## Pipeline Status

All checks must pass before PR can be merged:

| Check | Status | Required |
|-------|--------|----------|
| PR Title | ✅ | Yes |
| Branch Name | ✅ | Yes |
| Commit Messages | ✅ | Yes |
| Backend Tests | ✅ | Yes |
| Backend Coverage (80%) | ✅ | Yes |
| Frontend Tests | ✅ | Yes |
| Frontend Coverage (70%) | ✅ | Yes |
| Security Scan | ✅ | Yes |
| Dependency Check | ✅ | Yes |
| Code Quality | ✅ | Yes |

## Troubleshooting

### PR Title Fails
```bash
# Fix: Update PR title in GitHub UI
# Format: type(scope): Description [TICKET-ID]
```

### Branch Name Fails
```bash
# Fix: Rename branch
git branch -m feature/CB-123-new-feature
git push origin -u feature/CB-123-new-feature
```

### Commit Message Fails
```bash
# Fix: Amend last commit
git commit --amend -m "feat(auth): add JWT refresh token"
git push --force-with-lease

# Fix: Interactive rebase for multiple commits
git rebase -i HEAD~3
# Change 'pick' to 'reword' for commits to fix
```

### Coverage Below Threshold
```bash
# Add more tests
# Run locally to verify
mvn test jacoco:report  # Backend
npm test -- --coverage  # Frontend
```

### Security Vulnerabilities
```bash
# Update dependencies
mvn versions:use-latest-releases  # Backend
npm audit fix  # Frontend

# If false positive, add suppression
# backend/dependency-check-suppressions.xml
```

### Linting Errors
```bash
# Auto-fix
mvn checkstyle:check  # Backend (manual fix)
npm run lint:fix  # Frontend (auto-fix)
```

## Bypassing Checks (Emergency Only)

**Process:**
1. Document reason in PR description
2. Get approval from 2 tech leads
3. Create follow-up ticket to fix issues
4. Admin can override and merge

**Never bypass:**
- Critical security vulnerabilities
- Failing tests

## GitHub Branch Protection Settings

Required status checks:
- `pr-title-check`
- `branch-naming-check`
- `commit-message-lint`
- `backend-tests`
- `frontend-tests`
- `security-scan`
- `dependency-check`
- `code-quality`

## CI/CD Integration

This pipeline runs on:
- Pull request opened
- New commits pushed to PR
- PR title/description edited

**Typical run time:** 8-12 minutes

## Notifications

Failed checks will:
- Block PR merge
- Comment on PR with failure details
- Send notification to PR author
- Update PR status checks

## Best Practices

1. **Run checks locally before pushing**
2. **Keep PRs small** (< 400 lines changed)
3. **Write tests first** (TDD)
4. **Fix linting issues immediately**
5. **Update dependencies regularly**
6. **Never commit secrets**
7. **Squash commits before merge**

## Support

Issues with pipeline:
- Slack: #devops-support
- Email: devops@amalitech.com
- Create issue with label `ci-cd`
