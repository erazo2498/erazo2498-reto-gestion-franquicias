variable "project" {
  description = "Project name, used as a prefix for every resource"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_id" {
  description = "VPC of the target group"
  type        = string
}

variable "subnet_ids" {
  description = "Public subnets (at least two AZs)"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group of the load balancer"
  type        = string
}

variable "container_port" {
  description = "Port the application listens on"
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "Path the load balancer probes"
  type        = string
  default     = "/actuator/health"
}
