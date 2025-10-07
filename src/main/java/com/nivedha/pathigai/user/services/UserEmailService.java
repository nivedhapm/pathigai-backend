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
     * Send a queued email using JavaMailSender
     */
    @Transactional
    public void sendQueuedEmail(EmailOutbox emailOutbox) {
        try {
            log.info("📤 Sending email via JavaMailSender to: {}", emailOutbox.getRecipientEmail());

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

            log.info("✅ Email sent successfully via JavaMailSender to: {}", emailOutbox.getRecipientEmail());

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
                <link href="https://fonts.googleapis.com/css2?family=Rubik:wght@400;700;800&display=swap" rel="stylesheet">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo-container { margin-bottom: 20px; }
                    h1, h2, h3 { font-family: 'Rubik', sans-serif; }
                    .welcome { font-size: 20px; color: #333; margin: 10px 0; font-family: 'Rubik', sans-serif; font-weight: 700; }
                    .subtitle { font-size: 14px; color: #666; margin-bottom: 30px; font-family: Arial, sans-serif; }
                    .greeting-text { color: #000000; font-family: Arial, sans-serif; }
                    .credentials-box { 
                        background: #f8f9fa; 
                        border: 2px solid #e9ecef; 
                        border-radius: 10px; 
                        padding: 25px; 
                        margin: 25px 0; 
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .credentials-heading { 
                        font-family: 'Rubik', sans-serif; 
                        font-size: 18px; 
                        font-weight: 700; 
                        color: #2c5aa0; 
                        margin: 0 0 20px 0; 
                        text-align: center;
                    }
                    .credential-row { 
                        display: flex; 
                        align-items: center; 
                        margin: 15px 0; 
                        font-family: Arial, sans-serif;
                    }
                    .credential-label { 
                        font-weight: bold; 
                        color: #333; 
                        min-width: 140px; 
                        font-family: Arial, sans-serif;
                    }
                    .credential-value { 
                        font-family: 'Courier New', monospace; 
                        background: #e9ecef; 
                        padding: 8px 12px; 
                        border-radius: 6px; 
                        color: #2c5aa0; 
                        font-weight: 600;
                        flex: 1;
                    }
                    .security-note { background: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; font-size: 14px; font-family: Arial, sans-serif; }
                    .steps { margin: 20px 0; font-family: Arial, sans-serif; }
                    .step { margin: 8px 0; font-family: Arial, sans-serif; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; font-family: Arial, sans-serif; }
                    .button { 
                        display: inline-block !important; 
                        background-color: #2c5aa0 !important; 
                        color: #ffffff !important; 
                        padding: 12px 30px !important; 
                        text-decoration: none !important; 
                        border-radius: 5px !important; 
                        margin: 20px 0 !important;
                        font-weight: bold !important;
                        font-size: 16px !important;
                        font-family: 'Rubik', sans-serif !important;
                    }
                    .button:visited, .button:hover, .button:active, .button:link { 
                        color: #ffffff !important; 
                        background-color: #2c5aa0 !important; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo-container" style="display: flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <img src="https://raw.githubusercontent.com/nivedhapm/pathigai-logo/main/pathigai-logo.png" 
                                 alt="Pathigai Logo" 
                                 width="40" 
                                 height="50" 
                                 style="border: none; margin-right: 10px;" />
                            <h2 style="margin: 0; font-size: 24px; font-family: 'Rubik', sans-serif; font-weight: 800; color: #2c5aa0;">PATHIGAI</h2>
                        </div>
                        <h1 class="welcome">Welcome to %s!</h1>
                        <p class="subtitle">Your account has been created successfully</p>
                    </div>

                    <p class="greeting-text">Hello <strong>%s</strong>,</p>
                    
                    <p class="greeting-text">Great news! Your Pathigai account has been created by <strong>%s</strong>. You can now access the platform and start your journey with us.</p>

                    <div class="credentials-box">
                        <h3 class="credentials-heading">Your Login Credentials</h3>
                        <div class="credential-row">
                            <span class="credential-label">Email:</span>
                            <span class="credential-value">%s</span>
                        </div>
                        <div class="credential-row">
                            <span class="credential-label">Temporary Password:</span>
                            <span class="credential-value">%s</span>
                        </div>
                    </div>

                    <div class="security-note">
                        <strong>🔒 Important Security Note:</strong><br>
                        This is a temporary password. You will be required to change it upon your first login for security purposes.
                    </div>

                    <div class="steps">
                        <h3>📋 Steps to Get Started</h3>
                        <div class="step">1. Visit the Pathigai login page</div>
                        <div class="step">2. Enter your email and temporary password</div>
                        <div class="step">3. Create a new secure password when prompted</div>
                        <div class="step">4. Complete your profile setup</div>
                        <div class="step">5. Start exploring the platform features</div>
                    </div>

                    <div style="text-align: center;">
                        <a href="http://localhost:5173/login" class="button">Login to Pathigai</a>
                    </div>

                    <div class="security-note">
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
            temporaryPassword
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
