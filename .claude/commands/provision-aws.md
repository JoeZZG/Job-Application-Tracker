# provision-aws

Plan or apply Terraform to provision AWS infrastructure for this project.

## Goal
Create or update the AWS resources required to run the full microservices stack on ECS Fargate.

## Input (provide in prompt)
- Action: plan / apply / destroy / specific module
- Environment: staging / production
- AWS region
- Whether RDS should be Multi-AZ
- Whether an ACM certificate ARN is available for HTTPS
- Any existing resources to import

## Output format
1. Architecture summary (what will be created/changed/destroyed)
2. Terraform file(s) to create or modify (complete, copy-pasteable)
3. `terraform.tfvars.example` additions
4. `terraform init` + `terraform plan` + `terraform apply` commands
5. Post-apply verification checklist
6. Estimated monthly cost (rough ballpark)
7. Cleanup / destroy instructions

## Rules
- All sensitive variables marked `sensitive = true`
- Resources tagged: `Project = job-application-tracker`, `Environment = {env}`, `ManagedBy = terraform`
- State backend in S3 + DynamoDB — document but leave commented out by default (dev can enable)
- Never output secrets in Terraform outputs — output ARNs, endpoints, and IDs only
- Security groups follow least-privilege rules from CLAUDE.md
