package com.nivedha.pathigai.auth.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_fingerprint"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Device identification
    @Column(name = "device_fingerprint", nullable = false, length = 64)
    private String deviceFingerprint;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, length = 500)
    private String userAgent;

    // Token management
    @Column(name = "access_token_hash", nullable = false, length = 128)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "refresh_token_version", nullable = false)
    @Builder.Default
    private Integer refreshTokenVersion = 1;

    // Session lifecycle
    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "access_expires_at", nullable = false)
    private LocalDateTime accessExpiresAt;

    @Column(name = "refresh_expires_at", nullable = false)
    private LocalDateTime refreshExpiresAt;

    @Column(name = "last_used_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastUsedAt = LocalDateTime.now();

    // Session status
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason")
    private RevokeReason revokeReason;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enum for revoke reasons
    public enum RevokeReason {
        USER_LOGOUT,
        TOKEN_REFRESH,
        SECURITY_BREACH,
        MAX_SESSIONS_EXCEEDED,
        ADMIN_REVOKE
    }

    // Helper methods
    public boolean isExpired() {
        return refreshExpiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isAccessTokenExpired() {
        return accessExpiresAt.isBefore(LocalDateTime.now());
    }

    public void markAsRevoked(RevokeReason reason) {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
        this.revokeReason = reason;
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}