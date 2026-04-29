variable "aws_region" {
  type        = string
  description = "AWS region to deploy into"
  default     = "eu-central-1"
}

variable "project_name" {
  type        = string
  description = "Project name used for resource naming and tagging"
  default     = "rama"
}

variable "environment" {
  type        = string
  description = "Environment name (dev, staging, prod)"
  default     = "dev"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type. t3.micro is free-tier eligible; t3.small (2 GB RAM) is safer for Spring Boot."
  default     = "t3.micro"
}

variable "key_name" {
  type        = string
  description = "EC2 key pair name for SSH access. Leave empty to use SSM Session Manager only (recommended)."
  default     = ""
}

variable "admin_emails" {
  type        = string
  description = "Comma-separated list of admin email addresses"
  default     = ""
}

variable "backend_image" {
  type        = string
  description = "Full ECR image URI for the backend (e.g. 123456789012.dkr.ecr.eu-central-1.amazonaws.com/rama-backend:latest)"
}

variable "frontend_image" {
  type        = string
  description = "Full ECR image URI for the frontend (e.g. 123456789012.dkr.ecr.eu-central-1.amazonaws.com/rama-frontend:latest)"
}
