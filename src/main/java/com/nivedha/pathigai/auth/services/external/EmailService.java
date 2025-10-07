package com.nivedha.pathigai.auth.services.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@pathigai.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Pathigai}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.mail.use-mime:true}")
    private boolean useMimeMessage;

    public void sendEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            log.info("Email service disabled. Would send email to {}: Subject: {}, Body: {}", to, subject, text);
            return;
        }

        try {
            log.info("Sending email to: {} from: {}", to, fromEmail);

            if (useMimeMessage) {
                sendMimeEmail(to, subject, text);
            } else {
                sendSimpleEmail(to, subject, text);
            }

            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    private void sendMimeEmail(String to, String subject, String text)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // Set from address with custom name
        helper.setFrom(new InternetAddress(fromEmail, fromName));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false); // false = plain text, true = HTML

        // Add reply-to if different from from address
        helper.setReplyTo(fromEmail);

        mailSender.send(mimeMessage);
    }

    // Method for sending HTML emails (optional enhancement)
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Email service disabled. Would send HTML email to {}: Subject: {}", to, subject);
            return;
        }

        try {
            log.info("Sending HTML email to: {} from: {}", to, fromEmail);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content
            helper.setReplyTo(fromEmail);

            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}