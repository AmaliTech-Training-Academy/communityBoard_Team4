# Auto-Assign Reviewers

## Overview
Automatically assigns reviewers to pull requests based on files changed and team ownership.

## How It Works

When a PR is opened or marked ready for review:
1. Analyzes files changed in the PR
2. Matches file patterns to team members
3. Assigns 2 reviewers automatically
4. Respects CODEOWNERS file

## Configuration

### File: `.github/auto-assign.yml`

**Settings:**
- `numberOfReviewers: 2` - Assigns 2 reviewers per PR
- `skipDraftPR: true` - Skips draft PRs
- `addReviewers: true` - Adds reviewers
- `addAssignees: false` - Doesn't auto-assign PR author

### File Pattern Assignments

| Pattern | Reviewers |
|---------|-----------|
| `backend/**` | backend-lead, java-expert |
| `frontend/**` | frontend-lead, react-expert |
| `devops/**`, `.github/**` | devops-lead |
| `qa/**` | qa-lead |
| `data-engineering/**` | data-lead, python-expert |
| `**/*.md`, `docs/**` | tech-writer |
| `**/security/**`, `**/.env*` | security-lead, devops-lead |

## Team Mapping

Update these GitHub usernames in `.github/auto-assign.yml`:

```yaml
reviewers:
  - backend-lead      # Replace with actual GitHub username
  - frontend-lead     # Replace with actual GitHub username
  - devops-lead       # Replace with actual GitHub username
  - qa-lead           # Replace with actual GitHub username
  - data-lead         # Replace with actual GitHub username
  - java-expert       # Replace with actual GitHub username
  - react-expert      # Replace with actual GitHub username
  - python-expert     # Replace with actual GitHub username
  - security-lead     # Replace with actual GitHub username
  - tech-writer       # Replace with actual GitHub username
```

## Examples

### Backend PR
**Files changed:** `backend/src/main/java/Controller.java`
**Auto-assigned:** backend-lead, java-expert

### Frontend PR
**Files changed:** `frontend/src/components/Header.jsx`
**Auto-assigned:** frontend-lead, react-expert

### DevOps PR
**Files changed:** `.github/workflows/ci.yml`, `docker-compose.yml`
**Auto-assigned:** devops-lead, (1 random from pool)

### Multi-component PR
**Files changed:** `backend/Controller.java`, `frontend/Header.jsx`
**Auto-assigned:** backend-lead, frontend-lead

### Security PR
**Files changed:** `backend/src/security/JwtFilter.java`
**Auto-assigned:** security-lead, backend-lead

## Manual Override

You can still manually add/remove reviewers after auto-assignment.

## Integration with CODEOWNERS

Auto-assign works alongside CODEOWNERS:
- CODEOWNERS: Required approvals
- Auto-assign: Suggested reviewers

Both can assign the same people for critical files.

## Troubleshooting

### Reviewers not assigned

**Check:**
1. GitHub usernames are correct
2. Users have repository access
3. PR is not in draft mode
4. Workflow has proper permissions

### Wrong reviewers assigned

**Fix:**
1. Update file patterns in `.github/auto-assign.yml`
2. Ensure patterns are specific enough
3. Order matters - first match wins

### Too many/few reviewers

**Adjust:**
```yaml
numberOfReviewers: 3  # Change to desired number
```

## Permissions

Workflow needs `pull-requests: write` permission:

```yaml
permissions:
  pull-requests: write
```

## Disable Auto-Assign

To skip auto-assignment for a specific PR:
1. Create PR as draft
2. Mark ready for review after manual reviewer selection

Or add label `skip-auto-assign` and update workflow:

```yaml
on:
  pull_request:
    types: [opened, ready_for_review]

jobs:
  auto-assign:
    if: "!contains(github.event.pull_request.labels.*.name, 'skip-auto-assign')"
```

## Best Practices

1. **Keep reviewer pool updated** - Add/remove as team changes
2. **Balance load** - Rotate reviewers fairly
3. **Respect expertise** - Assign domain experts
4. **Document exceptions** - Note why manual assignment needed
5. **Monitor metrics** - Track review turnaround time

## Metrics to Track

- Average time to first review
- Review distribution across team
- PRs requiring manual reassignment
- Review quality by reviewer

## Alternative: GitHub Teams

For larger organizations, use GitHub Teams:

```yaml
reviewers:
  - backend-team
  - frontend-team
  - devops-team
```

Then manage team membership in GitHub Settings.

## Support

Issues with auto-assignment:
- Check workflow logs in Actions tab
- Verify GitHub usernames
- Ensure proper permissions
- Contact devops@amalitech.com
