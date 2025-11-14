---
name: Add Fork Detection to SonarCloud Jobs
issue: <TBD>
state: complete
version: 1
---

## Goal

Prevent SonarCloud jobs from running on pull requests from forks by adding the same fork detection condition that is currently used for the test reporter steps. This will prevent failed jobs due to missing secrets and save CI resources.

## Tasks

### 1. Add fork detection to Analysis SonarCloud job
- Modify `.github/workflows/build-analysis.yml` at line 114 (after `needs: build`)
- Add job-level `if` condition to skip the job for fork PRs
- Condition should allow internal PRs and non-PR events (push to main, workflow_dispatch)
- Use pattern: `if: github.event.pull_request.head.repo.full_name == github.repository || github.event_name != 'pull_request'`

### 2. Add fork detection to Visualization SonarCloud job
- Modify `.github/workflows/build-visualization.yml` at line 172 (after `needs: build`)
- Add the same job-level `if` condition as in Task 1
- Ensure consistency with the Analysis workflow

## Steps

- [x] Complete Task 1: Add fork detection to Analysis SonarCloud job
- [x] Complete Task 2: Add fork detection to Visualization SonarCloud job

## Review Feedback Addressed

N/A - Initial plan

## Notes

**Current fork detection pattern:**
The test reporter steps already use fork detection at step level:
```yaml
if: always() && github.event.pull_request.head.repo.full_name == github.repository
```

**Selected approach:**
- Job-level condition (skip entire job) rather than step-level
- Includes fallback for non-PR events to ensure SonarCloud runs on push to main

**Condition breakdown:**
- `github.event.pull_request.head.repo.full_name == github.repository` - Returns true if PR is from same repo (internal PR)
- `github.event_name != 'pull_request'` - Returns true if event is push, workflow_dispatch, etc.

**Expected behavior after implementation:**
- Push to main → SonarCloud runs ✅
- Internal PR → SonarCloud runs ✅
- Fork PR → SonarCloud skipped ⏭️
- Manual trigger → SonarCloud runs ✅

**Implementation completed:**
- Added `if` condition to `.github/workflows/build-analysis.yml:115`
- Added `if` condition to `.github/workflows/build-visualization.yml:173`
- Both jobs now use identical fork detection logic
