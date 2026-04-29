output "app_url" {
  description = "Public URL of the Rama application"
  value       = "http://${aws_eip.app.public_ip}"
}

output "app_public_ip" {
  description = "Elastic IP address of the EC2 instance"
  value       = aws_eip.app.public_ip
}

output "backend_ecr_repository_url" {
  description = "ECR repository URL for the backend image — push here before deploying"
  value       = aws_ecr_repository.backend.repository_url
}

output "frontend_ecr_repository_url" {
  description = "ECR repository URL for the frontend image — push here before deploying"
  value       = aws_ecr_repository.frontend.repository_url
}

output "h2_ebs_volume_id" {
  description = "EBS volume ID storing H2 database — snapshot this before major changes"
  value       = aws_ebs_volume.h2_data.id
}

output "ssm_connect_command" {
  description = "Command to open a shell on the EC2 instance via SSM (no SSH key needed)"
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.aws_region}"
}
