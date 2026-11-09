data "aws_caller_identity" "current" {}

locals {
  account_id   = data.aws_caller_identity.current.account_id
  backend_port = 8080

  # Built by hand: the aws_ssm_parameter data source would copy the decrypted
  # value into this layer's state.
  ssm_parameter_prefix = "arn:aws:ssm:us-east-2:${local.account_id}:parameter/bitmarket"
}

resource "aws_security_group" "backend" {
  name        = "bitmarket-backend"
  description = "Backend tasks on Fargate"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "bitmarket-backend"
  }
}

resource "aws_vpc_security_group_ingress_rule" "backend_from_load_balancer" {
  security_group_id            = aws_security_group.backend.id
  referenced_security_group_id = aws_security_group.load_balancer.id
  ip_protocol                  = "tcp"
  from_port                    = local.backend_port
  to_port                      = local.backend_port
}

# ECR, SSM, Secrets Manager, CloudWatch Logs, Cognito and Mercado Pago.
resource "aws_vpc_security_group_egress_rule" "backend_to_https" {
  security_group_id = aws_security_group.backend.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "backend_to_database" {
  security_group_id            = aws_security_group.backend.id
  referenced_security_group_id = aws_security_group.database.id
  ip_protocol                  = "tcp"
  from_port                    = aws_db_instance.main.port
  to_port                      = aws_db_instance.main.port
}

resource "aws_ecs_cluster" "main" {
  name = "bitmarket"
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/bitmarket-backend"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "bitmarket-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024

  # Created in the permanent layer and referenced by name (ADR-0017).
  execution_role_arn = "arn:aws:iam::${local.account_id}:role/bitmarket-ecs-execution"
  task_role_arn      = "arn:aws:iam::${local.account_id}:role/bitmarket-ecs-task"

  runtime_platform {
    cpu_architecture        = "X86_64"
    operating_system_family = "LINUX"
  }

  container_definitions = jsonencode([{
    name      = "backend"
    image     = "${local.account_id}.dkr.ecr.us-east-2.amazonaws.com/bitmarket/backend:${var.backend_image_tag}"
    essential = true

    portMappings = [{
      containerPort = local.backend_port
      protocol      = "tcp"
    }]

    environment = [
      { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.main.endpoint}/${aws_db_instance.main.db_name}" },
      { name = "DB_USER", value = aws_db_instance.main.username },
    ]

    secrets = [
      { name = "BFF_DB_PASSWORD", valueFrom = "${local.ssm_parameter_prefix}/bff-db-password" },
      { name = "DB_PASSWORD", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::" },
      { name = "MERCADOPAGO_ACCESS_TOKEN", valueFrom = "${local.ssm_parameter_prefix}/mercadopago-access-token" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.backend.name
        awslogs-region        = "us-east-2"
        awslogs-stream-prefix = "backend"
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "bitmarket-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  # Flyway runs before the application starts answering health checks.
  health_check_grace_period_seconds = 180

  # env-up turns green only when the task is healthy, and a backend that keeps
  # crashing fails the apply instead of waiting for the timeout.
  wait_for_steady_state = true

  deployment_circuit_breaker {
    enable   = true
    rollback = false
  }

  # Public IP because there is no NAT Gateway (ADR-0010).
  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.backend.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = local.backend_port
  }

  # ECS rejects a target group without a listener, and a task started before
  # its route and rules exist fails to pull the image or reach the database.
  depends_on = [
    aws_lb_listener_rule.backend,
    aws_route_table_association.public,
    aws_vpc_security_group_egress_rule.backend_to_database,
    aws_vpc_security_group_egress_rule.backend_to_https,
    aws_vpc_security_group_egress_rule.load_balancer_to_backend,
    aws_vpc_security_group_ingress_rule.backend_from_load_balancer,
    aws_vpc_security_group_ingress_rule.database_from_backend,
  ]
}
