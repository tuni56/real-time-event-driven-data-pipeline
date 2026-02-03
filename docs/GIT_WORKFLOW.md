# Git Workflow Guide

## Branch Strategy

```
main (production)
├── develop (integration)
    ├── feature/schema-registry
    ├── feature/monitoring-stack
    ├── feature/performance-testing
    └── feature/kafka-streams
```

## Branch Types

- **main**: Production-ready code
- **develop**: Integration branch for features
- **feature/***: Individual features
- **hotfix/***: Critical production fixes
- **release/***: Release preparation

## Workflow Commands

### Starting New Feature
```bash
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

### Daily Work
```bash
# Check status
git status

# Stage changes
git add .

# Commit with conventional format
git commit -m "feat(component): Brief description

- Detailed change 1
- Detailed change 2"

# Push to remote
git push origin feature/your-feature-name
```

### Finishing Feature
```bash
# Switch to develop
git checkout develop
git pull origin develop

# Merge feature
git merge feature/your-feature-name

# Push to remote
git push origin develop

# Clean up
git branch -d feature/your-feature-name
git push origin --delete feature/your-feature-name
```

## Commit Message Format

```
type(scope): Brief description

- Detailed change 1
- Detailed change 2
- Detailed change 3
```

### Types
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation
- **style**: Code style changes
- **refactor**: Code refactoring
- **test**: Adding tests
- **chore**: Maintenance tasks

## Current Features

- ✅ **feature/schema-registry**: Avro schema management
- ✅ **feature/monitoring-stack**: Prometheus/Grafana observability
- ✅ **feature/performance-testing**: Load testing suite
- ✅ **feature/kestra-workflows**: FAANG-level enhancements

## Safety Rules

1. **NEVER** commit directly to `main`
2. **ALWAYS** create features from `develop`
3. **ALWAYS** test before merging
4. **ALWAYS** use descriptive commit messages
5. **ALWAYS** pull before pushing
