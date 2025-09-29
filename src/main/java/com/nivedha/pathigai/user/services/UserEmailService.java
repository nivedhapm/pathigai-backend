package com.nivedha.pathigai.user.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.user.entities.EmailOutbox;
import com.nivedha.pathigai.user.repositories.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmailService {

    private final JavaMailSender mailSender;
    private final EmailOutboxRepository emailOutboxRepository;

    @Value("${app.mail.from:noreply@pathigai.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Pathigai}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send welcome email to newly created user
     */
    @Async
    public void sendWelcomeEmail(User newUser, String temporaryPassword, User createdBy) {
        try {
            log.info("📧 Preparing welcome email for user: {}", newUser.getEmail());

            String subject = "Welcome to Pathigai - Your Account is Ready!";
            String body = generateWelcomeEmailBody(newUser, temporaryPassword, createdBy);

            // Queue email for sending
            EmailOutbox email = EmailOutbox.builder()
                    .recipientEmail(newUser.getEmail())
                    .subject(subject)
                    .body(body)
                    .emailType(EmailOutbox.EmailType.INVITATION)
                    .relatedUser(newUser)
                    .sent(false)
                    .build();

            emailOutboxRepository.save(email);
            log.info("📬 Email queued for user: {}", newUser.getEmail());

            // Send immediately if email is enabled
            if (emailEnabled) {
                sendQueuedEmail(email);
            } else {
                log.info("📧 Email sending is disabled - email queued only");
            }

        } catch (Exception e) {
            log.error("❌ Error preparing welcome email for user {}: {}", newUser.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Send a queued email
     */
    @Transactional
    public void sendQueuedEmail(EmailOutbox emailOutbox) {
        try {
            log.info("📤 Sending email to: {}", emailOutbox.getRecipientEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailOutbox.getRecipientEmail());
            helper.setSubject(emailOutbox.getSubject());
            helper.setText(emailOutbox.getBody(), true); // true = HTML

            mailSender.send(message);

            // Mark as sent
            emailOutbox.setSent(true);
            emailOutbox.setSentAt(LocalDateTime.now());
            emailOutbox.setErrorMessage(null);
            emailOutboxRepository.save(emailOutbox);

            log.info("✅ Email sent successfully to: {}", emailOutbox.getRecipientEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", emailOutbox.getRecipientEmail(), e.getMessage(), e);

            // Update error info
            emailOutbox.setRetryCount(emailOutbox.getRetryCount() + 1);
            emailOutbox.setErrorMessage(e.getMessage());
            emailOutboxRepository.save(emailOutbox);
        }
    }

    /**
     * Generate welcome email HTML body
     */
    private String generateWelcomeEmailBody(User newUser, String temporaryPassword, User createdBy) {
        String companyName = newUser.getCompany() != null ? newUser.getCompany().getCompanyName() : "Pathigai";
        String createdByName = createdBy != null ? createdBy.getFullName() : "Administrator";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Pathigai</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 28px; font-weight: bold; color: #2c5aa0; margin-bottom: 10px; }
                    .welcome { font-size: 24px; color: #333; margin-bottom: 20px; }
                    .credentials { background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #2c5aa0; margin: 20px 0; }
                    .credential-item { margin: 10px 0; }
                    .credential-label { font-weight: bold; color: #555; }
                    .credential-value { font-family: monospace; background: #e9ecef; padding: 5px 10px; border-radius: 4px; display: inline-block; }
                    .steps { margin: 30px 0; }
                    .step { margin: 15px 0; padding-left: 25px; position: relative; }
                    .step:before { content: counter(step-counter); counter-increment: step-counter; position: absolute; left: 0; top: 0; background: #2c5aa0; color: white; width: 18px; height: 18px; border-radius: 50%%; font-size: 12px; text-align: center; line-height: 18px; }
                    .steps { counter-reset: step-counter; }
                    .important { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 14px; color: #666; }
                    .button { display: inline-block; background: #2c5aa0; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🎯 Pathigai</div>
                        <h1 class="welcome">Welcome to %s!</h1>
                        <p>Your account has been created successfully</p>
                    </div>

                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>Great news! Your Pathigai account has been created by <strong>%s</strong>. You can now access the platform and start your journey with us.</p>

                    <div class="credentials">
                        <h3>🔑 Your Login Credentials</h3>
                        <div class="credential-item">
                            <span class="credential-label">Email:</span> 
                            <span class="credential-value">%s</span>
                        </div>
                        <div class="credential-item">
                            <span class="credential-label">Temporary Password:</span> 
                            <span class="credential-value">%s</span>
                        </div>
                       
                    </div>

                    <div class="important">
                        <strong>🔒 Important Security Note:</strong><br>
                        This is a temporary password. You will be required to change it upon your first login for security purposes.
                    </div>

                    <div class="steps">
                        <h3>📋 Steps to Get Started</h3>
                        <div class="step">Visit the Pathigai login page</div>
                        <div class="step">Enter your email and temporary password</div>
                        <div class="step">Create a new secure password when prompted</div>
                        <div class="step">Complete your profile setup</div>
                        <div class="step">Start exploring the platform features</div>
                    </div>

                    <div style="text-align: center;">
                        <a href="http://localhost:3000/login" class="button">🚀 Login to Pathigai</a>
                    </div>

                    <div class="important">
                        <strong>📧 Need Help?</strong><br>
                        If you have any questions or need assistance, please contact your administrator or our support team.
                    </div>

                    <div class="footer">
                        <p>This email was sent automatically by the Pathigai system.</p>
                        <p>&copy; 2025 Pathigai. All rights reserved.</p>
                        <p><small>Please do not reply to this email. This mailbox is not monitored.</small></p>
                    </div>
                </div>
            </body>
            </html>
            """,
            companyName,
            newUser.getFullName(),
            createdByName,
            newUser.getEmail(),
            temporaryPassword,
            newUser.getPrimaryProfile() != null ? newUser.getPrimaryProfile().getName() : "Not assigned",
            newUser.getPrimaryRole() != null ? newUser.getPrimaryRole().getName() : "Not assigned"
        );
    }

    /**
     * Process pending emails (can be called by scheduler)
     */
    @Transactional
    public void processPendingEmails() {
        if (!emailEnabled) {
            log.debug("📧 Email sending is disabled - skipping pending emails");
            return;
        }

        try {
            List<EmailOutbox> pendingEmails = emailOutboxRepository.findBySentFalseAndRetryCountLessThanOrderByCreatedAtAsc(3);

            if (!pendingEmails.isEmpty()) {
                log.info("📬 Processing {} pending emails", pendingEmails.size());

                int successCount = 0;
                int failureCount = 0;

                for (EmailOutbox email : pendingEmails) {
                    try {
                        sendQueuedEmail(email);
                        if (email.getSent()) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                    } catch (Exception e) {
                        failureCount++;
                        log.error("❌ Failed to send email to {}: {}", email.getRecipientEmail(), e.getMessage());
                    }
                }

                log.info("🎉 Email processing completed: {} sent successfully, {} failed", successCount, failureCount);
            } else {
                log.debug("📪 No pending emails to process");
            }
        } catch (Exception e) {
            log.error("❌ Error processing pending emails: {}", e.getMessage(), e);
        }
    }

    /**
     * Get count of pending emails for monitoring
     */
    public long getPendingEmailCount() {
        return emailOutboxRepository.findBySentFalseAndRetryCountLessThanOrderByCreatedAtAsc(3).size();
    }

    /**
     * Process emails for a specific user (useful for bulk operations)
     */
    @Transactional
    public void processEmailsForUser(Integer userId) {
        try {
            List<EmailOutbox> userEmails = emailOutboxRepository.findByRelatedUserUserIdAndEmailType(
                userId, EmailOutbox.EmailType.INVITATION);

            for (EmailOutbox email : userEmails) {
                if (!email.getSent() && email.getRetryCount() < 3) {
                    sendQueuedEmail(email);
                }
            }
        } catch (Exception e) {
            log.error("❌ Error processing emails for user {}: {}", userId, e.getMessage(), e);
        }
    }
}
