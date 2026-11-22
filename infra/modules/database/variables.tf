variable "project" {
  description = "Project name, used as a prefix for every resource"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "subnet_ids" {
  description = "Private subnets for the database subnet group (at least two AZs)"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group that only accepts PostgreSQL from the application"
  type        = string
}

variable "instance_class" {
  description = "RDS instance class (db.t4g.micro is covered by the AWS free plan)"
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Storage in GB"
  type        = number
  default     = 20
}

variable "engine_version" {
  description = "PostgreSQL major version"
  type        = string
  default     = "16"
}

variable "db_name" {
  description = "Name of the database created with the instance"
  type        = string
  default     = "franchises"
}

variable "username" {
  description = "Master user name; its password is generated and kept by RDS in Secrets Manager"
  type        = string
  default     = "franchises"
}

variable "multi_az" {
  description = "Standby replica in another AZ"
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  description = "Days of automated backups"
  type        = number
  default     = 1
}

variable "skip_final_snapshot" {
  description = "Skip the snapshot taken on deletion (true only for disposable environments)"
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Block accidental deletion"
  type        = bool
  default     = false
}
