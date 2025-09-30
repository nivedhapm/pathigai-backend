package com.nivedha.pathigai.user.services;

import com.nivedha.pathigai.user.entities.EmailOutbox;
import com.nivedha.pathigai.user.repositories.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessorService {

    private final EmailOutboxRepository emailOutboxRepository;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.mock-send:true}")
    private boolean mockSend;

    /**
     * Process a single email immediately
     */
    @Async
    @Transactional
    public boolean processEmail(EmailOutbox email) {
        try {
            log.info("📧 Processing email ID: {} to {}", email.getEmailId(), email.getRecipientEmail());

            if (!emailEnabled) {
                log.info("📧 Email sending is disabled - marking as sent for development");
                email.markAsSent();
                emailOutboxRepository.save(email);
                return true;
            }

            if (mockSend) {
                // Mock email sending for development/testing
                log.info("📧 MOCK: Sending {} email to {}", email.getEmailType(), email.getRecipientEmail());
                log.info("📧 MOCK: Subject: {}", email.getSubject());
                log.debug("📧 MOCK: Body length: {} characters", email.getBody().length());

                // Simulate email sending delay
                Thread.sleep(100);

                email.markAsSent();
                emailOutboxRepository.save(email);

                log.info("✅ MOCK: Email sent successfully to {}", email.getRecipientEmail());
                return true;
            }

            // TODO: Implement actual email sending logic here
            // This is where you would integrate with your email service provider
            // Examples: SendGrid, AWS SES, SMTP, etc.

            boolean sent = sendEmailViaProvider(email);

            if (sent) {
                email.markAsSent();
                log.info("✅ Email sent successfully to {}", email.getRecipientEmail());
            } else {
                email.markAsFailed("Email service provider failed");
                log.error("❌ Failed to send email to {}", email.getRecipientEmail());
            }

            emailOutboxRepository.save(email);
            return sent;

        } catch (Exception e) {
            log.error("❌ Error processing email ID {}: {}", email.getEmailId(), e.getMessage(), e);
            email.markAsFailed("Processing error: " + e.getMessage());
            emailOutboxRepository.save(email);
            return false;
        }
    }

    /**
     * Process pending emails in batch (scheduled task)
     */
    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    @Transactional
    public void processPendingEmails() {
        try {
            List<EmailOutbox> pendingEmails = emailOutboxRepository.findPendingEmails();

            if (pendingEmails.isEmpty()) {
                log.debug("📧 No pending emails to process");
                return;
            }

            log.info("📧 Processing {} pending emails", pendingEmails.size());

            for (EmailOutbox email : pendingEmails) {
                try {
                    processEmail(email);
                } catch (Exception e) {
                    log.error("❌ Failed to process email ID {}: {}", email.getEmailId(), e.getMessage());
                }
            }

            log.info("✅ Completed processing {} pending emails", pendingEmails.size());

        } catch (Exception e) {
            log.error("❌ Error in scheduled email processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Retry failed emails (scheduled task)
     */
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    @Transactional
    public void retryFailedEmails() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24); // Only retry emails from last 24 hours
            List<EmailOutbox> retryableEmails = emailOutboxRepository.findRetryableEmails(since);

            if (retryableEmails.isEmpty()) {
                log.debug("📧 No failed emails to retry");
                return;
            }

            log.info("📧 Retrying {} failed emails", retryableEmails.size());

            for (EmailOutbox email : retryableEmails) {
                try {
                    log.info("🔄 Retrying email ID {} (attempt {})", email.getEmailId(), email.getRetryCount() + 1);
                    processEmail(email);
                } catch (Exception e) {
                    log.error("❌ Failed to retry email ID {}: {}", email.getEmailId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("❌ Error in email retry processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email via actual email service provider
     * TODO: Replace this with your actual email service integration
     */
    private boolean sendEmailViaProvider(EmailOutbox email) {
        try {
            // TODO: Implement your email service provider integration here
            // Examples:

            // For SendGrid:
            // return sendViaSendGrid(email);

            // For AWS SES:
            // return sendViaAwsSes(email);

            // For SMTP:
            // return sendViaSmtp(email);

            // For now, return true for development
            log.info("📧 TODO: Implement actual email sending for production");
            return true;

        } catch (Exception e) {
            log.error("❌ Email provider error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get email statistics
     */
    public EmailStats getEmailStats() {
        try {
            long pending = emailOutboxRepository.countPendingEmails();
            long recentSent = emailOutboxRepository.countRecentSentEmails(LocalDateTime.now().minusHours(24));

            return EmailStats.builder()
                    .pendingCount(pending)
                    .sentLast24Hours(recentSent)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting email stats: {}", e.getMessage(), e);
            return EmailStats.builder().build();
        }
    }

    /**
     * Email statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EmailStats {
        private long pendingCount;
        private long sentLast24Hours;
    }
}
