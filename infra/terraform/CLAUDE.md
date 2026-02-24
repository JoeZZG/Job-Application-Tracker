# infra/terraform/

## Purpose

Terraform IaC for all AWS infrastructure. Provisions the complete cloud environment: VPC networking, ECS Fargate cluster (4 services), RDS MySQL, ElastiCache Redis, Amazon MQ, ALB, CloudFront + S3 (frontend), ECR, Secrets Manager, and IAM roles. Provider: `hashicorp/aws ~> 5.0`.

**State backend**: S3 bucket + DynamoDB lock table (configured in `main.tf`).

## Key Files

| File | Role |
|---|---|
| `main.tf` | Provider config, S3/DynamoDB backend, locals (`project`, `env`, `common_tags`) |
| `variables.tf` | Input variables: `aws_region`, `environment`, `app_name`, ECR image URIs |
| `outputs.tf` | Outputs: ALB DNS, CloudFront domain, ECR repo URLs, RDS endpoint, MQ endpoint |
| `vpc.tf` | VPC (10.0.0.0/16), public/private subnets, NAT gateway, route tables, IGW |
| `security_groups.tf` | SGs for ALB, each ECS service, RDS, ElastiCache, Amazon MQ |
| `alb.tf` | Application Load Balancer (public subnet), HTTPS listener, HTTP→HTTPS redirect, target group for gateway |
| `cloudfront.tf` | CloudFront distribution: S3 origin (SPA via OAC), ALB origin (API), path behavior routing |
| `ecs.tf` | ECS Fargate cluster, 4 task definitions, 4 services with Service Connect, CloudWatch log groups |
| `ecr.tf` | ECR repositories (one per service): `jat-{env}-{service}` |
| `rds.tf` | RDS MySQL 8.0 instance, parameter group, subnet group |
| `elasticache.tf` | ElastiCache Redis 7 cluster (single-node), subnet group |
| `amazonmq.tf` | Amazon MQ RabbitMQ 3.x broker, broker user |
| `iam.tf` | ECS task execution role (ECR pull, CW logs, Secrets Manager), task roles (least-privilege) |
| `secrets.tf` | Secrets Manager secrets: DB passwords, JWT secret |
| `terraform.tfvars.example` | Template for required variable values |

## Structure

```
terraform/
├── main.tf               # Provider, backend, locals
├── variables.tf          # Input variables
├── outputs.tf            # Exported values
├── vpc.tf                # Networking
├── security_groups.tf    # SG per service (least-privilege)
├── alb.tf                # Public load balancer
├── cloudfront.tf         # CDN + S3 SPA + API routing
├── ecs.tf                # Compute (Fargate tasks + services)
├── ecr.tf                # Container registries
├── rds.tf                # MySQL database
├── elasticache.tf        # Redis cache
├── amazonmq.tf           # RabbitMQ broker
├── iam.tf                # IAM roles + policies
├── secrets.tf            # Secrets Manager
├── terraform.tfvars.example  # Variable template (committed)
├── terraform.tfvars      # Actual values (gitignored)
├── .terraform.lock.hcl   # Provider version lock (committed)
└── .terraform/           # Provider binaries (gitignored)
```

## Implementation Overview

### Networking (`vpc.tf`)
- VPC CIDR: 10.0.0.0/16
- Public subnets (ALB): 10.0.1.0/24, 10.0.2.0/24 (multi-AZ)
- Private subnets (ECS, RDS, Redis, MQ): 10.0.10.0/24, 10.0.11.0/24
- NAT Gateway in public subnet for outbound internet from private resources

### Security Groups (`security_groups.tf`)
- **ALB SG**: inbound 80/443 from `0.0.0.0/0`
- **Gateway SG**: inbound 8080 from ALB SG only
- **Auth/App/Notification SGs**: inbound their port from Gateway SG only
- **RDS SG**: inbound 3306 from Application SG only
- **ElastiCache SG**: inbound 6379 from Application SG only
- **MQ SG**: inbound 5671 (AMQPS) from Application SG + Notification SG

### CloudFront (`cloudfront.tf`)
Two origins:
1. **S3 origin** (private bucket via OAC) — serves static SPA files
2. **ALB origin** (HTTPS) — serves API requests

Path behavior routing order:
| Precedence | Path Pattern | Origin |
|---|---|---|
| 1 | `/auth/*` | ALB |
| 2 | `/applications*` | ALB |
| 3 | `/notifications*` | ALB |
| Default | `*` | S3 |

**Critical**: use `/applications*` (not `/applications/*`) for paths where the base is a valid endpoint. `/applications/*` does NOT match `GET /applications` (no trailing slash/segment).

Custom error responses: 403/404 from S3 → 200 with `/index.html` (SPA client-side routing support).

### ECS Task Definitions (`ecs.tf`)

| Service | CPU | Memory | Port |
|---|---|---|---|
| gateway | 256 | 512 MB | 8080 |
| auth | 256 | 512 MB | 8081 |
| application | 512 | 1024 MB | 8082 |
| notification | 256 | 512 MB | 8083 |

- Log driver: `awslogs` → `/ecs/{service-name}` CloudWatch log group
- Health check: `GET /actuator/health` via ALB target group
- Secrets injected via `valueFrom` using Secrets Manager ARNs (never in plaintext)
- Service Connect enabled for internal DNS between services

### Secrets (`secrets.tf`)
All secrets stored in AWS Secrets Manager:
- `jat-{env}/mysql/auth-password`
- `jat-{env}/mysql/app-password`
- `jat-{env}/mysql/notification-password`
- `jat-{env}/jwt/secret`
- `jat-{env}/rabbitmq/password`

Injected into ECS tasks as env vars via `secrets` block in task definition.

## Implementation Details & Gotchas

- **Resource naming**: `{project}-{env}-{resource}` (e.g., `jat-staging-gateway`). Defined via `locals` in `main.tf`.
- **All resources tagged**: `Project`, `Environment`, `ManagedBy = terraform` on every resource.
- **`terraform.tfstate` is sensitive**: contains plaintext secret values. Never commit. Stored in S3 backend with encryption.
- **ECR tags are mutable**: `:latest` is re-pushed on every CI/CD deploy. ECS force-new-deployment pulls the latest image.
- **ALB → CloudFront certificate**: the ACM certificate for the CloudFront distribution must be in `us-east-1` regardless of the deployment region. The ALB certificate must be in the deployment region.
- **Terraform state lock**: DynamoDB table prevents concurrent applies. If a `terraform apply` is interrupted, the lock may need manual release: `terraform force-unlock <lock-id>`.
- **`terraform.tfvars` is gitignored**: contains real values for `aws_region`, `environment`, ECR image URIs, etc. Use `terraform.tfvars.example` as the template.
- **ECS Service Connect**: requires an ECS cluster namespace. Internal service-to-service calls use DNS names (`auth-service`, `application-service`, `notification-service`) without hardcoding IP addresses.
- **RDS Multi-AZ**: set `multi_az = true` for production. Single-AZ for staging to reduce cost.

## Usage

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Fill in: aws_region, environment, ecr_image_uris, etc.

terraform init         # Downloads providers, configures S3 backend
terraform plan         # Review changes
terraform apply        # Provision infrastructure (~15-20 min first run)

# Outputs:
terraform output alb_dns_name
terraform output cloudfront_domain_name
terraform output ecr_repository_urls
```

**Pre-requisites before `terraform init`**:
1. S3 bucket for state must exist (create manually or via bootstrap script)
2. DynamoDB table for lock must exist
3. AWS credentials configured (`aws configure` or env vars)
4. ACM certificates must be requested and validated in advance

## Testing

No automated Terraform tests. Manual verification checklist after `terraform apply`:
1. `curl https://{cloudfront_domain}/` → returns `index.html` (200)
2. `curl https://{cloudfront_domain}/auth/login` → reaches gateway → 401 or 405 (not S3 HTML)
3. `curl https://{cloudfront_domain}/applications` → reaches gateway → 401 (not S3 HTML)
4. `curl https://{alb_dns}/actuator/health` → `{"status":"UP"}` (gateway health)
5. ECS console: all 4 services show 1/1 Running
6. CloudWatch logs: `/ecs/auth-service`, etc. — no ERROR lines on startup

## Related

- `.github/workflows/deploy.yml` — CI/CD that pushes images to ECR and triggers ECS deploy
- `infra/.env.example` — local dev env vars (different from Terraform vars)
- `.claude/agents/aws-deploy.md` — detailed AWS deployment conventions and rules
- Root `CLAUDE.md` → AWS Deployment Architecture section
