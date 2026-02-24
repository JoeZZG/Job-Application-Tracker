# ---------------------------------------------------------------------------
# Amazon MQ — managed RabbitMQ broker
# Replaces the Docker RabbitMQ container used locally.
# ---------------------------------------------------------------------------

resource "aws_mq_broker" "rabbitmq" {
  broker_name        = "${local.name_prefix}-rabbitmq"
  engine_type        = "RabbitMQ"
  engine_version     = "3.13"
  host_instance_type = var.amazonmq_instance_type
  deployment_mode    = var.amazonmq_deployment_mode

  auto_minor_version_upgrade = true
  publicly_accessible        = false

  subnet_ids         = var.amazonmq_deployment_mode == "ACTIVE_STANDBY_MULTI_AZ" ? aws_subnet.private[*].id : [aws_subnet.private[0].id]
  security_groups    = [aws_security_group.amazonmq.id]

  user {
    username = "jat_rabbit"
    password = var.rabbitmq_password
  }

  # Enable CloudWatch metrics
  logs {
    general = true
  }

  tags = { Name = "${local.name_prefix}-rabbitmq" }
}
