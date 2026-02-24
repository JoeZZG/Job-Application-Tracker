# ---------------------------------------------------------------------------
# RDS MySQL 8.0
# Hosts three logical databases: auth_db, application_db, notification_db.
# Applications connect with the same credential; schemas are separated by DB name.
# ---------------------------------------------------------------------------

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id
  tags       = { Name = "${local.name_prefix}-db-subnet-group" }
}

resource "aws_db_parameter_group" "mysql8" {
  name   = "${local.name_prefix}-mysql8"
  family = "mysql8.0"

  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }

  tags = { Name = "${local.name_prefix}-mysql8-params" }
}

resource "aws_db_instance" "main" {
  identifier     = "${local.name_prefix}-mysql"
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.rds_instance_class

  allocated_storage     = var.rds_allocated_storage_gb
  max_allocated_storage = var.rds_allocated_storage_gb * 5
  storage_type          = "gp3"
  storage_encrypted     = true

  # Applications create databases via Flyway; we only bootstrap the root user here.
  db_name  = null
  username = "root"
  password = var.mysql_root_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  parameter_group_name   = aws_db_parameter_group.mysql8.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az               = var.rds_multi_az
  publicly_accessible    = false
  deletion_protection    = var.environment == "production"
  skip_final_snapshot    = var.environment != "production"
  final_snapshot_identifier = var.environment == "production" ? "${local.name_prefix}-mysql-final" : null

  backup_retention_period = var.environment == "production" ? 7 : 1
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"

  enabled_cloudwatch_logs_exports = ["error", "slowquery"]

  tags = { Name = "${local.name_prefix}-mysql" }
}
