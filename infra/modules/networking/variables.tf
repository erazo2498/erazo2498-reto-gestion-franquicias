variable "project" {
  description = "Project name, used as a prefix for every resource"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "container_port" {
  description = "Port the application listens on; the ECS security group only accepts the ALB on it"
  type        = number
  default     = 8080
}
