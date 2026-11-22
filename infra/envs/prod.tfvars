# Values for the prod environment: protected against accidental deletion, with a standby database and more tasks.
# Because of that, "terraform destroy" fails on purpose until deletion_protection is turned off.
environment = "prod"

db_instance_class      = "db.t4g.small"
db_multi_az            = true
db_skip_final_snapshot = false
db_deletion_protection = true

task_cpu    = 1024
task_memory = 2048
min_tasks   = 2
max_tasks   = 4

ecr_force_delete         = false
db_backup_retention_days = 7

enable_execute_command = false
