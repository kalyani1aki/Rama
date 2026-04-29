data "aws_caller_identity" "current" {}

data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

locals {
  ecr_registry = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name
  key_name               = var.key_name != "" ? var.key_name : null

  # Replacing user_data (e.g. new image tags) triggers a clean instance replacement.
  # The separate EBS data volume preserves H2 data across replacements.
  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/templates/user_data.sh.tpl", {
    region         = var.aws_region
    ecr_registry   = local.ecr_registry
    backend_image  = var.backend_image
    frontend_image = var.frontend_image
    admin_emails   = var.admin_emails
  })

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  tags = { Name = "${var.project_name}-${var.environment}-app" }
}

# Separate EBS volume for H2 data — survives instance replacement.
# Do NOT delete this volume unless you want to wipe all order/user data.
resource "aws_ebs_volume" "h2_data" {
  availability_zone = aws_instance.app.availability_zone
  size              = 10
  type              = "gp3"
  encrypted         = true

  tags = { Name = "${var.project_name}-${var.environment}-h2-data" }
}

resource "aws_volume_attachment" "h2_data" {
  device_name  = "/dev/sdf"
  volume_id    = aws_ebs_volume.h2_data.id
  instance_id  = aws_instance.app.id
  force_detach = true
}

resource "aws_eip" "app" {
  instance   = aws_instance.app.id
  domain     = "vpc"
  depends_on = [aws_internet_gateway.main]

  tags = { Name = "${var.project_name}-${var.environment}-eip" }
}
