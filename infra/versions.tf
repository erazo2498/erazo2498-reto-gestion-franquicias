terraform {
  required_version = ">= 1.10.0" # S3 state locking without DynamoDB (use_lockfile)

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
  }

  # Partial configuration: the rest comes from backends/<environment>.hcl and the bucket from -backend-config at init.
  backend "s3" {}
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
