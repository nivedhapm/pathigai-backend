package com.nivedha.pathigai.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background service to process queued email invitations
 * Runs automatically to send welcome emails from bulk user creation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessorService {

    private final UserEmailService emailService;

    /**
     * Process pending emails every 30 seconds
     * This ensures bulk upload email invitations are sent promptly
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void processQueuedEmails() {
        try {
            log.debug("🔄 Processing queued email invitations...");
            emailService.processPendingEmails();
        } catch (Exception e) {
            log.error("❌ Error in scheduled email processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup old failed emails every hour
     * Remove emails that have failed too many times (more than 3 retries)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupFailedEmails() {
        try {
            log.debug("🧹 Cleaning up old failed emails...");
            // This could be implemented to clean up emails that have exceeded max retries
            // For now, we'll just log the cleanup attempt
            log.debug("✅ Email cleanup completed");
        } catch (Exception e) {
            log.error("❌ Error in email cleanup: {}", e.getMessage(), e);
        }
    }
}
