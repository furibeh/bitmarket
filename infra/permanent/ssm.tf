# The real values are loaded with `aws ssm put-parameter --overwrite`. With
# `value`, every refresh would store the decrypted value in the state, even
# with ignore_changes; `value_wo` never reaches it. Do not change
# value_wo_version: it would write the placeholder over the real value.
resource "aws_ssm_parameter" "backend_secrets" {
  for_each = toset(["bff-db-password", "mercadopago-access-token"])

  name             = "/bitmarket/${each.key}"
  type             = "SecureString"
  value_wo         = "placeholder"
  value_wo_version = 1
}
