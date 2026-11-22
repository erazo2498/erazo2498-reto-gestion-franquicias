variable "project" {
  description = "Project name, used as a prefix for every resource"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "region" {
  description = "AWS region (used by the log driver)"
  type        = string
}

variable "subnet_ids" {
  description = "Subnets where the tasks run (public, see the networking module)"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group of the tasks"
  type        = string
}

variable "target_group_arn" {
  description = "ALB target group the service registers into"
  type        = string
}

variable "container_image" {
  description = "Full image reference in ECR, including the tag"
  type        = string
}

variable "container_port" {
  description = "Port the application listens on"
  type        = number
  default     = 8080
}

variable "cpu" {
  description = "Fargate CPU units (256 = 0.25 vCPU)"
  type        = number
  default     = 512
}

variable "memory" {
  description = "Fargate memory in MiB"
  type        = number
  default     = 1024
}

variable "min_capacity" {
  description = "Minimum number of tasks (Auto Scaling)"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Maximum number of tasks (Auto Scaling)"
  type        = number
  default     = 3
}

variable "cpu_target_percent" {
  description = "Average CPU utilisation the Auto Scaling policy aims for"
  type        = number
  default     = 60
}

variable "db_connection_secret_arn" {
  description = "Secrets Manager secret with the database host, port, dbname and ssl"
  type        = string
}

variable "db_secret_arn" {
  description = "Secrets Manager secret with the database username and password"
  type        = string
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention"
  type        = number
  default     = 14
}

variable "enable_execute_command" {
  description = "Enable ECS Exec (shell / port-forward into a task). Only for environments where a person needs to inspect the database"
  type        = bool
  default     = false
}
