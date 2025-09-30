package com.nivedha.pathigai.user.services;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.user.entities.EmailOutbox;
import com.nivedha.pathigai.user.repositories.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmailService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailProcessorService emailProcessorService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${app.company.name:PathigAI}")
    private String companyName;

    /**
     * Send user invitation email with welcome message and temporary password
     */
    @Transactional
    public boolean sendUserInvitation(User user, String temporaryPassword) {
        try {
            log.info("📧 Preparing invitation email for user: {}", user.getEmail());

            // Generate email content
            String subject = generateInvitationSubject();
            String body = generateInvitationBody(user, temporaryPassword);

            // Create email outbox entry
            EmailOutbox email = EmailOutbox.builder()
                    .recipientEmail(user.getEmail())
                    .subject(subject)
                    .body(body)
                    .emailType(EmailOutbox.EmailType.INVITATION)
                    .relatedUserId(user.getUserId())
                    .sent(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save to outbox
            EmailOutbox savedEmail = emailOutboxRepository.save(email);
            log.info("📧 Email queued for sending: ID {}, Recipient: {}", savedEmail.getEmailId(), user.getEmail());

            // Process email immediately (you can make this asynchronous)
            return emailProcessorService.processEmail(savedEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send invitation email to {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate invitation email subject
     */
    private String generateInvitationSubject() {
        return String.format("Welcome to %s - Your Account Has Been Created", companyName);
    }

    /**
     * Generate invitation email body with welcome message and login instructions
     */
    private String generateInvitationBody(User user, String temporaryPassword) {
        String logoUrl = backendBaseUrl + "/images/pathigai-logo.svg";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Welcome to %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .logo { max-width: 150px; height: auto; margin-bottom: 10px; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .credentials { background-color: #e8f5e8; border: 1px solid #4CAF50; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .button { display: inline-block; background-color: #4CAF50; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .steps { background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; }
                    .step { margin: 10px 0; padding-left: 20px; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="%s" alt="Pathigai Logo" class="logo">
                        <h1>Welcome to %s!</h1>
                    </div>
                    
                    <div class="content">
                        <h2>Hello %s,</h2>
                        
                        <p>Congratulations! Your account has been successfully created in our system. We're excited to have you on board!</p>
                        
                        <div class="credentials">
                            <h3>🔐 Your Login Credentials:</h3>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Temporary Password:</strong> <code>%s</code></p>
                            <p><strong>Role:</strong> %s</p>
                            <p><strong>Profile:</strong> %s</p>
                        </div>
                        
                        <div class="steps">
                            <h3>📋 Next Steps - How to Login for the First Time:</h3>
                            <div class="step">1. Click the login button below or visit our platform</div>
                            <div class="step">2. Enter your email address: <strong>%s</strong></div>
                            <div class="step">3. Enter your temporary password: <strong>%s</strong></div>
                            <div class="step">4. Complete the verification process (SMS/Email OTP)</div>
                            <div class="step">5. You'll be prompted to create a new secure password</div>
                            <div class="step">6. Complete your profile setup</div>
                        </div>
                        
                        <div style="text-align: center;">
                            <a href="%s/login" class="button">🚀 Login to Your Account</a>
                        </div>
                        
                        <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <h4>🔒 Important Security Notes:</h4>
                            <ul>
                                <li>This is a temporary password - you'll be required to change it on first login</li>
                                <li>Keep your login credentials secure and don't share them with anyone</li>
                                <li>If you have any issues logging in, contact your administrator</li>
                            </ul>
                        </div>
                        
                        <p>If you have any questions or need assistance, please don't hesitate to reach out to our support team.</p>
                        
                        <p>Welcome aboard! 🎊</p>
                        
                        <p>Best regards,<br>
                        The %s Team</p>
                    </div>
                    
                    <div class="footer">
                        <p>This email was sent to %s because an account was created for you in %s.</p>
                        <p>If you believe this email was sent in error, please contact your administrator.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            companyName, // title
            logoUrl, // logo URL - NEW: replaces the emoji
            companyName, // header
            user.getFullName(), // greeting
            user.getEmail(), // email in credentials
            temporaryPassword, // password in credentials
            user.getPrimaryRole() != null ? user.getPrimaryRole().getName() : "Not Assigned", // role
            user.getPrimaryProfile() != null ? user.getPrimaryProfile().getName() : "Not Assigned", // profile
            user.getEmail(), // email in steps
            temporaryPassword, // password in steps
            frontendBaseUrl, // login URL
            companyName, // signature
            user.getEmail(), // footer email
            companyName // footer company
        );
    }

    /**
     * Send password reset email (for future use)
     */
    @Transactional
    public boolean sendPasswordResetEmail(User user, String resetToken) {
        try {
            log.info("📧 Preparing password reset email for user: {}", user.getEmail());

            String subject = "Password Reset Request - " + companyName;
            String body = generatePasswordResetBody(user, resetToken);

            EmailOutbox email = EmailOutbox.builder()
                    .recipientEmail(user.getEmail())
                    .subject(subject)
                    .body(body)
                    .emailType(EmailOutbox.EmailType.PASSWORD_RESET)
                    .relatedUserId(user.getUserId())
                    .sent(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            EmailOutbox savedEmail = emailOutboxRepository.save(email);
            return emailProcessorService.processEmail(savedEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate password reset email body
     */
    private String generatePasswordResetBody(User user, String resetToken) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2>Password Reset Request</h2>
                    <p>Hello %s,</p>
                    <p>We received a request to reset your password for your %s account.</p>
                    <p>Click the link below to reset your password:</p>
                    <p><a href="%s/reset-password?token=%s" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Reset Password</a></p>
                    <p>This link will expire in 1 hour for security reasons.</p>
                    <p>If you didn't request this password reset, please ignore this email.</p>
                    <p>Best regards,<br>The %s Team</p>
                </div>
            </body>
            </html>
            """,
            user.getFullName(),
            companyName,
            frontendBaseUrl,
            resetToken,
            companyName
        );
    }
}