# No ingress rules yet: CloudFront's security group is allowed together with
# the VPC origin (ADR-0014).
resource "aws_security_group" "load_balancer" {
  name        = "bitmarket-load-balancer"
  description = "Internal application load balancer"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "bitmarket-load-balancer"
  }
}

resource "aws_vpc_security_group_egress_rule" "load_balancer_to_backend" {
  security_group_id            = aws_security_group.load_balancer.id
  referenced_security_group_id = aws_security_group.backend.id
  ip_protocol                  = "tcp"
  from_port                    = local.backend_port
  to_port                      = local.backend_port
}

resource "aws_lb" "main" {
  name               = "bitmarket"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.load_balancer.id]
  subnets            = aws_subnet.private[*].id
}

resource "aws_lb_target_group" "backend" {
  name                 = "bitmarket-backend"
  port                 = local.backend_port
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = aws_vpc.main.id
  deregistration_delay = 30

  health_check {
    path              = "/actuator/health"
    matcher           = "200"
    healthy_threshold = 2
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      status_code  = "404"
    }
  }
}

# Only the routes that are public on the backend (architecture.md §2).
resource "aws_lb_listener_rule" "backend" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    path_pattern {
      values = ["/webhooks/*", "/mcp", "/.well-known/oauth-protected-resource/mcp"]
    }
  }
}
