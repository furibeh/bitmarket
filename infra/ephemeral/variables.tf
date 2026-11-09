variable "backend_image_tag" {
  description = "Tag of the backend image in ECR (the commit SHA). env-up checks that it exists; destroying ignores it."
  type        = string
}

variable "created_at" {
  description = "UTC time the environment was first created. Re-applying keeps the original value, so the environment's age is measured from its first creation."
  type        = string

  validation {
    condition     = can(regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$", var.created_at))
    error_message = "Use the UTC format YYYY-MM-DDThh:mm:ssZ."
  }
}
