package com.melodyHub.service;

import com.melodyHub.config.AppConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {
    private static EmailService instance;

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromAddress;
    private final String fromName;
    private final boolean enabled;

    private EmailService() {
        this.smtpHost = AppConfig.get("smtp.host");
        this.smtpPort = AppConfig.getInt("smtp.port", 587);
        this.smtpUsername = AppConfig.get("smtp.username");
        this.smtpPassword = AppConfig.get("smtp.password");
        this.fromAddress = getOrDefault("smtp.from.address", "noreply@melodyhub.com");
        this.fromName = getOrDefault("smtp.from.name", "MelodyHub");
        this.enabled = smtpHost != null && !smtpHost.isBlank();
    }

    private static String getOrDefault(String key, String defaultValue) {
        String value = AppConfig.get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        if (!enabled) {
            System.out.println("[EmailService] SMTP not configured. Token for " + toEmail + ": " + token);
            return;
        }

        String resetLink = buildResetLink(token);
        String subject = "Reset your MelodyHub password";
        String htmlContent = buildPasswordResetHtml(toEmail, resetLink);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private String buildResetLink(String token) {
        String baseUrl = getOrDefault("app.base-url", "http://localhost:5173");
        return baseUrl + "/reset-password?token=" + token;
    }

    private String buildPasswordResetHtml(String email, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background: #121212; color: #EDE9E0; margin: 0; padding: 20px; }
                    .container { max-width: 480px; margin: 0 auto; background: #1a1a1a; border-radius: 12px; padding: 32px; border: 1px solid rgba(255,255,255,0.1); }
                    .logo { font-size: 24px; font-weight: 900; letter-spacing: 0.18em; color: #1DB954; margin-bottom: 24px; }
                    h1 { font-size: 22px; font-weight: 800; color: #FFFFFF; margin: 0 0 16px 0; }
                    p { font-size: 15px; line-height: 1.6; color: #AAAAAA; margin: 0 0 24px 0; }
                    .button { display: inline-block; background: #1DB954; color: #000000; font-weight: 900; font-size: 13px; padding: 14px 28px; border-radius: 50px; text-decoration: none; letter-spacing: 0.05em; }
                    .button:hover { background: #20ca5c; }
                    .note { font-size: 12px; color: #666666; margin-top: 20px; }
                    .footer { font-size: 12px; color: #555555; margin-top: 28px; border-top: 1px solid rgba(255,255,255,0.06); padding-top: 16px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">Melody Hub</div>
                    <h1>Reset your password</h1>
                    <p>Hi,</p>
                    <p>We received a request to reset the password for your MelodyHub account (<strong>%s</strong>). Click the button below to set a new password.</p>
                    <a href="%s" class="button">RESET PASSWORD</a>
                    <p class="note">If you didn't request a password reset, you can safely ignore this email.</p>
                    <div class="footer">This link expires in 60 minutes and can only be used once.</div>
                </div>
            </body>
            </html>
            """.formatted(email, resetLink);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("[EmailService] Password reset email sent to " + to);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}
