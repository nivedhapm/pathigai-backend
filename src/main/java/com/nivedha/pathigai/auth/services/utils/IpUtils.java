package com.nivedha.pathigai.auth.services.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IpUtils {

    public String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (common in load balancers and proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            String ip = xForwardedFor.split(",")[0].trim();
            log.debug("Client IP from X-Forwarded-For: {}", ip);
            return ip;
        }

        // Check for X-Real-IP header (used by some proxies like Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            log.debug("Client IP from X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        // Check for X-Forwarded header
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            log.debug("Client IP from X-Forwarded: {}", xForwarded);
            return xForwarded;
        }

        // Check for Forwarded-For header
        String forwardedFor = request.getHeader("Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(forwardedFor)) {
            log.debug("Client IP from Forwarded-For: {}", forwardedFor);
            return forwardedFor;
        }

        // Check for Forwarded header
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty() && !"unknown".equalsIgnoreCase(forwarded)) {
            log.debug("Client IP from Forwarded: {}", forwarded);
            return forwarded;
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        log.debug("Client IP from RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    public boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Simple IPv4 validation
        String ipv4Pattern = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";

        // Simple IPv6 validation
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";

        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }

    public boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }
}