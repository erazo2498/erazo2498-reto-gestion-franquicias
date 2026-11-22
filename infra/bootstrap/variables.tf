variable "region" {
  description = "AWS region for the state bucket and the deploy roles."
  type        = string
  default     = "us-east-2"
}

variable "project" {
  description = "Prefix shared by every resource name; the deploy roles are limited to resources starting with it."
  type        = string
  default     = "reto-franquicias"
}

variable "github_repository" {
  description = "GitHub repository (owner/name) allowed to assume the deploy roles."
  type        = string
  default     = "erazo2498/reto-gestion-franquicias"
}

variable "environments" {
  description = "Environments that get their own deploy role. Each one must also exist as a GitHub Environment with the same name."
  type        = set(string)
  default     = ["dev", "staging", "prod"]
}

variable "create_oidc_provider" {
  description = "AWS allows one GitHub OIDC provider per account. Set to false if the account already has one."
  type        = bool
  default     = true
}
