# Pre-commit Hooks Setup Guide

## Quick Setup

### 1. Install pre-commit (one-time setup)

```bash
# Using pip in virtual environment (recommended)
python3 -m venv venv
source venv/bin/activate  # Linux/macOS
venv\Scripts\activate     # Windows
pip install pre-commit

# OR using pipx (system-wide)
pipx install pre-commit

# Verify installation
pre-commit --version
```

### 2. Install hooks in repository

```bash
cd communityBoard_Team4

# Run setup script (easiest)
bash setup-precommit.sh

# OR manually
pre-commit install
pre-commit install --hook-type commit-msg
```

## What Gets Checked

### All Files
- ✅ Trailing whitespace removed
- ✅ Files end with newline
- ✅ YAML/JSON syntax valid
- ✅ No files > 500KB
- ✅ No merge conflicts
- ✅ No private keys/secrets
- ✅ No direct commits to main/develop

### Frontend (JavaScript/React)
- ✅ ESLint checks and auto-fixes
- ✅ Code style consistency

### Backend (Java)
- ✅ Java code formatting
- ✅ Code style consistency

### Data Engineering (Python)
- ✅ Black formatting (88 char line length)
- ✅ Flake8 linting

### Commit Messages
- ✅ Must follow format: `type(scope): description`
- ✅ Valid types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- ✅ Examples:
  - `feat(auth): add JWT authentication`
  - `fix(api): resolve null pointer exception`
  - `docs(readme): update installation steps`

## Configuration

Create `.pre-commit-config.yaml` in project root:

```yaml
repos:
  # General file checks
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json
      - id: check-added-large-files
        args: ['--maxkb=500']
      - id: check-merge-conflict
      - id: detect-private-key
      - id: no-commit-to-branch
        args: ['--branch', 'main', '--branch', 'develop']

  # Java formatting
  - repo: https://github.com/macisamuele/language-formatters-pre-commit-hooks
    rev: v2.12.0
    hooks:
      - id: pretty-format-java
        args: [--autofix]

  # Python formatting (data-engineering)
  - repo: https://github.com/psf/black
    rev: 24.1.1
    hooks:
      - id: black
        files: ^data-engineering/

  - repo: https://github.com/pycqa/flake8
    rev: 7.0.0
    hooks:
      - id: flake8
        files: ^data-engineering/
        args: ['--max-line-length=88']

  # JavaScript/React formatting
  - repo: https://github.com/pre-commit/mirrors-eslint
    rev: v8.56.0
    hooks:
      - id: eslint
        files: ^frontend/src/.*\.[jt]sx?$
        types: [file]
        additional_dependencies:
          - eslint@8.56.0
          - eslint-config-react-app@7.0.1

  # Commit message linting
  - repo: https://github.com/compilerla/conventional-pre-commit
    rev: v3.0.0
    hooks:
      - id: conventional-pre-commit
        stages: [commit-msg]
        args: [feat, fix, docs, style, refactor, test, chore]

  # Secret detection
  - repo: https://github.com/Yelp/detect-secrets
    rev: v1.4.0
    hooks:
      - id: detect-secrets
        args: ['--baseline', '.secrets.baseline']
```

## Setup Scripts

### Backend (Java)

Create `backend/.pre-commit-config.yaml`:

```yaml
repos:
  - repo: local
    hooks:
      - id: maven-test
        name: Maven Test
        entry: mvn test -DskipTests=false
        language: system
        pass_filenames: false
        files: ^backend/.*\.java$

      - id: maven-checkstyle
        name: Maven Checkstyle
        entry: mvn checkstyle:check
        language: system
        pass_filenames: false
        files: ^backend/.*\.java$
```

### Frontend (React)

Create `frontend/.pre-commit-config.yaml`:

```yaml
repos:
  - repo: local
    hooks:
      - id: npm-lint
        name: ESLint
        entry: npm run lint
        language: system
        pass_filenames: false
        files: ^frontend/src/.*\.[jt]sx?$

      - id: npm-test
        name: Jest Tests
        entry: npm test -- --watchAll=false --passWithNoTests
        language: system
        pass_filenames: false
        files: ^frontend/src/.*\.[jt]sx?$
```

## Usage

### Normal Workflow (Automatic)

```bash
# 1. Make changes
vim src/file.js

# 2. Stage changes
git add .

# 3. Commit (hooks run automatically)
git commit -m "feat(api): add user endpoint"

# If hooks fail:
# - Review the errors
# - Fix the issues (some auto-fix)
# - Stage fixes: git add .
# - Commit again
```

### Manual Testing

```bash
# Test all hooks on all files
pre-commit run --all-files

# Test specific hook
pre-commit run eslint --all-files
pre-commit run trailing-whitespace --all-files

# Test only staged files
pre-commit run
```

### Emergency Bypass (Use Sparingly)

```bash
# Skip ALL hooks (not recommended)
git commit --no-verify -m "hotfix: critical production bug"

# Skip specific hook
SKIP=eslint git commit -m "fix: temporary bypass"
```

## Configuration Files

The repository includes:
- `.pre-commit-config.yaml` - Main hook configuration
- `.secrets.baseline` - Known secrets to ignore
- `setup-precommit.sh` - Automated setup script

## Troubleshooting

### "pre-commit: command not found"

```bash
# Activate virtual environment
source venv/bin/activate

# OR install with pipx
pipx install pre-commit
```

### "Hook failed" - ESLint errors

```bash
# Auto-fix most issues
cd frontend
npm run lint:fix
git add .
git commit -m "..."
```

### "Hook failed" - Secret detected

```bash
# If false positive, update baseline
detect-secrets scan > .secrets.baseline
detect-secrets audit .secrets.baseline
# Mark false positives, then commit
```

### "Hook failed" - Commit message format

```bash
# Wrong: "added new feature"
# Right: "feat(api): add user endpoint"

# Valid formats:
feat(scope): description
fix(scope): description
docs: description
test(scope): description
```

### Hooks running too slow

```bash
# Update hooks
pre-commit autoupdate

# Clear cache
pre-commit clean

# Temporarily skip slow hooks
SKIP=maven-test git commit -m "..."
```

### Reset hooks completely

```bash
pre-commit uninstall
pre-commit clean
pre-commit install
pre-commit install --hook-type commit-msg
```



## Team Onboarding

### New Developer Setup (5 minutes)

```bash
# 1. Clone repository
git clone https://github.com/AmaliTech/communityBoard_Team4.git
cd communityBoard_Team4

# 2. Create virtual environment
python3 -m venv venv
source venv/bin/activate

# 3. Install pre-commit
pip install pre-commit

# 4. Run setup script
bash setup-precommit.sh

# 5. Test it works
echo "test" >> test.txt
git add test.txt
git commit -m "test: verify pre-commit works"
# Should see hooks running

git reset HEAD~1  # Undo test commit
rm test.txt
```

### Add to Team Documentation

**Required for all developers:**
- Pre-commit hooks MUST be installed before first commit
- Run `bash setup-precommit.sh` during onboarding
- Never use `--no-verify` without team lead approval
- Commit messages MUST follow conventional format

**Team Leads:**
- Verify new developers have hooks installed
- Review bypass requests in PRs
- Update hooks monthly: `pre-commit autoupdate`



## Maintenance

### Monthly Updates (DevOps)

```bash
# Update all hooks to latest versions
pre-commit autoupdate

# Test updates
pre-commit run --all-files

# Commit updates
git add .pre-commit-config.yaml
git commit -m "chore(devops): update pre-commit hooks"
```

### Clean Cache

```bash
# Clear pre-commit cache
pre-commit clean

# Validate configuration
pre-commit validate-config
```

## FAQ

**Q: Do I need to install pre-commit for every project?**
A: No, install once with `pipx install pre-commit`, then run `pre-commit install` in each repo.

**Q: Can I commit without running hooks?**
A: Yes with `--no-verify`, but only for emergencies. Document reason in commit message.

**Q: What if a hook is broken?**
A: Skip it temporarily: `SKIP=hook-name git commit -m "..."` and report to DevOps team.

**Q: How do I see which hooks will run?**
A: Run `pre-commit run --all-files --verbose`

**Q: Hooks are slow, can I speed them up?**
A: Hooks only run on changed files. If still slow, contact DevOps to optimize.

## Support

- **Issues:** Create GitHub issue with `devops` label
- **Questions:** Ask in #devops Slack channel
- **DevOps Lead:** @JoelAlumasa
