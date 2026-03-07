# Contributing to CommunityBoard

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Git

### Setup Development Environment
```bash
git clone https://github.com/AmaliTech-Training-Academy/communityBoard_Team4.git
cd communityBoard_Team4
docker-compose up --build
```

## Development Workflow

### 1. Branch Strategy
- `main` - Production-ready code
- `develop` - Integration branch
- `feature/[issue-number]-[description]` - New features
- `bugfix/[issue-number]-[description]` - Bug fixes
- `hotfix/[issue-number]-[description]` - Critical fixes

### 2. Commit Guidelines
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
type(scope): description

feat(auth): add JWT token refresh
fix(posts): resolve null pointer exception
docs(readme): update setup instructions
style(frontend): fix linting issues
refactor(backend): optimize database queries
test(api): add integration tests for posts
```

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

### Pre-commit Hooks
Install and configure:
```bash
npm install -g pre-commit
# Hooks run automatically on commit
```

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
- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No merge conflicts

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
- Never commit secrets/credentials
- Use environment variables for config
- Validate all user inputs
- Follow OWASP guidelines

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