output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.main.id
}

output "cognito_issuer" {
  value = "https://${aws_cognito_user_pool.main.endpoint}"
}

output "cognito_domain_url" {
  value = "https://${aws_cognito_user_pool_domain.main.domain}.auth.us-east-2.amazoncognito.com"
}

output "cognito_local_client_id" {
  value = aws_cognito_user_pool_client.local.id
}

output "cognito_local_client_secret" {
  value     = aws_cognito_user_pool_client.local.client_secret
  sensitive = true
}

output "ecr_repository_urls" {
  value = { for name, repository in aws_ecr_repository.images : name => repository.repository_url }
}
