output "dns_name" {
  description = "Public DNS name of the API"
  value       = aws_lb.this.dns_name
}

output "target_group_arn" {
  description = "Target group the ECS service registers into"
  value       = aws_lb_target_group.this.arn
}
