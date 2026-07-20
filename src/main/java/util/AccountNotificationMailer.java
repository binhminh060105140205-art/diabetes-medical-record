package util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sends account credentials after creation without delaying the web request. */
public final class AccountNotificationMailer {
    private static final Logger LOG = Logger.getLogger(AccountNotificationMailer.class.getName());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "account-mailer");
        thread.setDaemon(true);
        return thread;
    });

    private AccountNotificationMailer() {}

    public static boolean sendAsync(String recipient, String fullName, String username,
                                    String temporaryPassword, String role) {
        String mailUser = config("MAIL_USERNAME", "GMAIL_USERNAME");
        String mailPassword = config("MAIL_PASSWORD", "GMAIL_APP_PASSWORD");
        if (blank(recipient) || blank(mailUser) || blank(mailPassword)) return false;
        EXECUTOR.submit(() -> send(recipient.trim(), fullName, username,
                temporaryPassword, role, mailUser, mailPassword));
        return true;
    }

    private static void send(String recipient, String fullName, String username,
                             String temporaryPassword, String role,
                             String mailUser, String mailPassword) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", configOrDefault("MAIL_STARTTLS", "true"));
        properties.put("mail.smtp.starttls.required", configOrDefault("MAIL_STARTTLS_REQUIRED", "true"));
        properties.put("mail.smtp.host", configOrDefault("MAIL_HOST", "smtp.gmail.com"));
        properties.put("mail.smtp.port", configOrDefault("MAIL_PORT", "587"));
        properties.put("mail.smtp.ssl.trust", configOrDefault("MAIL_HOST", "smtp.gmail.com"));
        properties.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUser, mailPassword);
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailUser,
                    configOrDefault("MAIL_FROM_NAME", "DiaCare"), StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
            message.setSubject("Thông tin tài khoản DiaCare", StandardCharsets.UTF_8.name());
            String safeName = blank(fullName) ? "bạn" : fullName.trim();
            message.setText("Xin chào " + safeName + ",\n\n"
                    + "Tài khoản DiaCare của bạn đã được tạo.\n"
                    + "Vai trò: " + roleLabel(role) + "\n"
                    + "Tên đăng nhập: " + username + "\n"
                    + "Mật khẩu tạm thời: " + temporaryPassword + "\n\n"
                    + "Vui lòng đăng nhập, đổi mật khẩu tạm thời và bảo mật thông tin tài khoản.\n\nDiaCare",
                    StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Không thể gửi email cấp tài khoản tới " + recipient, ex);
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static String configOrDefault(String name, String fallback) {
        String value = config(name);
        return value.isBlank() ? fallback : value;
    }

    private static String config(String name, String... aliases) {
        String value = env(name);
        if (!value.isBlank()) return value;
        value = System.getProperty(name, "").trim();
        if (!value.isBlank()) return value;
        Map<String, String> dotenv = readDotenv();
        value = dotenv.getOrDefault(name, "").trim();
        if (!value.isBlank()) return value;
        for (String alias : aliases) {
            value = env(alias);
            if (value.isBlank()) value = System.getProperty(alias, "").trim();
            if (value.isBlank()) value = dotenv.getOrDefault(alias, "").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static Map<String, String> readDotenv() {
        Path file = Path.of(".env");
        if (!Files.isRegularFile(file)) return Map.of();
        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int separator = trimmed.indexOf('=');
                if (separator <= 0) continue;
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (Exception error) {
            LOG.log(Level.FINE, "Không đọc được cấu hình .env", error);
        }
        return values;
    }

    private static String roleLabel(String role) {
        return switch (role == null ? "" : role) {
            case "ADMIN" -> "Quản trị viên";
            case "STAFF" -> "Nhân viên tiếp nhận";
            case "DOCTOR" -> "Bác sĩ";
            case "PATIENT" -> "Bệnh nhân";
            default -> "Người dùng";
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
