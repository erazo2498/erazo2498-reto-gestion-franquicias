# This file only wires modules together; every resource lives in infra/modules.
# The three environments (dev, staging, prod) use this same code and differ only in envs/<environment>.tfvars.

module "networking" {
  source = "./modules/networking"

  project     = var.project
  environment = var.environment
}

module "ecr" {
  source = "./modules/ecr"

  project      = var.project
  environment  = var.environment
  force_delete = var.ecr_force_delete
}

module "database" {
  source = "./modules/database"

  project             = var.project
  environment         = var.environment
  subnet_ids          = module.networking.private_subnet_ids
  security_group_id   = module.networking.rds_security_group_id
  instance_class      = var.db_instance_class
  multi_az            = var.db_multi_az
  skip_final_snapshot = var.db_skip_final_snapshot
  deletion_protection = var.db_deletion_protection

  backup_retention_days = var.db_backup_retention_days
}

module "alb" {
  source = "./modules/alb"

  project           = var.project
  environment       = var.environment
  vpc_id            = module.networking.vpc_id
  subnet_ids        = module.networking.public_subnet_ids
  security_group_id = module.networking.alb_security_group_id
}

module "ecs" {
  source = "./modules/ecs"

  # the service registers into a target group that must already be attached to the listener
  depends_on = [module.alb]

  project                  = var.project
  environment              = var.environment
  region                   = var.region
  subnet_ids               = module.networking.public_subnet_ids
  security_group_id        = module.networking.ecs_security_group_id
  target_group_arn         = module.alb.target_group_arn
  container_image          = "${module.ecr.repository_url}:${var.image_tag}"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  min_capacity             = var.min_tasks
  max_capacity             = var.max_tasks
  db_connection_secret_arn = module.database.connection_secret_arn
  db_secret_arn            = module.database.master_secret_arn
  enable_execute_command   = var.enable_execute_command
}
