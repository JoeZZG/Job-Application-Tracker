# ---------------------------------------------------------------------------
# Security Groups — least-privilege rules per service
# ---------------------------------------------------------------------------

# ALB — accepts public HTTP/HTTPS
resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-sg-alb"
  description = "ALB inbound: HTTP 80 and HTTPS 443 from internet"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-alb" }
}

# Gateway service — accepts traffic from ALB only
resource "aws_security_group" "gateway" {
  name        = "${local.name_prefix}-sg-gateway"
  description = "Gateway ECS: inbound 8080 from ALB only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Gateway port from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-gateway" }
}

# Auth service — accepts traffic from Gateway only
resource "aws_security_group" "auth" {
  name        = "${local.name_prefix}-sg-auth"
  description = "Auth ECS: inbound 8081 from Gateway only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Auth port from Gateway"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.gateway.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-auth" }
}

# Application service — accepts traffic from Gateway only
resource "aws_security_group" "application" {
  name        = "${local.name_prefix}-sg-application"
  description = "Application ECS: inbound 8082 from Gateway only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Application port from Gateway"
    from_port       = 8082
    to_port         = 8082
    protocol        = "tcp"
    security_groups = [aws_security_group.gateway.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-application" }
}

# Notification service — accepts traffic from Gateway only
resource "aws_security_group" "notification" {
  name        = "${local.name_prefix}-sg-notification"
  description = "Notification ECS: inbound 8083 from Gateway only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Notification port from Gateway"
    from_port       = 8083
    to_port         = 8083
    protocol        = "tcp"
    security_groups = [aws_security_group.gateway.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-notification" }
}

# RDS MySQL — accepts from application services only
resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-sg-rds"
  description = "RDS MySQL: inbound 3306 from application service SGs"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "MySQL from auth-service"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.auth.id]
  }

  ingress {
    description     = "MySQL from application-service"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.application.id]
  }

  ingress {
    description     = "MySQL from notification-service"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.notification.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-rds" }
}

# ElastiCache Redis — accepts from application-service only
resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-sg-redis"
  description = "ElastiCache Redis: inbound 6379 from application-service only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Redis from application-service"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.application.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-redis" }
}

# Amazon MQ — accepts from application-service and notification-service
resource "aws_security_group" "amazonmq" {
  name        = "${local.name_prefix}-sg-amazonmq"
  description = "Amazon MQ: inbound AMQPS 5671 from application and notification services"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "AMQPS from application-service"
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = [aws_security_group.application.id]
  }

  ingress {
    description     = "AMQPS from notification-service"
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = [aws_security_group.notification.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-sg-amazonmq" }
}
