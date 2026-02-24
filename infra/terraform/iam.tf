# ---------------------------------------------------------------------------
# IAM — ECS execution role (shared) + per-service task roles (least-privilege)
# ---------------------------------------------------------------------------

# ---- ECS Execution Role (used by ECS agent to pull images + fetch secrets) -

data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_execution" {
  name               = "${local.name_prefix}-ecs-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution_managed" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Allow execution role to read all secrets for this environment
resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name = "${local.name_prefix}-ecs-execution-secrets"
  role = aws_iam_role.ecs_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue"]
        Resource = [
          aws_secretsmanager_secret.mysql_root_password.arn,
          aws_secretsmanager_secret.mysql_app_password.arn,
          aws_secretsmanager_secret.rabbitmq_password.arn,
          aws_secretsmanager_secret.jwt_secret.arn,
        ]
      }
    ]
  })
}

# ---- Per-service task roles (CloudWatch Logs write) ------------------------

resource "aws_iam_role" "ecs_task" {
  for_each           = local.services
  name               = "${local.name_prefix}-ecs-task-${each.key}"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy" "ecs_task_logs" {
  for_each = aws_iam_role.ecs_task
  name     = "${local.name_prefix}-ecs-task-${each.key}-logs"
  role     = each.value.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "arn:aws:logs:${var.aws_region}:${local.account_id}:log-group:/ecs/${local.name_prefix}-${each.key}:*"
      }
    ]
  })
}
