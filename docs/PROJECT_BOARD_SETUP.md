# GitHub Project Board Setup Guide

## Overview
Project boards provide visual workflow management for tracking issues, PRs, and tasks across Teams 1-5.

## Board Creation

### 1. Create New Project

1. Navigate to repository → **Projects** tab
2. Click **New project**
3. Select **Board** template
4. Name: `CommunityBoard Sprint Board`
5. Click **Create**

### 2. Configure Board Views

**Default Views:**
- **Board View** - Kanban columns
- **Table View** - Spreadsheet format
- **Roadmap View** - Timeline visualization

## Column Structure

### Kanban Board Columns

| Column | Purpose | Automation |
|--------|---------|------------|
| **Backlog** | Unrefined items | New issues auto-added |
| **Ready** | Refined & estimated | Manual move |
| **In Progress** | Active work | Auto when PR opened |
| **In Review** | Code review | Auto when PR ready |
| **Testing** | QA validation | Manual move |
| **Done** | Completed | Auto when PR merged |

### Setup Columns

1. Click **+ Add column**
2. Create each column above
3. Set column limits (WIP limits):
   - In Progress: 10
   - In Review: 8
   - Testing: 5

## Automation Rules

### 1. Auto-add Issues

**Trigger:** Issue created
**Action:** Add to Backlog

```
When: Issue opened
Then: Add to project → Backlog column
```

### 2. Move to In Progress

**Trigger:** PR opened or issue assigned
**Action:** Move to In Progress

```
When: Pull request opened
Then: Move linked issues → In Progress
```

### 3. Move to In Review

**Trigger:** PR ready for review
**Action:** Move to In Review

```
When: Pull request marked ready for review
Then: Move linked issues → In Review
```

### 4. Move to Done

**Trigger:** PR merged
**Action:** Move to Done and close

```
When: Pull request merged
Then: Move linked issues → Done
And: Close issue
```

## Custom Fields

Add these fields to track metadata:

| Field | Type | Values |
|-------|------|--------|
| **Team** | Single select | Backend, Frontend, QA, Data, DevOps |
| **Priority** | Single select | P0 (Critical), P1 (High), P2 (Medium), P3 (Low) |
| **Sprint** | Single select | Sprint 1, Sprint 2, ... |
| **Story Points** | Number | 1, 2, 3, 5, 8, 13 |
| **Component** | Multi select | Auth, Posts, Comments, UI, API |
| **Status** | Status | Backlog, Ready, In Progress, Review, Testing, Done |

### Add Custom Fields

1. Click **⋮** menu → **Settings**
2. Scroll to **Custom fields**
3. Click **+ New field**
4. Configure each field above

## Labels

Create repository labels:

```bash
# Priority
P0-critical (red)
P1-high (orange)
P2-medium (yellow)
P3-low (green)

# Type
bug (red)
feature (blue)
enhancement (purple)
documentation (gray)
technical-debt (brown)

# Component
backend (blue)
frontend (cyan)
data-engineering (green)
qa (yellow)
devops (orange)

# Status
blocked (red)
needs-refinement (yellow)
ready-for-dev (green)
```

## Filters & Views

### Create Sprint View

1. Click **+ New view**
2. Name: `Current Sprint`
3. Filter: `Sprint = "Sprint 1" AND Status != "Done"`
4. Group by: **Team**
5. Sort by: **Priority**

### Create Team Views

**Backend Team View:**
```
Filter: Team = "Backend" AND Status != "Done"
Group by: Status
Sort by: Priority
```

**Frontend Team View:**
```
Filter: Team = "Frontend" AND Status != "Done"
Group by: Status
Sort by: Priority
```

### Create Priority View

```
Filter: Priority IN ["P0-critical", "P1-high"]
Group by: Team
Sort by: Priority, Story Points
```

## Sprint Planning

### Sprint Setup

1. Create milestone: `Sprint 1 (Jan 1-14)`
2. Set sprint dates
3. Add sprint goal in description
4. Link to project board

### Planning Process

1. **Backlog Refinement** (Weekly)
   - Review Backlog items
   - Add acceptance criteria
   - Estimate story points
   - Move to Ready

2. **Sprint Planning** (Bi-weekly)
   - Select items from Ready
   - Assign to team members
   - Set Sprint field
   - Move to In Progress when started

3. **Daily Standup** (Daily)
   - Review board
   - Update item status
   - Flag blockers

4. **Sprint Review** (End of sprint)
   - Demo completed items
   - Move unfinished to next sprint

5. **Retrospective** (End of sprint)
   - Discuss improvements
   - Create action items

## Issue Templates Integration

Link issues to board automatically:

```yaml
# .github/ISSUE_TEMPLATE/feature_request.md
---
name: Feature Request
about: Suggest a new feature
labels: feature, needs-refinement
projects: CommunityBoard Sprint Board
---
```

## Metrics & Reporting

### Velocity Tracking

Track story points completed per sprint:

```
Sprint 1: 45 points
Sprint 2: 52 points
Sprint 3: 48 points
Average: 48 points
```

### Burndown Chart

1. Create **Roadmap view**
2. Set iteration dates
3. Track progress daily

### Team Capacity

```
Backend: 3 developers × 8 points/day = 24 points/sprint
Frontend: 2 developers × 8 points/day = 16 points/sprint
QA: 1 tester × 6 points/day = 12 points/sprint
Total: 52 points/sprint
```

## GitHub CLI Setup

Automate board management:

```bash
# Install GitHub CLI
brew install gh

# Create project
gh project create --owner AmaliTech --title "CommunityBoard Sprint Board"

# Add issue to project
gh project item-add PROJECT_ID --owner AmaliTech --url ISSUE_URL

# List projects
gh project list --owner AmaliTech
```

## Best Practices

1. **Keep board updated** - Update status daily
2. **Link PRs to issues** - Use "Closes #123" in PR description
3. **Set WIP limits** - Prevent bottlenecks
4. **Regular grooming** - Weekly backlog refinement
5. **Clear acceptance criteria** - Define "done"
6. **Estimate consistently** - Use planning poker
7. **Track blockers** - Use "blocked" label
8. **Archive old sprints** - Keep board clean

## Team Responsibilities

### Product Owner
- Prioritize backlog
- Define acceptance criteria
- Approve completed work

### Scrum Master
- Facilitate ceremonies
- Remove blockers
- Update board metrics

### Developers
- Update item status
- Link PRs to issues
- Estimate story points

### QA
- Move items to Testing
- Verify acceptance criteria
- Report bugs

## Notifications

Configure notifications:

1. **Settings** → **Notifications**
2. Enable:
   - Item assigned to you
   - Item moved to your column
   - Mentions in comments
   - Sprint deadline approaching

## Integration with CI/CD

Auto-update board from pipeline:

```yaml
# .github/workflows/update-board.yml
- name: Update project board
  uses: alex-page/github-project-automation-plus@v0.8.3
  with:
    project: CommunityBoard Sprint Board
    column: In Progress
    repo-token: ${{ secrets.GITHUB_TOKEN }}
```

## Troubleshooting

### Items not auto-moving

Check automation settings:
1. Project → **⋮** → **Workflows**
2. Verify triggers enabled
3. Test with sample issue

### Missing custom fields

Re-add fields:
1. Project → **Settings**
2. **Custom fields** → **+ New field**

### Slow board loading

Archive completed items:
1. Filter: `Status = "Done" AND Updated < 30 days ago`
2. Bulk archive

## Quick Reference

**Keyboard Shortcuts:**
- `c` - Create issue
- `e` - Edit item
- `x` - Archive item
- `/` - Search
- `?` - Show shortcuts

**Common Filters:**
```
is:open is:issue assignee:@me
is:pr is:open review-requested:@me
label:bug priority:P0-critical
sprint:"Sprint 1" team:Backend
```

## Support

Project board issues:
- Slack: #project-management
- Email: scrum-master@amalitech.com
- GitHub Discussions
