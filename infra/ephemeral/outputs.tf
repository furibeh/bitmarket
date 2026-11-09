output "created_at" {
  value = var.created_at
}

output "vpc_id" {
  value = aws_vpc.main.id
}

output "db_instance_identifier" {
  value = aws_db_instance.main.identifier
}

output "load_balancer_dns_name" {
  value = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "backend_service_name" {
  value = aws_ecs_service.backend.name
}

output "backend_target_group_arn" {
  value = aws_lb_target_group.backend.arn
}
