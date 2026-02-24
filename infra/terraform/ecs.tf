# ---------------------------------------------------------------------------
# ECS Fargate — cluster, CloudWatch log groups, task definitions, services
# ---------------------------------------------------------------------------

# ---- Cluster ---------------------------------------------------------------

resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "${local.name_prefix}-cluster" }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 1
  }
}

# ---- CloudWatch Log Groups -------------------------------------------------

resource "aws_cloudwatch_log_group" "services" {
  for_each          = local.services
  name              = "/ecs/${local.name_prefix}-${each.key}"
  retention_in_days = var.environment == "production" ? 30 : 7
  tags              = { Name = "${local.name_prefix}-logs-${each.key}" }
}

# ---- Helper locals for service endpoints ------------------------------------

locals {
  rds_endpoint = aws_db_instance.main.address

  # ElastiCache returns a list — take the first cache node address
  redis_endpoint = aws_elasticache_cluster.main.cache_nodes[0].address

  # Amazon MQ AMQPS endpoint — strip the amqps:// prefix for host extraction
  # The broker returns URLs like amqps://b-xxxx.mq.us-east-1.amazonaws.com:5671
  mq_endpoint = replace(
    tolist(aws_mq_broker.rabbitmq.instances)[0].endpoints[0],
    "amqps://", ""
  )
  mq_host = split(":", local.mq_endpoint)[0]
}

# ---- auth-service task definition ------------------------------------------

resource "aws_ecs_task_definition" "auth" {
  family                   = "${local.name_prefix}-auth"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = local.services.auth.cpu
  memory                   = local.services.auth.memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task["auth"].arn

  container_definitions = jsonencode([
    {
      name      = "auth-service"
      image     = "${aws_ecr_repository.services["auth"].repository_url}:latest"
      essential = true

      portMappings = [{ name = "auth", containerPort = 8081, protocol = "tcp" }]

      environment = [
        { name = "AUTH_PORT",          value = "8081" },
        { name = "AUTH_DB_URL",        value = "jdbc:mysql://${local.rds_endpoint}:3306/auth_db?useSSL=true&requireSSL=true&serverTimezone=UTC" },
        { name = "AUTH_DB_USERNAME",   value = "root" },
      ]

      secrets = [
        { name = "AUTH_DB_PASSWORD", valueFrom = aws_secretsmanager_secret.mysql_root_password.arn },
        { name = "JWT_SECRET",       valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.services["auth"].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8081/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = { Name = "${local.name_prefix}-td-auth" }
}

# ---- application-service task definition -----------------------------------

resource "aws_ecs_task_definition" "application" {
  family                   = "${local.name_prefix}-application"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = local.services.application.cpu
  memory                   = local.services.application.memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task["application"].arn

  container_definitions = jsonencode([
    {
      name      = "application-service"
      image     = "${aws_ecr_repository.services["application"].repository_url}:latest"
      essential = true

      portMappings = [{ name = "application", containerPort = 8082, protocol = "tcp" }]

      environment = [
        { name = "APP_PORT",                        value = "8082" },
        { name = "APP_DB_URL",                      value = "jdbc:mysql://${local.rds_endpoint}:3306/application_db?useSSL=true&requireSSL=true&serverTimezone=UTC" },
        { name = "APP_DB_USERNAME",                 value = "root" },
        { name = "REDIS_HOST",                      value = local.redis_endpoint },
        { name = "REDIS_PORT",                      value = "6379" },
        { name = "REDIS_DASHBOARD_TTL_MINUTES",     value = "5" },
        { name = "RABBITMQ_HOST",                   value = local.mq_host },
        { name = "RABBITMQ_PORT",                   value = "5671" },
        { name = "RABBITMQ_DEFAULT_USER",           value = "jat_rabbit" },
        # Amazon MQ requires TLS (AMQPS port 5671) — enable SSL in Spring AMQP
        { name = "SPRING_RABBITMQ_SSL_ENABLED",              value = "true" },
        { name = "SPRING_RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE", value = "false" },
      ]

      secrets = [
        { name = "APP_DB_PASSWORD",       valueFrom = aws_secretsmanager_secret.mysql_root_password.arn },
        { name = "JWT_SECRET",            valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
        { name = "RABBITMQ_DEFAULT_PASS", valueFrom = aws_secretsmanager_secret.rabbitmq_password.arn },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.services["application"].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8082/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = { Name = "${local.name_prefix}-td-application" }
}

# ---- notification-service task definition ----------------------------------

resource "aws_ecs_task_definition" "notification" {
  family                   = "${local.name_prefix}-notification"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = local.services.notification.cpu
  memory                   = local.services.notification.memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task["notification"].arn

  container_definitions = jsonencode([
    {
      name      = "notification-service"
      image     = "${aws_ecr_repository.services["notification"].repository_url}:latest"
      essential = true

      portMappings = [{ name = "notification", containerPort = 8083, protocol = "tcp" }]

      environment = [
        { name = "NOTIF_PORT",             value = "8083" },
        { name = "NOTIF_DB_URL",           value = "jdbc:mysql://${local.rds_endpoint}:3306/notification_db?useSSL=true&requireSSL=true&serverTimezone=UTC" },
        { name = "NOTIF_DB_USERNAME",      value = "root" },
        { name = "RABBITMQ_HOST",          value = local.mq_host },
        { name = "RABBITMQ_PORT",          value = "5671" },
        { name = "RABBITMQ_DEFAULT_USER",  value = "jat_rabbit" },
        { name = "SPRING_RABBITMQ_SSL_ENABLED",              value = "true" },
        { name = "SPRING_RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE", value = "false" },
      ]

      secrets = [
        { name = "NOTIF_DB_PASSWORD",      valueFrom = aws_secretsmanager_secret.mysql_root_password.arn },
        { name = "JWT_SECRET",             valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
        { name = "RABBITMQ_DEFAULT_PASS",  valueFrom = aws_secretsmanager_secret.rabbitmq_password.arn },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.services["notification"].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8083/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = { Name = "${local.name_prefix}-td-notification" }
}

# ---- gateway-service task definition ---------------------------------------

resource "aws_ecs_task_definition" "gateway" {
  family                   = "${local.name_prefix}-gateway"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = local.services.gateway.cpu
  memory                   = local.services.gateway.memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task["gateway"].arn

  container_definitions = jsonencode([
    {
      name      = "gateway-service"
      image     = "${aws_ecr_repository.services["gateway"].repository_url}:latest"
      essential = true

      portMappings = [{ name = "gateway", containerPort = 8080, protocol = "tcp" }]

      environment = [
        { name = "GATEWAY_PORT",        value = "8080" },
        # ECS Service Connect provides internal DNS for service-to-service routing
        { name = "AUTH_SERVICE_URI",    value = "http://auth-service:8081" },
        { name = "APP_SERVICE_URI",     value = "http://application-service:8082" },
        { name = "NOTIF_SERVICE_URI",   value = "http://notification-service:8083" },
        { name = "CORS_ALLOWED_ORIGIN", value = var.cors_allowed_origin },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.services["gateway"].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 30
      }
    }
  ])

  tags = { Name = "${local.name_prefix}-td-gateway" }
}

# ---- ECS Services ----------------------------------------------------------

resource "aws_ecs_service" "auth" {
  name            = "${local.name_prefix}-auth"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.auth.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.auth.id]
    assign_public_ip = false
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
    service {
      port_name      = "auth"
      discovery_name = "auth-service"
      client_alias {
        port     = 8081
        dns_name = "auth-service"
      }
    }
  }

  tags = { Name = "${local.name_prefix}-svc-auth" }
  depends_on = [aws_iam_role.ecs_execution]
}

resource "aws_ecs_service" "application" {
  name            = "${local.name_prefix}-application"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.application.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.application.id]
    assign_public_ip = false
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
    service {
      port_name      = "application"
      discovery_name = "application-service"
      client_alias {
        port     = 8082
        dns_name = "application-service"
      }
    }
  }

  tags = { Name = "${local.name_prefix}-svc-application" }
  depends_on = [aws_iam_role.ecs_execution]
}

resource "aws_ecs_service" "notification" {
  name            = "${local.name_prefix}-notification"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.notification.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.notification.id]
    assign_public_ip = false
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
    service {
      port_name      = "notification"
      discovery_name = "notification-service"
      client_alias {
        port     = 8083
        dns_name = "notification-service"
      }
    }
  }

  tags = { Name = "${local.name_prefix}-svc-notification" }
  depends_on = [aws_iam_role.ecs_execution]
}

resource "aws_ecs_service" "gateway" {
  name            = "${local.name_prefix}-gateway"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.gateway.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.gateway.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.gateway.arn
    container_name   = "gateway-service"
    container_port   = 8080
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
  }

  tags = { Name = "${local.name_prefix}-svc-gateway" }
  depends_on = [aws_lb_listener.http, aws_iam_role.ecs_execution]
}

# ---- ECS Service Connect namespace (internal DNS) --------------------------

resource "aws_service_discovery_http_namespace" "main" {
  name = "${local.name_prefix}.local"
  tags = { Name = "${local.name_prefix}-sd-namespace" }
}
