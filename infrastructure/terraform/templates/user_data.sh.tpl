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
echo "Waiting for data volume to attach..."
for i in $(seq 1 30); do
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

if ! blkid $DATA_DEVICE > /dev/null 2>&1; then
  mkfs.ext4 $DATA_DEVICE
fi

mkdir -p /data
mount $DATA_DEVICE /data
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
  # Reverse Proxy with automatic HTTPS
  nginx-proxy:
    image: nginxproxy/nginx-proxy
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/tmp/docker.sock:ro
      - certs:/etc/nginx/certs:ro
      - vhost:/etc/nginx/vhost.d
      - html:/usr/share/nginx/html
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "10m"

  acme-companion:
    image: nginxproxy/acme-companion
    container_name: nginx-proxy-acme
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - certs:/etc/nginx/certs:rw
      - vhost:/etc/nginx/vhost.d:rw
      - html:/usr/share/nginx/html:rw
      - acme:/etc/acme.sh
    environment:
      - DEFAULT_EMAIL=${cert_email}
      - NGINX_PROXY_CONTAINER=nginx-proxy
    depends_on:
      - nginx-proxy
    restart: unless-stopped

  backend:
    image: ${backend_image}
    environment:
      - SERVER_PORT=8080
      - SPRING_DATASOURCE_URL=jdbc:h2:file:/data/ramadb
      - APP_ADMIN_EMAILS=${admin_emails}
      - MAIL_PASSWORD=${mail_password}
      - WEBSITE_URL=${website_url}
    volumes:
      - /data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "bash -c 'echo > /dev/tcp/localhost/8080'"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s

  frontend:
    image: ${frontend_image}
    environment:
      - BACKEND_URL=http://backend:8080
      - VIRTUAL_HOST=${domain_name},www.${domain_name}
      - VIRTUAL_PORT=80
      - LETSENCRYPT_HOST=${domain_name},www.${domain_name}
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped

volumes:
  certs:
  vhost:
  html:
  acme:
COMPOSE

# ── Pull images and start ──────────────────────────────────────────────────────
cd /home/ec2-user
docker compose pull
docker compose up -d

# ── Systemd service ────────────────────────────────────────────────────────────
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
