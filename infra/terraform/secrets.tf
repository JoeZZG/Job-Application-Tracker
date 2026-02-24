# ---------------------------------------------------------------------------
# AWS Secrets Manager — stores all runtime secrets
# ECS tasks inject these via `secrets` block (valueFrom = secret ARN)
# ---------------------------------------------------------------------------

resource "aws_secretsmanager_secret" "mysql_root_password" {
  name                    = "${local.name_prefix}/mysql/root_password"
  description             = "RDS MySQL root password"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  tags                    = { Name = "${local.name_prefix}-secret-mysql-root" }
}

resource "aws_secretsmanager_secret_version" "mysql_root_password" {
  secret_id     = aws_secretsmanager_secret.mysql_root_password.id
  secret_string = var.mysql_root_password
}

resource "aws_secretsmanager_secret" "mysql_app_password" {
  name                    = "${local.name_prefix}/mysql/app_password"
  description             = "RDS MySQL application user password"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  tags                    = { Name = "${local.name_prefix}-secret-mysql-app" }
}

resource "aws_secretsmanager_secret_version" "mysql_app_password" {
  secret_id     = aws_secretsmanager_secret.mysql_app_password.id
  secret_string = var.mysql_app_password
}

resource "aws_secretsmanager_secret" "rabbitmq_password" {
  name                    = "${local.name_prefix}/rabbitmq/password"
  description             = "Amazon MQ RabbitMQ broker password"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  tags                    = { Name = "${local.name_prefix}-secret-rabbitmq" }
}

resource "aws_secretsmanager_secret_version" "rabbitmq_password" {
  secret_id     = aws_secretsmanager_secret.rabbitmq_password.id
  secret_string = var.rabbitmq_password
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${local.name_prefix}/jwt/secret"
  description             = "JWT signing secret shared across all services"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
  tags                    = { Name = "${local.name_prefix}-secret-jwt" }
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}
