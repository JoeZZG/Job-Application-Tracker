# setup-cicd

Add or update the GitHub Actions CI/CD deployment pipeline.

## Goal
Automate: build JAR → Docker build → ECR push → ECS rolling deploy for all services.

## Input (provide in prompt)
- Services to include in the pipeline (default: all 4)
- AWS region
- ECS cluster name
- Whether to build all services in parallel or sequentially
- Any additional steps (run tests, lint, notify Slack, etc.)

## Output format
1. GitHub Actions workflow YAML (`.github/workflows/deploy.yml`) — complete file
2. List of GitHub repository secrets to configure
3. IAM policy the CI user needs (least-privilege, copy-pasteable JSON)
4. First-run checklist (ECR repos exist, ECS cluster exists, secrets set, etc.)
5. How to trigger a manual deploy
6. Rollback procedure (previous task definition revision)

## Rules
- Trigger: push to `main` only
- Image tag: git commit SHA (immutable) + `latest`
- Build cache: use `cache-from` for Docker layer caching
- AWS credentials via OIDC (preferred) or IAM user secrets
- Each service deployment independent — one failure should not block others (use separate jobs)
- ECS deploy via `aws ecs update-service --force-new-deployment`
