Below changed should be rolled back in prod

✓  Edit  security_groups.tf → Accepted (+8, -0)

      20     cidr_blocks = ["0.0.0.0/0"]
      21   }
      22
      23 + ingress {
      24 +   from_port   = 8080
      25 +   to_port     = 8080
      26 +   protocol    = "tcp"
      27 +   cidr_blocks = ["0.0.0.0/0"]
      28 +   description = "H2 Console (Debug only)"
      29 + }
      30 +
      31   egress {
      32     from_port   = 0
      33     to_port     = 0

✓  Edit  user_data.sh.tpl → Accepted (+4, -1)

      59   services:
      60     backend:
      61       image: ${backend_image}
      62 +     ports:
      63 +       - "8080:8080"
      64       environment:
      65         - SERVER_PORT=8080
      66         - SPRING_DATASOURCE_URL=jdbc:h2:file:/data/ramadb
      65 -       - SPRING_H2_CONSOLE_ENABLED=false
      67 +       - SPRING_H2_CONSOLE_ENABLED=true
      68 +       - SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS=true
      69         - APP_ADMIN_EMAILS=${admin_emails}
      70       volumes:
      71         - /data:/data

How to apply and access:

1. Deploy the changes:
   Run terraform apply from the infrastructure/terraform directory. This will update the Security Group and replace
   the EC2 instance with the new H2 console settings.

2. Access the Console:
   Open your browser and navigate to:
   http://<your-ec2-elastic-ip>:8080/h2-console

3. Login Settings:
    * JDBC URL: jdbc:h2:file:/data/ramadb
    * User Name: sa
    * Password: (leave empty)

### Secure SSH Tunnel (No Port Opening Required)
If you want to keep port 8080 closed on the EC2 Security Group for maximum security:
1. Keep the security group closed (remove the 8080 rule).
2. Run this command on your local machine:

1    ssh -i your-key.pem -L 8080:localhost:8080 ec2-user@<EC2_PUBLIC_IP>
3. Now access the console at http://localhost:8080/h2-console on your local machine. H2 will see the connection as coming from "localhost" and won't require the web-allow-others
   setting.

2. If you don't have a key (SSM recommended)
   Looking at your variables.tf, the key_name defaults to an empty string, which means you might be using AWS SSM Session Manager instead of traditional SSH.

If you don't have a .pem file, you can still create a secure tunnel using the AWS CLI and the Session Manager plugin:

1 # Tunnel port 8080 from EC2 to your local machine via SSM

aws ssm start-session --target i-04eb187fc477ed8b3  --region eu-central-1 --document-name AWS-StartPortForwardingSession --profile rama-deployer --parameters '{\"portNumber\":[\"8080\"],\"localPortNumber\":[\"8080\"]}'
nano /home/ec2-user/docker-compose.yml

Find the backend: section and add the ports: lines as shown below:
1   backend:
2     image: ...
3     environment:
4       - SERVER_PORT=8080
        **- SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS=true**
5       ...
**-     ports:
7       - "127.0.0.1:8080:8080"**  # <--- ADD THIS LINE

2 cd /home/ec2-user
3 docker compose up -d

Open http://localhost:8080/h2-console and use:
- JDBC URL: jdbc:h2:file:/data/ramadb (The path inside the container)
- User: sa
- Password: (leave blank)