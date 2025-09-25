package com.nivedha.pathigai.user.repositories;

import com.nivedha.pathigai.user.entities.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Integer> {

    List<EmailOutbox> findBySentFalseAndRetryCountLessThanOrderByCreatedAtAsc(Integer maxRetries);

    @Query("SELECT e FROM EmailOutbox e WHERE e.sent = false AND e.createdAt > :cutoffTime ORDER BY e.createdAt ASC")
    List<EmailOutbox> findPendingEmails(@Param("cutoffTime") LocalDateTime cutoffTime);

    List<EmailOutbox> findByRelatedUserUserIdAndEmailType(Integer userId, EmailOutbox.EmailType emailType);
}
