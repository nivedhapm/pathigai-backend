-- Database Verification Queries for Token Expiration Testing
-- Run these queries to verify your token expiration settings are working correctly

USE pathigai_app;

-- ==========================================
-- 1. CHECK CURRENT SESSION TOKEN EXPIRATION TIMES
-- ==========================================
SELECT
    s.session_id,
    s.user_id,
    u.email,
    u.full_name,
    s.device_name,
    s.issued_at,
    s.access_expires_at,
    s.refresh_expires_at,
    s.last_used_at,
    s.is_active,
    -- Calculate time remaining for access token (in minutes)
    TIMESTAMPDIFF(MINUTE, NOW(), s.access_expires_at) AS access_expires_in_minutes,
    -- Calculate time remaining for refresh token (in hours)
    TIMESTAMPDIFF(HOUR, NOW(), s.refresh_expires_at) AS refresh_expires_in_hours,
    -- Check if access token is expired
    CASE
        WHEN NOW() > s.access_expires_at THEN 'EXPIRED'
        WHEN TIMESTAMPDIFF(MINUTE, NOW(), s.access_expires_at) <= 5 THEN 'EXPIRING_SOON'
        ELSE 'VALID'
    END AS access_token_status,
    -- Check if refresh token is expired
    CASE
        WHEN NOW() > s.refresh_expires_at THEN 'EXPIRED'
        WHEN TIMESTAMPDIFF(HOUR, NOW(), s.refresh_expires_at) <= 1 THEN 'EXPIRING_SOON'
        ELSE 'VALID'
    END AS refresh_token_status
FROM sessions s
JOIN users u ON s.user_id = u.user_id
WHERE s.is_active = 1
ORDER BY s.last_used_at DESC;

-- ==========================================
-- 2. VERIFY TOKEN EXPIRATION CONFIGURATION
-- ==========================================
-- This query shows the actual token duration based on database records
SELECT
    'Access Token Duration' as token_type,
    MIN(TIMESTAMPDIFF(SECOND, s.issued_at, s.access_expires_at)) as min_duration_seconds,
    MAX(TIMESTAMPDIFF(SECOND, s.issued_at, s.access_expires_at)) as max_duration_seconds,
    AVG(TIMESTAMPDIFF(SECOND, s.issued_at, s.access_expires_at)) as avg_duration_seconds,
    ROUND(AVG(TIMESTAMPDIFF(SECOND, s.issued_at, s.access_expires_at)) / 3600, 2) as avg_duration_hours
FROM sessions s
WHERE s.created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)

UNION ALL

SELECT
    'Refresh Token Duration' as token_type,
    MIN(TIMESTAMPDIFF(SECOND, s.issued_at, s.refresh_expires_at)) as min_duration_seconds,
    MAX(TIMESTAMPDIFF(SECOND, s.issued_at, s.refresh_expires_at)) as max_duration_seconds,
    AVG(TIMESTAMPDIFF(SECOND, s.issued_at, s.refresh_expires_at)) as avg_duration_seconds,
    ROUND(AVG(TIMESTAMPDIFF(SECOND, s.issued_at, s.refresh_expires_at)) / 3600, 2) as avg_duration_hours
FROM sessions s
WHERE s.created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY);

-- ==========================================
-- 3. CHECK TOKEN REFRESH ACTIVITY
-- ==========================================
-- Shows recent token refresh activities
SELECT
    s.session_id,
    s.user_id,
    u.email,
    s.refresh_token_version,
    s.issued_at,
    s.updated_at,
    TIMESTAMPDIFF(MINUTE, s.issued_at, s.updated_at) as session_age_minutes,
    s.last_used_at,
    CASE
        WHEN s.updated_at > s.issued_at THEN 'TOKEN_REFRESHED'
        ELSE 'ORIGINAL_SESSION'
    END as refresh_status
FROM sessions s
JOIN users u ON s.user_id = u.user_id
WHERE s.is_active = 1
  AND s.created_at >= DATE_SUB(NOW(), INTERVAL 2 HOUR)
ORDER BY s.updated_at DESC;

-- ==========================================
-- 4. EXPECTED VS ACTUAL TOKEN EXPIRATION CHECK
-- ==========================================
-- Verify that your 2-hour access token and 24-hour refresh token settings are applied
SELECT
    s.session_id,
    s.user_id,
    u.email,
    s.issued_at,
    s.access_expires_at,
    s.refresh_expires_at,
    -- Expected access token expiration (2 hours = 7200 seconds)
    DATE_ADD(s.issued_at, INTERVAL 7200 SECOND) as expected_access_expires,
    -- Expected refresh token expiration (24 hours = 86400 seconds)
    DATE_ADD(s.issued_at, INTERVAL 86400 SECOND) as expected_refresh_expires,
    -- Check if actual matches expected for access token
    CASE
        WHEN ABS(TIMESTAMPDIFF(SECOND, s.access_expires_at, DATE_ADD(s.issued_at, INTERVAL 7200 SECOND))) <= 60
        THEN '✅ CORRECT (2 hours)'
        ELSE '❌ INCORRECT'
    END as access_token_check,
    -- Check if actual matches expected for refresh token
    CASE
        WHEN ABS(TIMESTAMPDIFF(SECOND, s.refresh_expires_at, DATE_ADD(s.issued_at, INTERVAL 86400 SECOND))) <= 60
        THEN '✅ CORRECT (24 hours)'
        ELSE '❌ INCORRECT'
    END as refresh_token_check
FROM sessions s
JOIN users u ON s.user_id = u.user_id
WHERE s.is_active = 1
  AND s.created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY s.created_at DESC;

-- ==========================================
-- 5. SESSION MANAGEMENT TEST QUERY
-- ==========================================
-- Check if session reuse is working (should have only one active session per device)
SELECT
    u.email,
    u.full_name,
    s.device_fingerprint,
    s.device_name,
    COUNT(*) as active_sessions_count,
    GROUP_CONCAT(s.session_id ORDER BY s.last_used_at DESC) as session_ids,
    MAX(s.last_used_at) as latest_activity,
    CASE
        WHEN COUNT(*) = 1 THEN '✅ CORRECT (One session per device)'
        ELSE '⚠️ MULTIPLE SESSIONS (Check if device fingerprint is working)'
    END as session_management_status
FROM sessions s
JOIN users u ON s.user_id = u.user_id
WHERE s.is_active = 1
GROUP BY u.user_id, s.device_fingerprint
ORDER BY active_sessions_count DESC, latest_activity DESC;

-- ==========================================
-- 6. QUICK TEST COMMANDS
-- ==========================================
-- Use these to quickly check current status

-- Check if your current session has correct token expiration times:
-- SELECT * FROM sessions WHERE user_id = 5 AND is_active = 1 ORDER BY last_used_at DESC LIMIT 1;

-- Check token expiration times in a readable format:
-- SELECT
--     session_id,
--     user_id,
--     DATE_FORMAT(access_expires_at, '%Y-%m-%d %H:%i:%s') as access_expires,
--     DATE_FORMAT(refresh_expires_at, '%Y-%m-%d %H:%i:%s') as refresh_expires,
--     CONCAT(TIMESTAMPDIFF(MINUTE, NOW(), access_expires_at), ' minutes') as access_time_remaining,
--     CONCAT(TIMESTAMPDIFF(HOUR, NOW(), refresh_expires_at), ' hours') as refresh_time_remaining
-- FROM sessions
-- WHERE user_id = 5 AND is_active = 1;

-- ==========================================
-- 7. TEST SCENARIO SIMULATION
-- ==========================================
-- To test your specific scenarios, run this after logging in:

/*
SCENARIO TEST:
1. Login at 12:00 → Check session record is created with correct expiration times
2. Wait until access token near expiration → Should see refresh token activity
3. Check session updates → Should see refresh_token_version increment and updated timestamps

Query to monitor:
SELECT
    session_id,
    refresh_token_version,
    DATE_FORMAT(issued_at, '%H:%i:%s') as login_time,
    DATE_FORMAT(access_expires_at, '%H:%i:%s') as access_expires,
    DATE_FORMAT(updated_at, '%H:%i:%s') as last_refresh,
    TIMESTAMPDIFF(MINUTE, NOW(), access_expires_at) as minutes_until_access_expires
FROM sessions
WHERE user_id = YOUR_USER_ID AND is_active = 1;
*/
