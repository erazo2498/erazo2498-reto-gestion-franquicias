# Application Load Balancer that exposes the service.

locals {
  name = "${var.project}-${var.environment}"
}

resource "aws_lb" "this" {
  name               = local.name
  load_balancer_type = "application"
  internal           = false
  subnets            = var.subnet_ids
  security_groups    = [var.security_group_id]

  tags = { Name = local.name }
}

resource "aws_lb_target_group" "this" {
  name        = local.name
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip" # Fargate tasks register by IP
  vpc_id      = var.vpc_id

  deregistration_delay = 30

  health_check {
    path                = var.health_check_path
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = { Name = local.name }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}
