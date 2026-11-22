# Values for the staging environment: same shape as dev, disposable, so it can be destroyed cleanly.
environment = "staging"

db_instance_class      = "db.t4g.micro"
db_multi_az            = false
db_skip_final_snapshot = true
db_deletion_protection = false

task_cpu    = 512
task_memory = 1024
min_tasks   = 1
max_tasks   = 2

ecr_force_delete         = true
db_backup_retention_days = 1

enable_execute_command = false
