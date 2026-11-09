terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.72"
    }
  }

  # The bucket name includes the account ID, so it is passed at init time:
  # terraform init -backend-config="bucket=<name>"
  backend "s3" {
    key          = "ephemeral/terraform.tfstate"
    region       = "us-east-2"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = "us-east-2"

  default_tags {
    tags = {
      "bitmarket:project"    = "bitmarket"
      "bitmarket:layer"      = "ephemeral"
      "bitmarket:created-at" = var.created_at
    }
  }
}
