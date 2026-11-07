locals {
  post_confirmation_name = "bitmarket-post-confirmation"
}

data "archive_file" "post_confirmation" {
  type        = "zip"
  source_dir  = "${path.module}/lambda/post-confirmation/src"
  output_path = "${path.module}/build/post-confirmation.zip"
}

data "aws_iam_policy_document" "post_confirmation_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "post_confirmation" {
  name               = local.post_confirmation_name
  assume_role_policy = data.aws_iam_policy_document.post_confirmation_assume.json
}

data "aws_iam_policy_document" "post_confirmation" {
  statement {
    actions   = ["cognito-idp:AdminAddUserToGroup"]
    resources = [aws_cognito_user_pool.main.arn]
  }

  statement {
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.post_confirmation.arn}:*"]
  }
}

resource "aws_iam_role_policy" "post_confirmation" {
  name   = local.post_confirmation_name
  role   = aws_iam_role.post_confirmation.id
  policy = data.aws_iam_policy_document.post_confirmation.json
}

resource "aws_cloudwatch_log_group" "post_confirmation" {
  name              = "/aws/lambda/${local.post_confirmation_name}"
  retention_in_days = 14
}

resource "aws_lambda_function" "post_confirmation" {
  function_name    = local.post_confirmation_name
  role             = aws_iam_role.post_confirmation.arn
  runtime          = "nodejs24.x"
  handler          = "index.handler"
  filename         = data.archive_file.post_confirmation.output_path
  source_code_hash = data.archive_file.post_confirmation.output_base64sha256
  memory_size      = 128
  # Cognito waits at most 5 seconds for a trigger and that limit cannot be changed.
  timeout = 5

  depends_on = [aws_cloudwatch_log_group.post_confirmation]
}

resource "aws_lambda_permission" "post_confirmation_from_cognito" {
  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.post_confirmation.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.main.arn
}
