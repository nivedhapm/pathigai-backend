package com.nivedha.pathigai.auth.services.external;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * SendGrid Email Service - Uses HTTP API instead of SMTP
 * This bypasses DigitalOcean's SMTP port blocking
 */
@Service
@Slf4j
public class SendGridEmailService {

    @Value("${app.sendgrid.api-key:}")
    private String sendGridApiKey;

    @Value("${app.sendgrid.enabled:false}")
    private boolean sendGridEnabled;

    @Value("${app.mail.from:noreply@pathigai.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Pathigai}")
    private String fromName;

    /**
     * Send email using SendGrid HTTP API
     */
    public void sendEmail(String to, String subject, String text) {
        if (!sendGridEnabled) {
            log.info("SendGrid disabled. Would send email to {}: {}", to, subject);
            return;
        }

        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            log.error("SendGrid API key not configured!");
            throw new RuntimeException("SendGrid API key not configured");
        }

        try {
            log.info("Sending email via SendGrid to: {}", to);

            Email from = new Email(fromEmail, fromName);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", text);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("✅ Email sent successfully via SendGrid to: {} (Status: {})", to, response.getStatusCode());
            } else {
                log.error("❌ SendGrid API returned error. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("SendGrid API error: " + response.getStatusCode());
            }

        } catch (IOException e) {
            log.error("❌ Failed to send email via SendGrid to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via SendGrid", e);
        }
    }

    /**
     * Send HTML email using SendGrid HTTP API
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!sendGridEnabled) {
            log.info("SendGrid disabled. Would send HTML email to {}: {}", to, subject);
            return;
        }

        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            log.error("SendGrid API key not configured!");
            throw new RuntimeException("SendGrid API key not configured");
        }

        try {
            log.info("Sending HTML email via SendGrid to: {}", to);

            Email from = new Email(fromEmail, fromName);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("✅ HTML email sent successfully via SendGrid to: {} (Status: {})",
                    to, response.getStatusCode());
            } else {
                log.error("❌ SendGrid API returned error. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("SendGrid API error: " + response.getStatusCode());
            }

        } catch (IOException e) {
            log.error("❌ Failed to send HTML email via SendGrid to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email via SendGrid", e);
        }
    }
}

