package com.nivedha.pathigai.user.repositories;

import com.nivedha.pathigai.user.entities.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    /**
     * Find emails that need to be sent (not sent and can retry)
     */
    @Query("SELECT e FROM EmailOutbox e WHERE e.sent = false AND e.retryCount < 3")
    List<EmailOutbox> findPendingEmails();

    /**
     * Find emails by type
     */
    List<EmailOutbox> findByEmailType(EmailOutbox.EmailType emailType);

    /**
     * Find emails by recipient
     */
    List<EmailOutbox> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    /**
     * Find emails for a specific user
     */
    List<EmailOutbox> findByRelatedUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find failed emails that can be retried
     */
    @Query("SELECT e FROM EmailOutbox e WHERE e.sent = false AND e.retryCount < 3 AND e.createdAt > :since")
    List<EmailOutbox> findRetryableEmails(@Param("since") LocalDateTime since);

    /**
     * Count pending emails
     */
    @Query("SELECT COUNT(e) FROM EmailOutbox e WHERE e.sent = false")
    long countPendingEmails();

    /**
     * Count sent emails in the last 24 hours
     */
    @Query("SELECT COUNT(e) FROM EmailOutbox e WHERE e.sent = true AND e.sentAt > :since")
    long countRecentSentEmails(@Param("since") LocalDateTime since);
}
