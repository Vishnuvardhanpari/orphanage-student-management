# AGENT_WORKFLOW.md

# AI Agent Standard Operating Procedure (SOP)

## Purpose

This document defines the mandatory workflow that every AI agent must follow while working on this project.

The objective is to ensure:

* Consistent implementation
* High code quality
* Production-ready software
* Small reviewable changes
* Minimal technical debt

This workflow applies to every milestone and every feature.

---

# Source of Truth

Before starting any work, always read:

1. `docs/12_AI_Context.md`
2. `docs/Session_context.md`

Then read only the documentation relevant to the current task.

Examples:

Authentication

* 04_User_Roles.md
* 05_Business_Rules.md
* 07_API_Design.md

Student Module

* 02_Requirements.md
* 03_Features.md
* 06_Database_Design.md
* 08_UI_UX.md

Deployment

* 09_Deployment.md

Testing

* 11_Testing_Strategy.md

Do not load unnecessary documentation.

---

# Standard Workflow

Every milestone must follow this lifecycle.

```
Planning
      ↓
Architecture Review
      ↓
Approval
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
Git Commit
```

Never skip any step.

---

# Step 1 — Planning

Before generating code:

* Understand the requirement.
* Read the required documentation.
* Explain the implementation approach.
* Identify affected modules.
* Identify risks.
* Break implementation into logical tasks.

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

If improvements are needed:

Revise the plan.

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

Fix any Critical or Major issues before presenting the work.

---

# Step 5 — Independent Review

Perform a second review from the perspective of a Software Architect.

Do not implement new features.

Identify:

Critical

Major

Minor

Suggestions

Explain the reasoning behind every finding.

---

# Step 6 — Approval

Wait for user approval.

Do not continue to the next phase automatically.

---

# Step 7 — Update Session Context

Update:

`docs/Session_context.md`

Include:

* Completed milestone
* Completed phase
* Completed tasks
* Current task
* Next task
* Blockers

---

# Step 8 — Git

Recommend an appropriate commit message.

Example:

```
feat(auth): implement JWT authentication foundation
```

Do not perform Git operations.

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

Never:

* Rewrite unrelated files.
* Change project architecture without approval.
* Introduce breaking changes without explanation.
* Commit secrets.
* Hardcode credentials.
* Modify completed milestones unnecessarily.

---

# Review Checklist

Before considering work complete, verify:

Backend

* Project builds
* Tests pass
* No dependency conflicts
* Logging configured
* Configuration valid

Frontend

* Project builds
* Responsive UI
* Tailwind classes consistent
* No console errors

Infrastructure

* Docker builds
* PostgreSQL connectivity
* Environment variables
* Deployment compatibility

Documentation

* Session_context updated
* Documentation still accurate

---

# Agent Roles

## Planning Agent

Responsible for:

* Requirement analysis
* Architecture
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

---

## Review Agent

Responsible for:

* Architecture review
* Security review
* Performance review
* Maintainability review

Does not generate new features.

---

# Prompting Guidelines

When starting a new agent:

1. Read the required documentation.
2. Explain the implementation plan.
3. Wait for approval.
4. Implement only the approved scope.
5. Perform self-review.
6. Wait for approval before continuing.

---

# Definition of Done

A task is complete only when:

* Requirements implemented.
* Code builds successfully.
* No Critical review findings remain.
* Documentation updated.
* Session_context updated.
* Manual verification steps provided.
* Ready for Git commit.

Never proceed to the next milestone without explicit approval.
