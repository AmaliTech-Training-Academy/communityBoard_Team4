# GitHub Projects Setup (Kanban/Scrum Board)

## Overview
GitHub Projects provides project management capabilities with Kanban boards, automation, and tracking.

## Setup Instructions

### 1. Create New Project
1. Go to repository → **Projects** tab
2. Click **New project**
3. Choose **Board** template
4. Name: "CommunityBoard Development"

### 2. Configure Board Columns

#### Kanban Setup:
- **Backlog** - New issues and ideas
- **Ready** - Issues ready for development
- **In Progress** - Currently being worked on
- **In Review** - Pull requests under review
- **Testing** - Features being tested
- **Done** - Completed items

#### Scrum Setup (Alternative):
- **Product Backlog** - All user stories
- **Sprint Backlog** - Current sprint items
- **In Progress** - Active development
- **Review** - Code review and testing
- **Done** - Sprint completed items

### 3. Custom Fields
Add these fields to track additional information:

- **Priority**: Single select (Low, Medium, High, Critical)
- **Story Points**: Number (1, 2, 3, 5, 8, 13)
- **Component**: Single select (Backend, Frontend, DevOps, QA)
- **Sprint**: Text (Sprint 1, Sprint 2, etc.)
- **Assignee**: Person field
- **Due Date**: Date field

### 4. Automation Rules

#### Auto-move Cards:
```yaml
# When PR is opened
- When: Pull request opened
- Then: Move linked issues to "In Review"

# When PR is merged
- When: Pull request merged
- Then: Move linked issues to "Done"

# When issue is assigned
- When: Issue assigned
- Then: Move to "In Progress"
```

#### Auto-assign Labels:
```yaml
# Priority-based automation
- When: Issue created with "Critical" priority
- Then: Add "urgent" label and assign to team lead

# Component-based automation
- When: Issue affects "Backend" component
- Then: Add "backend" label and assign to backend team
```

### 5. Views and Filters

#### Sprint View:
- Filter: `sprint:"Sprint 1" is:open`
- Group by: Status
- Sort by: Priority

#### Team View:
- Filter: `assignee:@me is:open`
- Group by: Component
- Sort by: Due date

#### Backlog View:
- Filter: `no:assignee is:open`
- Group by: Priority
- Sort by: Created date

## Workflow Integration

### 1. Issue Creation
```markdown
<!-- Link issues to project automatically -->
- Create issue with proper labels
- Set priority and component
- Assign story points
- Add to current sprint if ready
```

### 2. Development Process
1. **Planning**: Move issues from Backlog to Ready
2. **Development**: Assign and move to In Progress
3. **Review**: Create PR, auto-moves to In Review
4. **Testing**: Manual move to Testing column
5. **Completion**: Auto-moves to Done when PR merged

### 3. Sprint Management
```bash
# Sprint planning
- Review backlog items
- Assign story points
- Set sprint field
- Move to Sprint Backlog

# Daily standups
- Review In Progress items
- Update blockers in comments
- Move completed items

# Sprint review
- Demo completed features
- Move items to Done
- Update sprint metrics
```

## Project Templates

### Epic Template:
```markdown
## Epic: [Epic Name]

### Description
High-level feature description

### User Stories
- [ ] As a user, I want to...
- [ ] As an admin, I want to...

### Acceptance Criteria
- [ ] Criteria 1
- [ ] Criteria 2

### Technical Tasks
- [ ] Backend API
- [ ] Frontend UI
- [ ] Database changes
- [ ] Tests
- [ ] Documentation
```

### Sprint Template:
```markdown
## Sprint [Number]: [Sprint Goal]

### Duration
Start: [Date]
End: [Date]

### Sprint Goal
[What we want to achieve]

### Sprint Backlog
- [ ] Issue #1 (5 points)
- [ ] Issue #2 (3 points)
- [ ] Issue #3 (8 points)

### Total Points: 16
### Team Capacity: 20 points
```

## Metrics and Reporting

### Burndown Chart
- Track story points completed per day
- Identify scope creep or blockers
- Adjust sprint planning

### Velocity Tracking
- Average story points per sprint
- Team capacity planning
- Delivery predictability

### Cycle Time
- Time from "Ready" to "Done"
- Identify bottlenecks
- Process improvement

## Best Practices

### 1. Issue Management
- Keep issues small and focused
- Use clear, descriptive titles
- Add proper labels and assignments
- Link related issues and PRs

### 2. Board Maintenance
- Regular grooming sessions
- Archive completed sprints
- Update project documentation
- Review and adjust workflows

### 3. Team Collaboration
- Daily board reviews
- Sprint planning sessions
- Retrospectives for improvement
- Clear communication in comments

## Integration with CI/CD
- Link deployment status to board
- Auto-update based on build results
- Track feature flags and releases
- Monitor production issues
