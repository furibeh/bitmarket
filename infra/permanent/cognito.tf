resource "aws_cognito_user_pool" "main" {
  name                     = "bitmarket"
  user_pool_tier           = "ESSENTIALS"
  deletion_protection      = "ACTIVE"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  username_configuration {
    case_sensitive = false
  }

  # Required attributes cannot change once the pool exists, so only email is required.
  schema {
    name                = "email"
    attribute_data_type = "String"
    required            = true
    mutable             = true

    string_attribute_constraints {
      min_length = 0
      max_length = 2048
    }
  }

  lambda_config {
    post_confirmation = aws_lambda_function.post_confirmation.arn
  }
}

resource "aws_cognito_user_group" "customer" {
  name         = "CUSTOMER"
  user_pool_id = aws_cognito_user_pool.main.id
  description  = "Buyers; assigned by the Post Confirmation trigger (RN-44)."
}

resource "aws_cognito_user_group" "admin" {
  name         = "ADMIN"
  user_pool_id = aws_cognito_user_pool.main.id
  description  = "Administrators; assigned by hand (RN-44)."
}

resource "aws_cognito_user_pool_domain" "main" {
  domain                = var.cognito_domain_prefix
  user_pool_id          = aws_cognito_user_pool.main.id
  managed_login_version = 2
}

resource "aws_cognito_user_pool_client" "local" {
  name         = "bitmarket-local"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "profile"]
  supported_identity_providers         = ["COGNITO"]
  explicit_auth_flows                  = ["ALLOW_REFRESH_TOKEN_AUTH"]
  callback_urls                        = ["http://localhost:3000/api/auth/callback/cognito"]
  logout_urls                          = ["http://localhost:3000/"]
  enable_token_revocation              = true
  prevent_user_existence_errors        = "ENABLED"
}

resource "aws_cognito_managed_login_branding" "local" {
  client_id                   = aws_cognito_user_pool_client.local.id
  user_pool_id                = aws_cognito_user_pool.main.id
  use_cognito_provided_values = true
}
