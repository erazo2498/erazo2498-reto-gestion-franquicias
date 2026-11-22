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

variable "github_subject_prefix" {
  description = <<-EOT
    Start of the "sub" claim that GitHub puts in its OIDC token for this repository. This repository uses immutable
    subjects (owner and repository IDs), so it is not just "repo:<owner>/<name>". Read the real value with:
    gh api repos/<owner>/<name>/actions/oidc/customization/sub  (field sub_claim_prefix)
  EOT
  type        = string
  default     = "repo:erazo2498@61292603/reto-gestion-franquicias@1467641897"
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
