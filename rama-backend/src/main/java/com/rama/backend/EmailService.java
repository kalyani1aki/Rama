package com.rama.backend;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserService userService;

    @Value("${app.website.url}")
    private String websiteUrl;

    public EmailService(JavaMailSender mailSender, UserService userService) {
        this.mailSender = mailSender;
        this.userService = userService;
    }

    public void sendOrderConfirmation(Order order) {
        boolean isGoogleUser = userService.isGoogleUser(order.getUserEmail());
        String subject = "Order Confirmation - Mangoes Bern";
        String content = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #28a745;'>Thank you for your order!</h2>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>We have received your order. We will get back to you shortly with the <strong>exact pickup date</strong>.</p>" +
                "<div style='background-color: #fff; padding: 15px; border-radius: 5px; border: 1px solid #eee; margin: 20px 0;'>" +
                "<h3>Order Details:</h3>" +
                "<p><strong>Email:</strong> %s</p>" +
                "<p><strong>Quantity:</strong> %d</p>" +
                "<p><strong>Pickup Location:</strong> %s</p>" +
                "</div>" +
                "<p>%s</p>" +
                "<p>If you have any questions, please contact us at <a href='mailto:mangoes.bern@gmail.com'>mangoes.bern@gmail.com</a>.</p>" +
                "<p>Best regards,<br>Dilli Prasad Ramannagari</p>" +
                "</div></body></html>",
                order.getName(),
                order.getUserEmail(),
                order.getQuantity(),
                order.getPickupLocation(),
                getWebsiteLinks(isGoogleUser)
        );
        sendHtmlEmail(order.getUserEmail(), subject, content);
    }

    public void sendOrderUpdateNotification(Order order) {
        boolean isGoogleUser = userService.isGoogleUser(order.getUserEmail());
        String subject = "Order Updated - Mangoes Bern";
        String content = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #007bff;'>Order Updated</h2>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>Your order has been successfully updated. We will get back to you shortly with the <strong>exact pickup date</strong>.</p>" +
                "<div style='background-color: #fff; padding: 15px; border-radius: 5px; border: 1px solid #eee; margin: 20px 0;'>" +
                "<h3>New Order Details:</h3>" +
                "<p><strong>Email:</strong> %s</p>" +
                "<p><strong>Quantity:</strong> %d</p>" +
                "<p><strong>Pickup Location:</strong> %s</p>" +
                "</div>" +
                "<p>%s</p>" +
                "<p>Best regards,<br>Dilli Prasad Ramannagari</p>" +
                "</div></body></html>",
                order.getName(),
                order.getUserEmail(),
                order.getQuantity(),
                order.getPickupLocation(),
                getWebsiteLinks(isGoogleUser)
        );
        sendHtmlEmail(order.getUserEmail(), subject, content);
    }

    public void sendOrderDeletionNotification(Order order) {
        String subject = "Order Cancelled - Mangoes Bern";
        String content = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #fff3cd; padding: 20px; border-radius: 10px; border: 1px solid #ffeeba;'>" +
                "<h2 style='color: #856404;'>Order Cancelled</h2>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>Your order has been cancelled/deleted as requested.</p>" +
                "<p>To place new order visit: <a href='%s'>%s</a></p>" +
                "<p>If you did not request this, please contact us immediately at <a href='mailto:mangoes.bern@gmail.com'>mangoes.bern@gmail.com</a>.</p>" +
                "<p>Best regards,<br>Dilli Prasad Ramannagari</p>" +
                "</div></body></html>",
                order.getName(), websiteUrl, websiteUrl
        );
        sendHtmlEmail(order.getUserEmail(), subject, content);
    }

    private String getWebsiteLinks(boolean isGoogleUser) {
        if (isGoogleUser) {
            return String.format("<p>To view/update your order visit: <a href='%s'>%s</a></p>", websiteUrl, websiteUrl);
        } else {
            return String.format("<p>For additional information visit: <a href='%s'>%s</a></p>", websiteUrl, websiteUrl);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("mangoes.bern@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}
