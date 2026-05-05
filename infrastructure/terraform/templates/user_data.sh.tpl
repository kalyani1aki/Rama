#!/bin/bash
set -e

# ── System packages ────────────────────────────────────────────────────────────
dnf update -y
dnf install -y docker

systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Docker Compose plugin
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/download/v2.29.0/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# ── Data volume (separate EBS — survives instance replacement) ─────────────────
# Nitro-based t3 instances present /dev/sdf as /dev/nvme1n1 or /dev/nvme2n1
echo "Waiting for data volume to attach..."
for i in $(seq 1 30); do
  # Check for common device names
  DATA_DEVICE=$(lsblk -no PATH | grep -E '/dev/nvme[1-9]n1|/dev/xvdf' | head -n 1)
  if [ -n "$DATA_DEVICE" ]; then 
    echo "Found device: $DATA_DEVICE"
    break 
  fi
  sleep 2
done

if [ -z "$DATA_DEVICE" ]; then
  echo "ERROR: Data volume not found after 60 seconds."
  exit 1
fi

# Only format if no filesystem exists
if ! blkid $DATA_DEVICE > /dev/null 2>&1; then
  echo "Formatting $DATA_DEVICE as ext4..."
  mkfs.ext4 $DATA_DEVICE
else
  echo "Existing filesystem detected on $DATA_DEVICE. Skipping format."
fi

mkdir -p /data
mount $DATA_DEVICE /data || { echo "ERROR: Failed to mount $DATA_DEVICE"; exit 1; }

# Add to fstab if not already present
if ! grep -q "/data" /etc/fstab; then
  echo "$DATA_DEVICE /data ext4 defaults,nofail 0 2" >> /etc/fstab
fi

# ── ECR login ──────────────────────────────────────────────────────────────────
aws ecr get-login-password --region ${region} | \
  docker login --username AWS --password-stdin ${ecr_registry}

# ── docker-compose.yml ─────────────────────────────────────────────────────────
mkdir -p /home/ec2-user
cat > /home/ec2-user/docker-compose.yml << 'COMPOSE'
services:
  backend:
    image: ${backend_image}
#    TODO remove port
    ports:
      - "8080:8080"
    environment:
      - SERVER_PORT=8080
      - SPRING_DATASOURCE_URL=jdbc:h2:file:/data/ramadb
#      TODO remove spring enabe for h2. clean next 2 lines
      - SPRING_H2_CONSOLE_ENABLED=true
      - SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS=true
      - APP_ADMIN_EMAILS=${admin_emails}
      - MAIL_PASSWORD=${mail_password}
      - WEBSITE_URL=${website_url}
    volumes:
      - /data:/data
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "3"
    healthcheck:
      test: ["CMD-SHELL", "bash -c 'echo > /dev/tcp/localhost/8080'"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s

  frontend:
    image: ${frontend_image}
    ports:
      - "80:80"
    environment:
      - BACKEND_URL=http://backend:8080
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "3"
COMPOSE

# ── Pull images and start ──────────────────────────────────────────────────────
cd /home/ec2-user
docker compose pull
docker compose up -d

# ── Systemd service — ensures containers restart after reboot ──────────────────
cat > /etc/systemd/system/rama.service << 'SERVICE'
[Unit]
Description=Rama Application (Docker Compose)
After=docker.service network-online.target
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ec2-user
ExecStartPre=/bin/bash -c 'aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${ecr_registry}'
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=300

[Install]
WantedBy=multi-user.target
SERVICE

systemctl enable rama
