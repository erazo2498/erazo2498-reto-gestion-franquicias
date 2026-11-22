variable "project" {
  description = "Project name, used as a prefix for every resource"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "image_tag_mutability" {
  description = "IMMUTABLE keeps every tag pointing to one image forever (reproducible deployments)"
  type        = string
  default     = "IMMUTABLE"
}

variable "force_delete" {
  description = "Let terraform destroy delete a repository that still has images (disposable environments only)"
  type        = bool
  default     = false
}

variable "keep_last_images" {
  description = "How many images the lifecycle policy keeps"
  type        = number
  default     = 10
}
