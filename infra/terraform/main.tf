terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # ---------------------------------------------------------------------------
  # Remote state — S3 + DynamoDB lock.
  # Uncomment and fill in once you have created:
  #   - S3 bucket:       jat-terraform-state-{account_id}
  #   - DynamoDB table:  jat-terraform-locks  (partition key: LockID, type: S)
  # ---------------------------------------------------------------------------
  # backend "s3" {
  #   bucket         = "jat-terraform-state-<YOUR_ACCOUNT_ID>"
  #   key            = "job-application-tracker/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "jat-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "job-application-tracker"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ---------------------------------------------------------------------------
# Data — current AWS account / caller identity
# ---------------------------------------------------------------------------
data "aws_caller_identity" "current" {}

data "aws_availability_zones" "available" {
  state = "available"
}

# ---------------------------------------------------------------------------
# Locals — derived values used across modules
# ---------------------------------------------------------------------------
locals {
  name_prefix  = "jat-${var.environment}"
  account_id   = data.aws_caller_identity.current.account_id
  azs          = slice(data.aws_availability_zones.available.names, 0, 2)

  services = {
    gateway     = { port = 8080, cpu = 256,  memory = 512  }
    auth        = { port = 8081, cpu = 256,  memory = 512  }
    application = { port = 8082, cpu = 512,  memory = 1024 }
    notification= { port = 8083, cpu = 256,  memory = 512  }
  }
}
