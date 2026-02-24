variable "aws_region" {
  description = "AWS region to deploy into (e.g. us-east-1)"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment: staging or production"
  type        = string
  default     = "staging"
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment must be 'staging' or 'production'."
  }
}

# ---------------------------------------------------------------------------
# Networking
# ---------------------------------------------------------------------------
variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

# ---------------------------------------------------------------------------
# RDS
# ---------------------------------------------------------------------------
variable "rds_instance_class" {
  description = "RDS instance type (e.g. db.t3.micro, db.t3.small)"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ for RDS (true = production, false = staging)"
  type        = bool
  default     = false
}

variable "rds_allocated_storage_gb" {
  description = "Initial allocated storage in GB"
  type        = number
  default     = 20
}

# ---------------------------------------------------------------------------
# ElastiCache
# ---------------------------------------------------------------------------
variable "elasticache_node_type" {
  description = "ElastiCache node type (e.g. cache.t3.micro)"
  type        = string
  default     = "cache.t3.micro"
}

# ---------------------------------------------------------------------------
# Amazon MQ
# ---------------------------------------------------------------------------
variable "amazonmq_instance_type" {
  description = "Amazon MQ broker instance type (e.g. mq.t3.micro)"
  type        = string
  default     = "mq.t3.micro"
}

variable "amazonmq_deployment_mode" {
  description = "Amazon MQ deployment mode: SINGLE_INSTANCE or ACTIVE_STANDBY_MULTI_AZ"
  type        = string
  default     = "SINGLE_INSTANCE"
}

# ---------------------------------------------------------------------------
# Secrets (sensitive — never commit real values)
# ---------------------------------------------------------------------------
variable "mysql_root_password" {
  description = "MySQL root password"
  type        = string
  sensitive   = true
}

variable "mysql_app_password" {
  description = "MySQL application user password"
  type        = string
  sensitive   = true
}

variable "rabbitmq_password" {
  description = "RabbitMQ broker password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret (min 32 chars)"
  type        = string
  sensitive   = true
}

# ---------------------------------------------------------------------------
# ACM / HTTPS (optional — leave empty to skip HTTPS on ALB)
# ---------------------------------------------------------------------------
variable "acm_certificate_arn" {
  description = "ARN of an ACM certificate for HTTPS on the ALB. Leave empty to use HTTP only."
  type        = string
  default     = ""
}

variable "cors_allowed_origin" {
  description = "CORS allowed origin for the API Gateway (e.g. https://yourdomain.com)"
  type        = string
  default     = "http://localhost:5173"
}
