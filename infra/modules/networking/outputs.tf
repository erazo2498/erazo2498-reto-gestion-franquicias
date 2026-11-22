output "vpc_id" {
  description = "VPC id"
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Public subnets (ALB and ECS tasks)"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnets (database)"
  value       = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  description = "Security group of the load balancer"
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "Security group of the ECS tasks"
  value       = aws_security_group.ecs.id
}

output "rds_security_group_id" {
  description = "Security group of the database"
  value       = aws_security_group.rds.id
}
