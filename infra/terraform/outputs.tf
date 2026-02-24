# ---------------------------------------------------------------------------
# Outputs — endpoints and identifiers needed after apply
# ---------------------------------------------------------------------------

output "alb_dns_name" {
  description = "Public DNS name of the ALB — use as your API base URL"
  value       = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name (for CLI commands and CI/CD)"
  value       = aws_ecs_cluster.main.name
}

output "ecr_repository_urls" {
  description = "ECR repository URLs — use as Docker image base in CI/CD"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "rds_endpoint" {
  description = "RDS MySQL endpoint hostname"
  value       = aws_db_instance.main.address
}

output "elasticache_endpoint" {
  description = "ElastiCache Redis endpoint hostname"
  value       = aws_elasticache_cluster.main.cache_nodes[0].address
}

output "amazonmq_broker_endpoint" {
  description = "Amazon MQ AMQPS endpoint (amqps://...)"
  value       = tolist(aws_mq_broker.rabbitmq.instances)[0].endpoints[0]
}

output "frontend_s3_bucket" {
  description = "S3 bucket name for the React frontend — set as FRONTEND_S3_BUCKET GitHub secret"
  value       = aws_s3_bucket.frontend.bucket
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID — set as CLOUDFRONT_DISTRIBUTION_ID GitHub secret"
  value       = aws_cloudfront_distribution.frontend.id
}

output "secret_arns" {
  description = "Secrets Manager ARNs — reference in ECS task definitions"
  value = {
    mysql_app_password = aws_secretsmanager_secret.mysql_app_password.arn
    rabbitmq_password  = aws_secretsmanager_secret.rabbitmq_password.arn
    jwt_secret         = aws_secretsmanager_secret.jwt_secret.arn
  }
}
