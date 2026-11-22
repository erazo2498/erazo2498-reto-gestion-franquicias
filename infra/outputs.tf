output "api_url" {
  description = "Public URL of the API (the ALB DNS name)"
  value       = "http://${module.alb.dns_name}"
}

output "alb_dns_name" {
  description = "DNS name of the load balancer"
  value       = module.alb.dns_name
}

output "ecr_repository_url" {
  description = "Repository to push the image to"
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "ECS service"
  value       = module.ecs.service_name
}

output "db_address" {
  description = "Database host (reachable only from inside the VPC)"
  value       = module.database.address
}
