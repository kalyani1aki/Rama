package com.rama.backend;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.List;

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

    public void sendWaitingListConfirmation(Order order) {
        boolean isGoogleUser = userService.isGoogleUser(order.getUserEmail());
        String subject = "Waiting List Confirmation - Mangoes Bern";
        String content = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #fff3cd; padding: 20px; border-radius: 10px; border: 1px solid #ffeeba;'>" +
                "<h2 style='color: #856404;'>You're on the Waiting List!</h2>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>Thank you for your interest. Our mangoes are currently sold out, but we have added you to our <strong>waiting list</strong>.</p>" +
                "<p>If more stock becomes available, we will confirm your order and notify you immediately.</p>" +
                "<div style='background-color: #fff; padding: 15px; border-radius: 5px; border: 1px solid #eee; margin: 20px 0;'>" +
                "<h3>Waiting List Details:</h3>" +
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

    public void sendOrderConfirmedFromWaitingList(Order order) {
        boolean isGoogleUser = userService.isGoogleUser(order.getUserEmail());
        String subject = "Order Confirmed! - Mangoes Bern";
        String content = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #28a745;'>Good news! Your order is confirmed.</h2>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>We are happy to inform you that we have enough stock to fulfill your order from the waiting list.</p>" +
                "<p>Your order is now <strong>CONFIRMED</strong>. We will get back to you shortly with the exact pickup date.</p>" +
                "<div style='background-color: #fff; padding: 15px; border-radius: 5px; border: 1px solid #eee; margin: 20px 0;'>" +
                "<h3>Order Details:</h3>" +
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
/*
    public void sendGenericEmail(List<String> emails, String subject, String content) {
        if (emails.isEmpty()) return;
        
        String htmlContent = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 10px; border: 1px solid #ddd;'>" +
                "%s" +
                "<p style='margin-top: 20px;'>Best regards,<br>Dilli Prasad Ramannagari</p>" +
                "<div style='margin-top: 20px; padding: 15px; background-color: #f1f8e9; border: 1px solid #c5e1a5; border-radius: 5px; color: #2e7d32; font-size: 0.9em;'>" +
                "<p style='margin-top: 0;'><strong>PS: General Notes on Mangoes</strong></p>" +
                "<ul style='margin-bottom: 0;'>" +
                "<li>Mangoes taste best when they are ripe.</li>" +
                "<li>Temperatures between 18 to 22 deg. C is best for ripening.</li>" +
                "<li>If your mangoes are already ripe, you can store them in the refrigerator to extend life.</li>" +
                "<li>Please open all boxes and enjoy the best Ripen mangoes first.</li>" +
                "</ul>" +
                "</div>" +
                "<div style='text-align: center; margin-top: 20px;'><img src='cid:mango_ripe_img' style='max-width: 100%%; border-radius: 10px;'></div>" +
                "</div></body></html>",
                content.replace("\n", "<br>")
        );
        
        sendHtmlEmailWithBcc(emails, subject, htmlContent);
    }*/

    public void sendPickupConfirmationEmail(List<String> toEmails, String location, String time, String contactPerson, String contactPhone) {
        if (toEmails.isEmpty()) return;

        String subject = "Pickup Confirmation (" + location + ")";
        String htmlContent = String.format(
                "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<div style='background-color: #e8f5e9; padding: 20px; border-radius: 10px; border: 1px solid #c8e6c9;'>" +
                "<h2 style='color: #2e7d32;'>Pickup Information</h2>" +
                "<p>Dear Customer,</p>" +
                "<p>We are happy to inform you that your mangoes are ready for pickup!</p>" +
                "<div style='background-color: #fff; padding: 15px; border-radius: 5px; border: 1px solid #eee; margin: 20px 0;'>" +
                "<h3>Details for your location:</h3>" +
                "<p><strong>Location:</strong> %s</p>" +
                "<p><strong>Pickup Date & Time:</strong> %s</p>" +
                "<p><strong>Contact Person:</strong> %s (%s)</p>" +
                "</div>" +
                "<div style='background-color: #fff; padding: 15px; border-radius: 5px; border: 1px solid #eee; margin: 20px 0;'>" +
                "<h3>Payment Details:</h3>" +
                "<p><strong>Price per box:</strong> 30 CHF</p>" +
                "<p><strong>Payment Method:</strong> Please pay via <strong>Cash</strong> or <strong>Twint</strong> to %s (%s)</p>" +
                "</div>" +
                "<p>Please ensure you arrive within the specified time slot. If you have any trouble finding the location, feel free to contact the person mentioned above.</p>" +
                "<p>For order details please visit: <a href='https://ramaswiss.ch'>https://ramaswiss.ch</a></p>" +
                "<p>For any other questions please contact email: <a href='mailto:mangoes.bern@gmail.com'>mangoes.bern@gmail.com</a></p>" +
                "<p>Best regards,<br>Dilli Prasad Ramannagari<br>0799466631</p>" +
                "<div style='margin-top: 20px; padding: 15px; background-color: #f1f8e9; border: 1px solid #c5e1a5; border-radius: 5px; color: #2e7d32; font-size: 0.9em;'>" +
                "<p style='margin-top: 0;'><strong>PS: General Notes on Mangoes</strong></p>" +
                "<ul style='margin-bottom: 0;'>" +
                "<li>Mangoes taste best when they are ripe.</li>" +
                "<li>Temperatures between 18 to 22 deg. C is best for ripening.</li>" +
                "<li>If your mangoes are already ripe, you can store them in the refrigerator to extend life.</li>" +
                "<li>Please open all boxes and enjoy the best Ripen mangoes first.</li>" +
                "</ul>" +
                "</div>" +
                "<div style='text-align: center; margin-top: 20px;'><img src='cid:mango_ripe_img' style='max-width: 100%%; border-radius: 10px;'></div>" +
                "</div></body></html>",
                location, time, contactPerson, contactPhone, contactPerson, contactPhone
        );

        sendHtmlEmailWithBcc(toEmails, subject, htmlContent);
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

    private void sendHtmlEmailWithBcc(List<String> bccEmails, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("mangoes.bern@gmail.com");
            // Set TO as sender so recipients don't see others
            helper.setTo("mangoes.bern@gmail.com");
            helper.setBcc(bccEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addInline("mango_ripe_img", new ClassPathResource("images/mango_ripe_img.png"));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email with BCC", e);
        }
    }
}
