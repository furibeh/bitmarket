# Both roles live here so the deploy user needs no IAM write access (ADR-0017).
data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_execution" {
  name               = "bitmarket-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "ecs_execution_secrets" {
  statement {
    sid       = "ReadBackendParameters"
    actions   = ["ssm:GetParameters"]
    resources = [for parameter in aws_ssm_parameter.backend_secrets : parameter.arn]
  }

  # RDS names the master password secret itself, and it changes on every apply.
  statement {
    sid       = "ReadDatabaseMasterSecret"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = ["arn:aws:secretsmanager:us-east-2:${data.aws_caller_identity.current.account_id}:secret:rds!*"]
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name   = "read-secrets"
  role   = aws_iam_role.ecs_execution.id
  policy = data.aws_iam_policy_document.ecs_execution_secrets.json
}

# No policies yet: the backend does not call any AWS API.
resource "aws_iam_role" "ecs_task" {
  name               = "bitmarket-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}
