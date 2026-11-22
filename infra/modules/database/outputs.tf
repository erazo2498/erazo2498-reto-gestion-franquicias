output "address" {
  description = "Database host name"
  value       = aws_db_instance.this.address
}

output "port" {
  description = "Database port"
  value       = aws_db_instance.this.port
}

output "db_name" {
  description = "Database name"
  value       = aws_db_instance.this.db_name
}

output "master_secret_arn" {
  description = "Secrets Manager secret (JSON with username and password) managed by RDS"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}

output "connection_secret_arn" {
  description = "Secrets Manager secret (JSON with host, port, dbname and ssl) for the application"
  value       = aws_secretsmanager_secret.connection.arn

  # ECS must not start before the secret has its content, or the tasks fail to read host/port/dbname.
  depends_on = [aws_secretsmanager_secret_version.connection]
}
