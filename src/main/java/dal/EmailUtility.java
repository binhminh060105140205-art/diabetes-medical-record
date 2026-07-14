package dal;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailUtility {
    private static final String SENDER_EMAIL = System.getenv().getOrDefault("MAIL_USERNAME", "");
    private static final String APP_PASSWORD = System.getenv().getOrDefault("MAIL_PASSWORD", "");

    public static void sendAccountDetails(String toEmail, String fullName, String username, String password, String role) {
        if (SENDER_EMAIL.isBlank() || APP_PASSWORD.isBlank()) {
            throw new IllegalStateException("Set MAIL_USERNAME and MAIL_PASSWORD before sending email");
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            
            message.setFrom(new InternetAddress(SENDER_EMAIL, "System Management", "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            String subject = "Account Activation Notification - Role: " + role;
            message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
            
            String htmlContent = "<div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px;'>"
                    + "<h3>Dear " + fullName + ",</h3>"
                    + "<p>Your personal account has been successfully created by the Administrator.</p>"
                    + "<p>Please use the credentials below to log in to the system:</p>"
                    + "<table style='border-collapse: collapse; width: 100%; max-width: 400px; margin-bottom: 20px;'>"
                    + "  <tr>"
                    + "    <td style='padding: 8px; border: 1px solid #ddd; font-weight: bold; background: #f9f9f9;'>Username:</td>"
                    + "    <td style='padding: 8px; border: 1px solid #ddd;'>" + username + "</td>"
                    + "  </tr>"
                    + "  <tr>"
                    + "    <td style='padding: 8px; border: 1px solid #ddd; font-weight: bold; background: #f9f9f9;'>Temporary Password:</td>"
                    + "    <td style='padding: 8px; border: 1px solid #ddd; color: #d9534f;'><code>" + password + "</code></td>"
                    + "  </tr>"
                    + "  <tr>"
                    + "    <td style='padding: 8px; border: 1px solid #ddd; font-weight: bold; background: #f9f9f9;'>System Role:</td>"
                    + "    <td style='padding: 8px; border: 1px solid #ddd;'>" + role + "</td>"
                    + "  </tr>"
                    + "</table>"
                    + "<p style='color: #555; font-style: italic;'>* For security purposes, please change your password immediately upon your first successful login.</p>"
                    + "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>"
                    + "<p style='font-size: 12px; color: #999;'>This is an automated notification from the system. Please do not reply directly to this email.</p>"
                    + "</div>";
            
            message.setContent(htmlContent, "text/html; charset=UTF-8");
            
            Transport.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.out.println("EmailUtility ERROR: " + e.getMessage());
        }
    }
}
