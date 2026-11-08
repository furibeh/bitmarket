# The access keys are created outside Terraform so they never reach its state
# (ADR-0016).
resource "aws_iam_user" "ci_images" {
  name = "bitmarket-ci-images"
}

data "aws_iam_policy_document" "ci_images" {
  statement {
    sid       = "AuthenticateToEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PublishImages"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [for repository in aws_ecr_repository.images : repository.arn]
  }
}

resource "aws_iam_user_policy" "ci_images" {
  name   = "publish-images"
  user   = aws_iam_user.ci_images.name
  policy = data.aws_iam_policy_document.ci_images.json
}
