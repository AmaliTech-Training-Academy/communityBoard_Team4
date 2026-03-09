# Contributing to CommunityBoard

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Git
- Python 3.8+ (for pre-commit hooks)

### Setup Development Environment
```bash
# 1. Clone repository
git clone https://github.com/AmaliTech-Training-Academy/communityBoard_Team4.git
cd communityBoard_Team4

# 2. Setup pre-commit hooks (REQUIRED)
python3 -m venv venv
source venv/bin/activate  # Linux/macOS
venv\Scripts\activate     # Windows
pip install pre-commit detect-secrets
bash setup-precommit.sh

# 3. Start services
docker-compose up --build
```

## Development Workflow

### 1. Branch Strategy
- `main` - Production-ready code
- `develop` - Integration branch
- `feature/[issue-number]-[description]` - New features
- `bugfix/[issue-number]-[description]` - Bug fixes
- `hotfix/[issue-number]-[description]` - Critical fixes

### 2. Create a Feature Branch Safely

Developers should always branch from `develop` to ensure they have the latest integration changes:

```bash
# 1. Clone the repo
git clone https://github.com/AmaliTech-Training-Academy/communityBoard_Team4.git

# 2. Navigate inside the repo
cd communityBoard_Team4/

# 3. Switch to develop branch
git checkout develop

# 4. Pull latest changes
git pull origin develop

# 5. Create and switch to new feature branch
git checkout -b feature/[issue-number]-[description]

# Example:
git checkout -b feature/24-payment-endpoint
```

### 3. Commit with Issue References

You can optionally reference the issue in the commit message:

```bash
git commit -m "Add payment endpoint (#24)"
```

This step is optional but helps with tracking and automatically links commits to issues.

### 4. Commit Guidelines

**IMPORTANT**: Pre-commit hooks will automatically run before each commit to:
- ✅ Format code (Java, JavaScript, Python)
- ✅ Run linters (ESLint, Flake8)
- ✅ Check for secrets/credentials
- ✅ Validate commit message format
- ✅ Remove trailing whitespace
- ✅ Prevent direct commits to main/develop

Follow [Conventional Commits](https://www.conventionalcommits.org/):
```bash
# Format: type(scope): description

feat(auth): add JWT token refresh
fix(posts): resolve null pointer exception
docs(readme): update setup instructions
style(frontend): fix linting issues
refactor(backend): optimize database queries
test(api): add integration tests for posts
chore(deps): update dependencies
```

**Valid types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**If hooks fail**:
```bash
# 1. Review the errors
# 2. Fix the issues (some auto-fix)
# 3. Stage fixes: git add .
# 4. Commit again

# Emergency bypass (requires approval)
git commit --no-verify -m "hotfix: critical bug"
```

See [Pre-commit Hooks Documentation](docs/PRE_COMMIT_HOOKS.md) for details.



## Coding Standards

### Backend (Java/Spring Boot)
- **Style**: Google Java Style Guide
- **Naming**: camelCase for variables/methods, PascalCase for classes
- **Packages**: `com.amalitech.communityboard.[module]`
- **Testing**: JUnit 5, minimum 80% coverage
- **Documentation**: JavaDoc for public APIs

```java
@RestController
@RequestMapping("/api/posts")
@Validated
public class PostController {

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable @Positive Long id) {
        // Implementation
    }
}
```

### Frontend (React)
- **Style**: Airbnb JavaScript Style Guide
- **Components**: PascalCase, functional components with hooks
- **Files**: kebab-case for files, PascalCase for components
- **State**: Use Context API or Redux for global state
- **Testing**: Jest + React Testing Library

```jsx
const PostCard = ({ post, onEdit, onDelete }) => {
  const [isLoading, setIsLoading] = useState(false);

  return (
    <div className="post-card">
      {/* Component content */}
    </div>
  );
};
```

### Database
- **Tables**: snake_case naming
- **Migrations**: Flyway with versioned SQL files
- **Indexes**: Add for foreign keys and search columns

### API Design
- **REST**: Follow RESTful principles
- **Endpoints**: `/api/v1/[resource]`
- **HTTP Status**: Use appropriate status codes
- **Pagination**: Use `page`, `size`, `sort` parameters
- **Validation**: Validate all inputs

## Code Quality

### Pre-commit Hooks (REQUIRED)

All developers MUST install pre-commit hooks before making their first commit.

**Quick Setup**:
```bash
# In project root with venv activated
bash setup-precommit.sh
```

**What gets checked**:
- **All files**: Trailing whitespace, file endings, YAML/JSON syntax, no large files, no merge conflicts, no secrets
- **Frontend**: ESLint auto-fixes JavaScript/React code
- **Backend**: Java code formatting (when enabled)
- **Data Engineering**: Black formatting, Flake8 linting
- **Commit messages**: Conventional commit format validation

**Manual testing**:
```bash
# Test all hooks
pre-commit run --all-files

# Test specific hook
pre-commit run eslint --all-files
```

**Troubleshooting**:
- See [Pre-commit Hooks Documentation](docs/PRE_COMMIT_HOOKS.md)
- Contact DevOps team (@JoelAlumasa) for issues

### Linting & Formatting
- **Backend**: Checkstyle, SpotBugs
- **Frontend**: ESLint, Prettier
- **Auto-format**: Configure IDE to format on save

### Testing Requirements
- **Unit Tests**: All new code must have tests
- **Integration Tests**: API endpoints and database operations
- **E2E Tests**: Critical user flows
- **Coverage**: Minimum 80% for new code

## Pull Request Process

### 1. Before Creating PR
- [ ] Pre-commit hooks installed and passing
- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No merge conflicts
- [ ] Commit messages follow conventional format

### 2. PR Requirements
- **Title**: Clear, descriptive title
- **Description**: Link to issue, describe changes
- **Size**: Keep PRs small (< 400 lines changed)
- **Reviews**: At least 1 approval required

### 3. PR Template
```markdown
## Changes
- Brief description of changes

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
```

## Issue Guidelines

### Bug Reports
- Use bug report template
- Include reproduction steps
- Provide environment details
- Add relevant logs/screenshots

### Feature Requests
- Use feature request template
- Describe problem and solution
- Include acceptance criteria
- Consider technical impact

## Security

### Reporting Vulnerabilities
- Email: security@amalitech.com
- Do not create public issues for security bugs
- Include detailed reproduction steps

### Security Guidelines
- **Never commit secrets/credentials** - Pre-commit hooks will block commits with detected secrets
- Use environment variables for config (see `.env.example` files)
- Validate all user inputs
- Follow OWASP guidelines
- If you accidentally commit a secret, contact security@amalitech.com immediately

## Documentation

### Code Documentation
- JavaDoc for public APIs
- JSDoc for complex functions
- README files for modules
- API documentation in Swagger

### Architecture Decisions
- Document significant decisions
- Use ADR (Architecture Decision Records)
- Update diagrams when needed

## Getting Help

- **Discussions**: Use GitHub Discussions for questions
- **Issues**: Create issues for bugs/features
- **Code Review**: Tag team members for reviews
- **Slack**: #communityboard-team4 channel

## Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- Team meetings
