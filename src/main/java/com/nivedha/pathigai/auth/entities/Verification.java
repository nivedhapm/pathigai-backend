package com.nivedha.pathigai.auth.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Integer verificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false)
    private VerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "context", nullable = false)
    private VerificationContext context;

    @Column(name = "otp_hash", nullable = false, length = 128)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "resend_count", nullable = false)
    private Integer resendCount = 0;

    @Column(name = "last_resend")
    private LocalDateTime lastResend;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum VerificationType {
        SMS, EMAIL
    }

    public enum VerificationContext {
        SIGNUP, LOGIN, PASSWORD_RESET
    }
}