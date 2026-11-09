resource "aws_db_subnet_group" "main" {
  name       = "bitmarket"
  subnet_ids = aws_subnet.private[*].id
}

# No rules yet: traffic from the backend tasks is allowed together with the
# service that needs it.
resource "aws_security_group" "database" {
  name        = "bitmarket-database"
  description = "PostgreSQL access to the RDS instance"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "bitmarket-database"
  }
}

resource "aws_db_instance" "main" {
  identifier = "bitmarket"

  engine            = "postgres"
  engine_version    = "17"
  instance_class    = "db.t4g.micro"
  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name                     = "bitmarket"
  username                    = "bitmarket"
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.database.id]
  publicly_accessible    = false
  multi_az               = false

  # The environment does not keep data between applies, and any backup or
  # snapshot would outlive env-down.
  backup_retention_period  = 0
  delete_automated_backups = true
  skip_final_snapshot      = true
  deletion_protection      = false
  apply_immediately        = true
}
