## Rama Infrastructure

### Architecture

Single EC2 instance running both services via Docker Compose.

```
Internet → EC2 Elastic IP :80
              ├── nginx (frontend container) — serves Angular SPA
              └── /api/* → Spring Boot (backend container) :8080
                               └── H2 database → EBS data volume (/data/ramadb)
```

### AWS resources

| Resource | Purpose | Est. cost |
|---|---|---|
| EC2 t3.micro | Runs frontend + backend | ~$8.5/mo (free tier: $0) |
| EBS 20 GB gp3 | Root volume (Docker images, OS) | ~$1.6/mo |
| EBS 10 GB gp3 | H2 database data (separate, persistent) | ~$0.8/mo |
| Elastic IP | Stable public IP | $0 when attached |
| ECR (x2) | Docker image storage | ~$0.10/GB |
| VPC, IGW, SG | Networking | $0 |
| **Total** | | **~$11/mo** ($0 on free tier) |

### Deployment workflow

```bash
# 1. Configure AWS credentials
aws configure --profile rama-deployer

# 2. Terraform init
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars — set admin_emails

# 3. Create ECR repositories first
terraform apply -target=aws_ecr_repository.backend -target=aws_ecr_repository.frontend

# 4. Build and push Docker images
cd ../..
./mvnw -f rama-backend/pom.xml package -DskipTests
docker build -t rama-backend rama-backend/
docker build -t rama-frontend rama-frontend/

# Tag and push (use ECR URLs from terraform output)
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=eu-central-1
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
docker tag rama-backend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/rama-backend:latest
docker tag rama-frontend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/rama-frontend:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/rama-backend:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/rama-frontend:latest

# 5. Update terraform.tfvars with the ECR image URIs, then deploy
cd infrastructure/terraform
terraform apply
```

### Accessing the instance (no SSH key needed)

```bash
# Shell via SSM Session Manager (use the ssm_connect_command output)
aws ssm start-session --target <instance-id> --region eu-central-1

# View application logs
docker compose -f /home/ec2-user/docker-compose.yml logs -f
```

### Updating images (redeployment)

Update `backend_image` / `frontend_image` tags in `terraform.tfvars`, then:
```bash
terraform apply
```
Terraform replaces the EC2 instance; the separate EBS data volume is detached and reattached automatically — H2 data is preserved.

### ⚠️ Important notes

- **`backend_desired_count = 1` always** — H2 file mode does not support concurrent writers
- **H2 console is disabled in prod** — do not re-enable without network restrictions
- **TLS (HTTPS)** — not configured by default. Run `certbot` on the instance or add CloudFront in front
- **EBS snapshots** — snapshot `h2_ebs_volume_id` before major changes or deployments

### Debug iaws deployment

login to the instance using SSM Session Manager:
terraform output ssm_connect_command
```bash
aws ssm start-session --target i-09821001789451c48 --region eu-central-1 --profile rama-deployer

verify logs

sudo tail -50 /var/log/cloud-init-output.log

build and push images to ECR (use ECR URLs from terraform output)
AWS_ACCOUNT_ID=886121091893 \
 AWS_REGION=eu-central-1 \
 AWS_PROFILE=rama-deployer \
 ./scripts/push-to-ecr.sh latest

```

Then on the EC2 instance, pull and restart the backend:
aws ssm start-session --target i-09821001789451c48 --region eu-central-1 --profile rama-deployer

aws ecr get-login-password --region eu-central-1 \
| docker login --username AWS --password-stdin \
886121091893.dkr.ecr.eu-central-1.amazonaws.com

cd /home/ec2-user
docker compose pull backend
docker compose up -d backend