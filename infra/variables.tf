variable "project" {
  description = "Project name, used as a prefix for every resource"
  type        = string
  default     = "reto-franquicias"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "image_tag" {
  description = "Immutable tag of the image in ECR that the service runs (pass it with -var image_tag=...)"
  type        = string
}

variable "ecr_force_delete" {
  description = "Allow destroying the ECR repository with images inside (disposable environments only)"
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "Days of automated database backups"
  type        = number
  default     = 1
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_multi_az" {
  description = "Standby replica of the database in another AZ"
  type        = bool
  default     = false
}

variable "db_skip_final_snapshot" {
  description = "Skip the final snapshot on deletion (disposable environments only)"
  type        = bool
  default     = true
}

variable "db_deletion_protection" {
  description = "Block accidental deletion of the database"
  type        = bool
  default     = false
}

variable "task_cpu" {
  description = "Fargate CPU units per task"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate memory (MiB) per task"
  type        = number
  default     = 1024
}

variable "min_tasks" {
  description = "Minimum number of tasks"
  type        = number
  default     = 1
}

variable "max_tasks" {
  description = "Maximum number of tasks"
  type        = number
  default     = 3
}

variable "enable_execute_command" {
  description = "Enable ECS Exec so a person can open a tunnel to the private database"
  type        = bool
  default     = false
}
