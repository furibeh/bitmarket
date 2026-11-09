output "created_at" {
  value = var.created_at
}

output "vpc_id" {
  value = aws_vpc.main.id
}

output "db_instance_identifier" {
  value = aws_db_instance.main.identifier
}
