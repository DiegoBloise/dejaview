---
name: git-commits
description: Analyzes unstaged changes and creates incremental, well-organized commits following project conventions. Use when the user asks to commit changes, organize commits, or split work into logical commits.
---

# Git Commits

Automates the creation of incremental, well-organized commits by analyzing changes and grouping them logically.

## When to Use

- User asks to "commit changes" or "organize commits"
- User wants to "split changes into commits"
- User asks for "smart commits" or "logical commits"
- User wants to follow "good commit practices"

## Process

### 1. Analyze Current State

```bash
git status
git log --oneline -15
git diff --stat
```

Understand:
- What files are modified/added/deleted
- Recent commit patterns (message style, prefixes)
- Scope of changes

### 2. Analyze Changes in Detail

For each group of related files, run:
```bash
git diff -- <files>
```

Identify:
- **Feature additions** (new files, new functionality)
- **Refactoring** (code reorganization, formatting)
- **Bug fixes** (corrections, patches)
- **Configuration changes** (config files, dependencies)
- **Test updates** (test files)
- **Documentation** (README, AGENTS.md, etc.)

### 3. Group Changes Logically

**Single Responsibility per commit:** each commit should address one logical change — a feature, a fix, or a refactor. Split controller/service/repository changes into separate commits when they address different concerns.

Group files by **cohesion** - changes that belong together conceptually:

| Group Type | Examples |
|------------|----------|
| Database migrations | `V*.sql` files |
| Infrastructure | Config files, context classes, filters |
| Entity/Model changes | Models with similar annotations |
| Authentication/Security | Token services, security configs |
| Feature implementation | Controllers, services, DTOs for same feature |
| Repository changes | Repository interfaces with similar updates |
| DTO formatting | Multiple DTOs with formatting-only changes |
| Tests | Test files for new/modified features |
| Documentation | README, AGENTS.md, docs |

### 4. Commit Message Convention

Follow the project's existing convention. Common prefixes:

| Prefix | Usage |
|--------|-------|
| `feat:` | New features, new functionality |
| `fix:` | Bug fixes, corrections |
| `refact:` | Code refactoring, reorganization, formatting |
| `update:` | Updates to existing code, tests, docs |
| `chore:` | Maintenance, dependencies |

**Message format:**
- Short and descriptive (max 50-60 chars)
- Lowercase after prefix
- No period at end
- Describe WHAT changed, not HOW

Examples:
```
feat: multi-tenant infrastructure - context, filter, aspect
refact: DTO formatting and mapper updates
update: tests for multi-tenant and new features
```

### 5. Execute Commits

For each logical group:

```bash
# Stage files
git add <file1> <file2> ...

# Commit with descriptive message
git commit -m "<prefix>: <description>"
```

### 6. Verify

```bash
git status  # Should be clean
git log --oneline -N  # Show created commits
```

## Multi-Project Handling

If workspace has multiple projects (e.g., `api/`, `app/`, `platform/`):

1. Complete all commits in first project
2. Move to next project
3. Repeat process for each project independently
4. Show summary of all commits per project at the end

## Example Output

```
**apex-api** (5 commits):
- feat: database migrations for multi-tenant schemas
- feat: multi-tenant infrastructure
- refact: DTO formatting
- update: tests
- update: documentation

**apex-app** (3 commits):
- feat: STOMP WebSocket dependencies
- feat: tenant header in auth
- refact: kanban event handling
```

## Important Notes

- **Never commit secrets** (API keys, passwords, tokens)
- **Single Responsibility** - each commit must address one logical change (feature, fix, refactor), not a mix of unrelated concerns
- **Reviewable** - each commit must be reviewable independently
- **Related changes together** - don't split tightly coupled changes
- **Descriptive bodies** - if the subject line isn't enough, add a body explaining WHY
- **Follow existing patterns** - match the project's commit style
- **No backend-only assumption** - handle frontend, backend, and fullstack projects
