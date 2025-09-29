-- ==========================================
-- SESSION MANAGEMENT ENHANCEMENT
-- Fixes duplicate sessions and adds refresh token security
-- ==========================================

USE pathigai_app;

-- ==========================================
-- 1. DROP EXISTING SESSIONS TABLE AND RECREATE
-- ==========================================
DROP TABLE IF EXISTS sessions;

CREATE TABLE sessions (
  session_id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,

  -- Device identification
  device_fingerprint VARCHAR(64) NOT NULL, -- SHA256 hash of IP + User-Agent + additional entropy
  device_name VARCHAR(100) NULL, -- Optional friendly name like "Chrome on Windows"
  ip_address VARCHAR(45) NOT NULL,
  user_agent VARCHAR(500) NOT NULL,

  -- Token management
  access_token_hash VARCHAR(128) NOT NULL, -- Hash of current access token
  refresh_token_hash VARCHAR(128) NOT NULL, -- Hash of current refresh token
  refresh_token_version INT DEFAULT 1, -- Increment on each refresh to detect replay

  -- Session lifecycle
  issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  access_expires_at DATETIME NOT NULL,
  refresh_expires_at DATETIME NOT NULL,
  last_used_at DATETIME DEFAULT CURRENT_TIMESTAMP,

  -- Session status
  is_active BOOLEAN DEFAULT 1,
  revoked_at DATETIME NULL,
  revoke_reason ENUM('USER_LOGOUT', 'TOKEN_REFRESH', 'SECURITY_BREACH', 'MAX_SESSIONS_EXCEEDED', 'ADMIN_REVOKE') NULL,

  -- Timestamps
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  -- Constraints and indexes
  UNIQUE KEY uk_user_device (user_id, device_fingerprint), -- One session per device
  INDEX idx_user_active (user_id, is_active, last_used_at),
  INDEX idx_refresh_token (refresh_token_hash),
  INDEX idx_session_cleanup (is_active, refresh_expires_at),

  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================
-- 2. CREATE SESSION AUDIT TABLE
-- ==========================================
CREATE TABLE session_audit_logs (
  audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id INT NULL,
  user_id INT NOT NULL,
  action ENUM('LOGIN', 'TOKEN_REFRESH', 'LOGOUT', 'REVOKE', 'SECURITY_EVENT') NOT NULL,
  device_fingerprint VARCHAR(64),
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  details JSON NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_user_audit (user_id, created_at),
  INDEX idx_session_audit (session_id, created_at),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (session_id) REFERENCES sessions(session_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================
-- 3. CREATE STORED PROCEDURES FOR SESSION MANAGEMENT
-- ==========================================

DELIMITER $$

-- Procedure to create or reuse session (handles duplicate device sessions)
CREATE PROCEDURE CreateOrReuseSession(
    IN p_user_id INT,
    IN p_device_fingerprint VARCHAR(64),
    IN p_device_name VARCHAR(100),
    IN p_ip_address VARCHAR(45),
    IN p_user_agent VARCHAR(500),
    IN p_access_token_hash VARCHAR(128),
    IN p_refresh_token_hash VARCHAR(128),
    IN p_access_expires_at DATETIME,
    IN p_refresh_expires_at DATETIME,
    IN p_max_sessions INT,
    OUT p_session_id INT,
    OUT p_action VARCHAR(20), -- 'CREATED' or 'REUSED'
    OUT p_success BOOLEAN,
    OUT p_error_message VARCHAR(500)
)
BEGIN
    DECLARE existing_session_id INT DEFAULT NULL;
    DECLARE session_count INT DEFAULT 0;
    DECLARE oldest_session_id INT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        GET DIAGNOSTICS CONDITION 1 p_error_message = MESSAGE_TEXT;
    END;

    START TRANSACTION;

    -- Check if user already has an active session on this device
    SELECT session_id INTO existing_session_id
    FROM sessions
    WHERE user_id = p_user_id
      AND device_fingerprint = p_device_fingerprint
      AND is_active = TRUE
    LIMIT 1;

    IF existing_session_id IS NOT NULL THEN
        -- Reuse existing session - update tokens and timestamps
        UPDATE sessions
        SET access_token_hash = p_access_token_hash,
            refresh_token_hash = p_refresh_token_hash,
            refresh_token_version = refresh_token_version + 1,
            access_expires_at = p_access_expires_at,
            refresh_expires_at = p_refresh_expires_at,
            last_used_at = NOW(),
            ip_address = p_ip_address,
            user_agent = p_user_agent,
            updated_at = NOW()
        WHERE session_id = existing_session_id;

        SET p_session_id = existing_session_id;
        SET p_action = 'REUSED';

        -- Log the session reuse
        INSERT INTO session_audit_logs (session_id, user_id, action, device_fingerprint, ip_address, user_agent, details)
        VALUES (existing_session_id, p_user_id, 'LOGIN', p_device_fingerprint, p_ip_address, p_user_agent,
                JSON_OBJECT('action', 'session_reused', 'device_name', p_device_name));
    ELSE
        -- Check if user has reached max concurrent sessions
        SELECT COUNT(*) INTO session_count
        FROM sessions
        WHERE user_id = p_user_id AND is_active = TRUE;

        -- If at max sessions, revoke the oldest one
        IF session_count >= p_max_sessions THEN
            SELECT session_id INTO oldest_session_id
            FROM sessions
            WHERE user_id = p_user_id AND is_active = TRUE
            ORDER BY last_used_at ASC
            LIMIT 1;

            UPDATE sessions
            SET is_active = FALSE,
                revoked_at = NOW(),
                revoke_reason = 'MAX_SESSIONS_EXCEEDED'
            WHERE session_id = oldest_session_id;

            -- Log the session revocation
            INSERT INTO session_audit_logs (session_id, user_id, action, details)
            VALUES (oldest_session_id, p_user_id, 'REVOKE',
                    JSON_OBJECT('reason', 'max_sessions_exceeded', 'max_allowed', p_max_sessions));
        END IF;

        -- Create new session
        INSERT INTO sessions (user_id, device_fingerprint, device_name, ip_address, user_agent,
                            access_token_hash, refresh_token_hash, access_expires_at, refresh_expires_at)
        VALUES (p_user_id, p_device_fingerprint, p_device_name, p_ip_address, p_user_agent,
                p_access_token_hash, p_refresh_token_hash, p_access_expires_at, p_refresh_expires_at);

        SET p_session_id = LAST_INSERT_ID();
        SET p_action = 'CREATED';

        -- Log the new session
        INSERT INTO session_audit_logs (session_id, user_id, action, device_fingerprint, ip_address, user_agent, details)
        VALUES (p_session_id, p_user_id, 'LOGIN', p_device_fingerprint, p_ip_address, p_user_agent,
                JSON_OBJECT('action', 'session_created', 'device_name', p_device_name));
    END IF;

    SET p_success = TRUE;
    COMMIT;
END$$

-- Procedure to refresh token (with security checks)
CREATE PROCEDURE RefreshToken(
    IN p_refresh_token_hash VARCHAR(128),
    IN p_new_access_token_hash VARCHAR(128),
    IN p_new_refresh_token_hash VARCHAR(128),
    IN p_new_access_expires_at DATETIME,
    IN p_new_refresh_expires_at DATETIME,
    IN p_ip_address VARCHAR(45),
    OUT p_session_id INT,
    OUT p_user_id INT,
    OUT p_success BOOLEAN,
    OUT p_error_message VARCHAR(500)
)
BEGIN
    DECLARE existing_session_id INT DEFAULT NULL;
    DECLARE existing_user_id INT DEFAULT NULL;
    DECLARE current_version INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        GET DIAGNOSTICS CONDITION 1 p_error_message = MESSAGE_TEXT;
    END;

    START TRANSACTION;

    -- Find and validate the refresh token
    SELECT session_id, user_id, refresh_token_version
    INTO existing_session_id, existing_user_id, current_version
    FROM sessions
    WHERE refresh_token_hash = p_refresh_token_hash
      AND is_active = TRUE
      AND refresh_expires_at > NOW()
    LIMIT 1;

    IF existing_session_id IS NULL THEN
        -- Invalid or expired refresh token - potential security breach
        INSERT INTO session_audit_logs (user_id, action, ip_address, details)
        VALUES (COALESCE(existing_user_id, 0), 'SECURITY_EVENT', p_ip_address,
                JSON_OBJECT('event', 'invalid_refresh_token_used', 'token_hash', p_refresh_token_hash));

        SET p_error_message = 'Invalid or expired refresh token';
        SET p_success = FALSE;
    ELSE
        -- Valid refresh token - update with new tokens
        UPDATE sessions
        SET access_token_hash = p_new_access_token_hash,
            refresh_token_hash = p_new_refresh_token_hash,
            refresh_token_version = refresh_token_version + 1,
            access_expires_at = p_new_access_expires_at,
            refresh_expires_at = p_new_refresh_expires_at,
            last_used_at = NOW(),
            updated_at = NOW()
        WHERE session_id = existing_session_id;

        SET p_session_id = existing_session_id;
        SET p_user_id = existing_user_id;
        SET p_success = TRUE;

        -- Log the token refresh
        INSERT INTO session_audit_logs (session_id, user_id, action, ip_address, details)
        VALUES (existing_session_id, existing_user_id, 'TOKEN_REFRESH', p_ip_address,
                JSON_OBJECT('old_version', current_version, 'new_version', current_version + 1));
    END IF;

    COMMIT;
END$$

-- Procedure to revoke all sessions for a user (security breach response)
CREATE PROCEDURE RevokeAllUserSessions(
    IN p_user_id INT,
    IN p_reason VARCHAR(50),
    OUT p_revoked_count INT,
    OUT p_success BOOLEAN,
    OUT p_error_message VARCHAR(500)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        GET DIAGNOSTICS CONDITION 1 p_error_message = MESSAGE_TEXT;
    END;

    START TRANSACTION;

    -- Count active sessions
    SELECT COUNT(*) INTO p_revoked_count
    FROM sessions
    WHERE user_id = p_user_id AND is_active = TRUE;

    -- Revoke all active sessions
    UPDATE sessions
    SET is_active = FALSE,
        revoked_at = NOW(),
        revoke_reason = p_reason
    WHERE user_id = p_user_id AND is_active = TRUE;

    -- Log the mass revocation
    INSERT INTO session_audit_logs (user_id, action, details)
    VALUES (p_user_id, 'SECURITY_EVENT',
            JSON_OBJECT('event', 'all_sessions_revoked', 'reason', p_reason, 'count', p_revoked_count));

    SET p_success = TRUE;
    COMMIT;
END$$

DELIMITER ;

-- ==========================================
-- 4. CREATE CLEANUP PROCEDURES
-- ==========================================

DELIMITER $$

CREATE PROCEDURE CleanupExpiredSessions()
BEGIN
    -- Archive expired sessions before deletion
    INSERT INTO session_audit_logs (session_id, user_id, action, details)
    SELECT session_id, user_id, 'LOGOUT',
           JSON_OBJECT('reason', 'session_expired', 'expired_at', refresh_expires_at)
    FROM sessions
    WHERE is_active = TRUE AND refresh_expires_at < NOW();

    -- Mark expired sessions as inactive
    UPDATE sessions
    SET is_active = FALSE,
        revoked_at = NOW(),
        revoke_reason = 'TOKEN_REFRESH'
    WHERE is_active = TRUE AND refresh_expires_at < NOW();

    -- Delete very old inactive sessions (older than 30 days)
    DELETE FROM sessions
    WHERE is_active = FALSE AND revoked_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
END$$

DELIMITER ;

-- ==========================================
-- 5. CREATE SCHEDULED EVENT FOR CLEANUP
-- ==========================================

-- Drop existing event if it exists
DROP EVENT IF EXISTS cleanup_expired_sessions;

CREATE EVENT cleanup_expired_sessions
ON SCHEDULE EVERY 1 HOUR
STARTS CURRENT_TIMESTAMP
DO
  CALL CleanupExpiredSessions();

-- ==========================================
-- 6. CREATE HELPFUL VIEWS
-- ==========================================

CREATE OR REPLACE VIEW active_user_sessions AS
SELECT
    s.session_id,
    s.user_id,
    u.email,
    u.full_name,
    s.device_name,
    s.ip_address,
    SUBSTRING(s.user_agent, 1, 100) AS user_agent_short,
    s.issued_at,
    s.last_used_at,
    s.access_expires_at,
    s.refresh_expires_at,
    TIMESTAMPDIFF(MINUTE, s.last_used_at, NOW()) AS minutes_since_last_use
FROM sessions s
JOIN users u ON s.user_id = u.user_id
WHERE s.is_active = TRUE
ORDER BY s.last_used_at DESC;

CREATE OR REPLACE VIEW user_session_summary AS
SELECT
    u.user_id,
    u.email,
    u.full_name,
    COUNT(s.session_id) AS active_sessions,
    MAX(s.last_used_at) AS last_activity,
    GROUP_CONCAT(s.device_name SEPARATOR ', ') AS devices
FROM users u
LEFT JOIN sessions s ON u.user_id = s.user_id AND s.is_active = TRUE
GROUP BY u.user_id, u.email, u.full_name
HAVING active_sessions > 0
ORDER BY last_activity DESC;
