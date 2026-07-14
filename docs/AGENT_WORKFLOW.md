# AGENT_WORKFLOW.md

# AI Agent Standard Operating Procedure (SOP)

## Purpose

This document defines the mandatory workflow that every AI agent must follow while working on this project.

The objectives are:

* Consistent implementation
* High code quality
* Production-ready software
* Small, reviewable changes
* Minimal technical debt
* Predictable development workflow
* Clean Git history

This workflow applies to every milestone and every feature.

---

# Source of Truth

Before starting any work, always read:

1. `docs/12_AI_Context.md`
2. `docs/Session_context.md`

Then read only the documentation relevant to the current task.

Examples:

### Authentication

* `docs/04_User_Roles.md`
* `docs/05_Business_Rules.md`
* `docs/07_API_Design.md`

### Student Module

* `docs/02_Requirements.md`
* `docs/03_Features.md`
* `docs/06_Database_Design.md`
* `docs/08_UI_UX.md`

### Deployment

* `docs/09_Deployment.md`

### Testing

* `docs/11_Testing_Strategy.md`

Do not load unnecessary documentation.

---


# Standard Workflow

Every milestone must follow this lifecycle.

```text
Planning
      ↓
Architecture Review
      ↓
Approval
      ↓
Git Branch Verification
      ↓
Create / Switch to Milestone Branch
      ↓
Implementation
      ↓
Self Review
      ↓
Independent Review
      ↓
Approval
      ↓
Update Session Context
      ↓
Recommend Git Commit
      ↓
Milestone Complete
```

Never skip any step.

---

Git Branch Verification (Mandatory)

Git branches are created only after the implementation plan has been reviewed and approved.

This ensures:

The implementation scope is finalized.
The architecture is approved.
No unnecessary branches are created for discarded or revised plans.
Branch Strategy

Every milestone must be developed in its own dedicated Git branch.

Examples:

milestone/project-initialization
milestone/authentication
milestone/user-management
milestone/student-database
milestone/student-registration
milestone/student-profile
milestone/student-search
milestone/report-export
milestone/dashboard

If a milestone is large, feature branches may be created under the milestone.

Examples:

feature/auth-jwt
feature/auth-google-oauth
feature/auth-refresh-token

feature/student-upload
feature/student-search

feature/report-single-pdf
feature/report-bulk-pdf
AI Workflow

After the Architecture Review has been approved, but before any implementation begins, the AI agent must:

Verify the current Git branch.
Determine whether it matches the approved milestone.
If not, recommend the Git commands to create and switch to the correct milestone branch.
Wait for confirmation before generating any implementation.

Example:

git checkout main
git pull origin main
git checkout -b milestone/authentication

Never begin implementation on the main branch.

Do not perform Git operations automatically.


And I'd also update the **Prompting Guidelines** to match:

:::writing{variant="document" id="64720"}
# Prompting Guidelines

When starting a new AI agent:

1. Read the required documentation.
2. Analyze the requirements.
3. Produce the implementation plan.
4. Perform the Architecture Review.
5. Wait for approval.
6. Verify the current Git branch.
7. If the milestone branch is not checked out, recommend the appropriate Git commands.
8. Wait for branch confirmation.
9. Implement only the approved scope.
10. Perform a self-review.
11. Wait for approval before continuing.

Never assume the correct Git branch has already been checked out.
---

# Step 1 — Planning

Before generating code:

* Understand the requirement.
* Read the required documentation.
* Explain the implementation approach.
* Identify affected modules.
* Identify risks.
* Identify assumptions.
* Break implementation into logical, reviewable tasks.

Do not generate code.

Wait for approval.

---

# Step 2 — Architecture Review

Review the proposed implementation.

Evaluate:

* Clean Architecture
* SOLID Principles
* DRY
* KISS
* YAGNI
* Security
* Scalability
* Maintainability
* Future compatibility
* Production readiness

If improvements are needed:

* Revise the implementation plan.
* Explain why changes are required.

If acceptable, explicitly state:

**Architecture Review: APPROVED**

---

# Step 3 — Implementation

Generate production-ready code.

Requirements:

* Follow project coding standards.
* Keep changes limited to the approved scope.
* Avoid unrelated modifications.
* Avoid placeholder implementations.
* Avoid TODO comments.
* Use descriptive names.
* Follow existing project conventions.
* Keep implementations modular.
* Preserve backward compatibility whenever possible.

---

# Step 4 — Self Review

Immediately after implementation, review your own work.

Review for:

* Compilation issues
* Dependency conflicts
* Security
* Performance
* Maintainability
* Readability
* Clean Architecture
* SOLID
* Documentation consistency
* Project standards compliance

Fix any Critical or Major issues before presenting the implementation.

---

# Step 5 — Independent Review

Review the completed implementation from the perspective of a Software Architect.

Do not implement new features.

Evaluate:

* Architecture
* Security
* Performance
* Maintainability
* Scalability
* Code quality
* Folder structure
* Configuration
* Documentation

Present findings grouped as:

* Critical
* Major
* Minor
* Suggestions

Explain the reasoning behind every finding.

---

# Step 6 — Approval

Wait for user approval.

Do not continue automatically to another phase, task, or milestone.

---

# Step 7 — Update Session Context

Update:

`docs/Session_context.md`

Include:

* Current milestone
* Current phase
* Completed tasks
* Current task
* Next task
* Known blockers
* Current project status

---

# Step 8 — Git

Recommend an appropriate commit message.

Example:

```text
feat(auth): implement JWT authentication foundation
```

Recommend:

* Reviewing changes
* Running builds
* Running tests
* Creating a Pull Request

Do not perform Git operations automatically.

---

# Development Rules

Always:

* Generate production-ready code.
* Prefer composition over inheritance.
* Prefer constructor injection.
* Use configuration instead of hardcoding.
* Keep components small.
* Keep classes focused.
* Keep methods concise.
* Keep commits logically grouped.
* Explain important architectural decisions.

Never:

* Rewrite unrelated files.
* Change project architecture without approval.
* Introduce breaking changes without explanation.
* Commit secrets.
* Hardcode credentials.
* Modify completed milestones unnecessarily.
* Continue beyond the approved scope.

---

# Review Checklist

Before considering work complete, verify:

## Backend

* Project builds successfully.
* Tests pass.
* No dependency conflicts.
* Logging configured.
* Configuration valid.
* Security configuration reviewed.

## Frontend

* Project builds successfully.
* Responsive UI.
* Tailwind classes consistent.
* No console errors.
* Lazy loading strategy respected.

## Infrastructure

* Docker builds.
* PostgreSQL connectivity.
* Environment variables.
* Deployment compatibility.

## Documentation

* Session_context updated.
* Documentation remains accurate.
* Architecture still aligns with AI Context.

---

# Agent Roles

## Planning Agent

Responsible for:

* Requirement analysis
* Architecture
* Technical design
* Task breakdown
* Risk analysis

Never generates implementation code.

---

## Implementation Agent

Responsible for:

* Backend
* Frontend
* Infrastructure

Implements only approved tasks.

Performs a self-review after implementation.

---

## Review Agent

Responsible for:

* Architecture review
* Security review
* Performance review
* Maintainability review
* Production readiness review

Does not generate new features.

---

# Prompting Guidelines

When starting a new AI agent:

1. Read the required documentation.
2. Verify the current Git branch.
3. If the milestone branch is not checked out, recommend the appropriate Git commands.
4. Wait for branch confirmation.
5. Explain the implementation plan.
6. Wait for approval.
7. Implement only the approved scope.
8. Perform a self-review.
9. Wait for approval before continuing.

Never assume the correct Git branch has already been checked out.

---

# Standard AI Startup Questions

At the beginning of every new milestone or feature, ask:

1. Which milestone or feature are we implementing?
2. Are you currently on the correct Git branch?
3. If not, would you like me to recommend the Git commands?
4. Have the required documentation files been loaded?
5. Shall I begin with the Planning stage?

Do not generate code until these questions have been answered.

---

# Definition of Done

A task is complete only when:

* Requirements implemented.
* Code builds successfully.
* Tests pass where applicable.
* No Critical review findings remain.
* Documentation updated.
* Session_context updated.
* Manual verification steps provided.
* Development completed on the correct milestone branch.
* Recommended commit message provided.
* Pull Request strategy recommended.
* Ready for Git commit.

Never proceed to the next milestone without explicit user approval.
