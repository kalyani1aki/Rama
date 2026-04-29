# Single security group for the EC2 instance.
# Port 22 (SSH) is intentionally omitted — use AWS SSM Session Manager instead.
# Port 443 is open so certbot / Let's Encrypt can be set up later without Terraform changes.
resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-${var.environment}-ec2-sg"
  description = "Rama EC2: allow HTTP and HTTPS inbound"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-${var.environment}-ec2-sg" }
}
