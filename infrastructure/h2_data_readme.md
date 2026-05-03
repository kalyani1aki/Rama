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