package com.nivedha.pathigai.user.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_id")
    private Long emailId;

    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false)
    private EmailType emailType;

    @Column(name = "related_user_id")
    private Long relatedUserId;

    @Column(name = "related_invitation_id")
    private Long relatedInvitationId;

    @Column(name = "sent", nullable = false)
    @Builder.Default
    private Boolean sent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EmailType {
        INVITATION,
        PASSWORD_RESET,
        NOTIFICATION
    }

    // Helper methods
    public boolean isDelivered() {
        return Boolean.TRUE.equals(sent) && sentAt != null;
    }

    public boolean hasFailed() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public boolean canRetry() {
        return retryCount < 3 && !isDelivered();
    }

    public void markAsSent() {
        this.sent = true;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String error) {
        this.sent = false;
        this.errorMessage = error;
        this.retryCount++;
    }
}
