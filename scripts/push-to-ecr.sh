#!/usr/bin/env bash
# push-to-ecr.sh — Build and push rama-backend and rama-frontend images to ECR.
# Run this after `terraform apply` creates the ECR repositories.
#
# Usage:
#   cd /path/to/rama
#   AWS_ACCOUNT_ID=123456789012 AWS_REGION=eu-central-1 ./scripts/push-to-ecr.sh [TAG]
#
# TAG defaults to "latest".

set -euo pipefail

AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
AWS_REGION="${AWS_REGION:-eu-central-1}"
TAG="${1:-latest}"

BACKEND_REPO="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/rama-backend"
FRONTEND_REPO="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/rama-frontend"

echo "==> Authenticating Docker with ECR ..."
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin \
    "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo ""
echo "==> Building backend JAR ..."
(cd rama-backend && ./mvnw package -DskipTests -q)

echo ""
echo "==> Building and pushing backend image (${BACKEND_REPO}:${TAG}) ..."
docker build -t "${BACKEND_REPO}:${TAG}" rama-backend/
docker push "${BACKEND_REPO}:${TAG}"

echo ""
echo "==> Building and pushing frontend image (${FRONTEND_REPO}:${TAG}) ..."
docker build -t "${FRONTEND_REPO}:${TAG}" rama-frontend/
docker push "${FRONTEND_REPO}:${TAG}"

echo ""
echo "Done. Update terraform.tfvars with:"
echo "  backend_image  = \"${BACKEND_REPO}:${TAG}\""
echo "  frontend_image = \"${FRONTEND_REPO}:${TAG}\""
