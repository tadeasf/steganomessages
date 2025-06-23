package com.tadeasfort.steganomessages.service;

import com.tadeasfort.steganomessages.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.name}")
    private String appName;

    @Value("${app.url}")
    private String appUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Verify your email - " + appName);

            String verificationUrl = appUrl + "/auth/verify?token=" + user.getVerificationToken();
            String emailContent = String.format(
                    "Hello %s,\n\n" +
                            "Thank you for registering with %s!\n\n" +
                            "Please click the link below to verify your email address:\n" +
                            "%s\n\n" +
                            "This link will expire in 24 hours.\n\n" +
                            "If you didn't create an account with us, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "The %s Team",
                    user.getFullName(),
                    appName,
                    verificationUrl,
                    appName);

            message.setText(emailContent);
            mailSender.send(message);

            log.info("Verification email sent to: {}", user.getEmail());
        } catch (MailException e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Password Reset - " + appName);

            String resetUrl = appUrl + "/auth/reset-password?token=" + user.getPasswordResetToken();
            String emailContent = String.format(
                    "Hello %s,\n\n" +
                            "We received a request to reset your password for your %s account.\n\n" +
                            "Please click the link below to reset your password:\n" +
                            "%s\n\n" +
                            "This link will expire in 1 hour.\n\n" +
                            "If you didn't request a password reset, please ignore this email. Your password will remain unchanged.\n\n"
                            +
                            "Best regards,\n" +
                            "The %s Team",
                    user.getFullName(),
                    appName,
                    resetUrl,
                    appName);

            message.setText(emailContent);
            mailSender.send(message);

            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (MailException e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendWelcomeEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Welcome to " + appName + "!");

            String emailContent = String.format(
                    "Hello %s,\n\n" +
                            "Welcome to %s!\n\n" +
                            "Your email has been successfully verified and your account is now active.\n\n" +
                            "You can now start using our steganography tools to:\n" +
                            "• Hide secret messages in images\n" +
                            "• Extract hidden messages from images\n" +
                            "• Share your steganographic creations securely\n\n" +
                            "Visit %s to get started!\n\n" +
                            "Best regards,\n" +
                            "The %s Team",
                    user.getFullName(),
                    appName,
                    appUrl,
                    appName);

            message.setText(emailContent);
            mailSender.send(message);

            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (MailException e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            // Don't throw exception for welcome email failures
        }
    }
}