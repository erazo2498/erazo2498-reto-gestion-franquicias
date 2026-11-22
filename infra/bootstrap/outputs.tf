output "state_bucket" {
  description = "Pass it at init: terraform init -backend-config=\"bucket=<this value>\"."
  value       = aws_s3_bucket.state.bucket
}

output "deploy_role_arns" {
  description = "Role to assume per environment; store each one as the AWS_ROLE_ARN variable of the matching GitHub Environment."
  value       = { for env, role in aws_iam_role.deploy : env => role.arn }
}
