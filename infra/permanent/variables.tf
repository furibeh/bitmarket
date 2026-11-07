variable "alert_email" {
  description = "Address that receives the AWS Budgets alerts."
  type        = string
  sensitive   = true
}

variable "cognito_domain_prefix" {
  description = "Prefix of the Cognito domain; it must be free across the region."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$", var.cognito_domain_prefix))
    error_message = "Use lowercase letters, digits and inner hyphens, up to 63 characters."
  }
}
