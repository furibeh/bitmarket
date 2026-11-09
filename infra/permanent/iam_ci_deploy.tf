data "aws_caller_identity" "current" {}

locals {
  terraform_state_bucket_arn = "arn:aws:s3:::bitmarket-tfstate-${data.aws_caller_identity.current.account_id}"
  ephemeral_state_object_arn = "${local.terraform_state_bucket_arn}/ephemeral/terraform.tfstate"
}

# The access keys are created outside Terraform so they never reach its state
# (ADR-0016). This user has no IAM write permissions (ADR-0017).
resource "aws_iam_user" "ci_deploy" {
  name = "bitmarket-ci-deploy"
}

data "aws_iam_policy_document" "ci_deploy" {
  statement {
    sid       = "DenyOtherRegions"
    effect    = "Deny"
    actions   = ["*"]
    resources = ["*"]

    condition {
      test     = "StringNotEquals"
      variable = "aws:RequestedRegion"
      values   = ["us-east-2"]
    }
  }

  statement {
    sid = "ManageNetwork"
    actions = [
      "ec2:AssociateRouteTable",
      "ec2:AttachInternetGateway",
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:CreateInternetGateway",
      "ec2:CreateRoute",
      "ec2:CreateRouteTable",
      "ec2:CreateSecurityGroup",
      "ec2:CreateSubnet",
      "ec2:CreateTags",
      "ec2:CreateVpc",
      "ec2:DeleteInternetGateway",
      "ec2:DeleteRoute",
      "ec2:DeleteRouteTable",
      "ec2:DeleteSecurityGroup",
      "ec2:DeleteSubnet",
      "ec2:DeleteTags",
      "ec2:DeleteVpc",
      "ec2:Describe*",
      "ec2:DetachInternetGateway",
      "ec2:DisassociateRouteTable",
      "ec2:ModifySubnetAttribute",
      "ec2:ModifyVpcAttribute",
      "ec2:RevokeSecurityGroupEgress",
      "ec2:RevokeSecurityGroupIngress",
    ]
    resources = ["*"]
  }

  statement {
    sid = "ManageDatabase"
    actions = [
      "rds:AddTagsToResource",
      "rds:CreateDBInstance",
      "rds:CreateDBSubnetGroup",
      "rds:DeleteDBInstance",
      "rds:DeleteDBSubnetGroup",
      "rds:DescribeDBInstances",
      "rds:DescribeDBSubnetGroups",
      "rds:ListTagsForResource",
      "rds:ModifyDBInstance",
      "rds:ModifyDBSubnetGroup",
      "rds:RemoveTagsFromResource",
    ]
    resources = ["*"]
  }

  # RDS creates the master password secret with the caller's permissions.
  statement {
    sid = "CreateDatabaseMasterSecret"
    actions = [
      "secretsmanager:CreateSecret",
      "secretsmanager:TagResource",
    ]
    resources = ["arn:aws:secretsmanager:us-east-2:${data.aws_caller_identity.current.account_id}:secret:rds!*"]
  }

  statement {
    sid       = "DescribeDefaultEncryptionKeys"
    actions   = ["kms:DescribeKey"]
    resources = ["*"]
  }

  statement {
    sid       = "VerifyTeardown"
    actions   = ["tag:GetResources"]
    resources = ["*"]
  }

  # Listing exposes only object names; the objects below are the only ones
  # this user can read or write.
  statement {
    sid       = "ListTerraformState"
    actions   = ["s3:ListBucket"]
    resources = [local.terraform_state_bucket_arn]
  }

  statement {
    sid = "ReadWriteEphemeralState"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]
    resources = [
      local.ephemeral_state_object_arn,
      "${local.ephemeral_state_object_arn}.tflock",
    ]
  }
}

# A managed policy because inline user policies are limited to 2,048 characters.
resource "aws_iam_policy" "ci_deploy" {
  name   = "bitmarket-ci-deploy"
  policy = data.aws_iam_policy_document.ci_deploy.json
}

resource "aws_iam_user_policy_attachment" "ci_deploy" {
  user       = aws_iam_user.ci_deploy.name
  policy_arn = aws_iam_policy.ci_deploy.arn
}
