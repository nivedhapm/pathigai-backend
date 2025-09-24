package com.nivedha.pathigai.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.entities.Profile;
import com.nivedha.pathigai.auth.entities.Role;

@Component
@Slf4j
public class JwtConfig {

    @Value("${app.jwt.secret:pathigai-jwt-secret-key-2024-super-secure}")
    private String secret;

    @Value("${app.jwt.access-token-expiration:7200000}") // 2 hours (improved UX)
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:86400000}") // 24 hours
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(Integer userId, String email, String fullName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("fullName", fullName);
        claims.put("type", "access");

        return createToken(claims, email, accessTokenExpiration);
    }

    public String generateRefreshToken(Integer userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "refresh");

        return createToken(claims, email, refreshTokenExpiration);
    }

    public String generateAccessToken(User user) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());
        if (user.getCompany() != null) {
            claims.put("companyId", user.getCompany().getCompanyId());
            claims.put("companyName", user.getCompany().getCompanyName());
        }

        Profile profile = user.getPrimaryProfile();
        Role role = user.getPrimaryRole();

        if (profile != null) {
            String pName = profile.getName(); // Fixed: was getProfileName()
            claims.put("profile", pName);
            claims.put("profileId", profile.getProfileId());
            claims.put("profileLevel", profile.getHierarchyLevel());
            claims.put("primary_profile", pName);
            claims.put("authorities", List.of("ROLE_" + pName));
        } else if (role != null) {
            String rName = role.getName();
            claims.put("role", rName);
            claims.put("roleId", role.getRoleId());
            claims.put("authorities", List.of("ROLE_" + rName));

            // Special handling for APPLICANT role
            if ("APPLICANT".equals(rName)) {
                claims.put("redirectTo", "/applicant-portal");
            }
        }

        // Add redirect path
        String redirectTo = determineRedirectPath(profile, role);
        claims.put("redirectTo", redirectTo);

        claims.put("type", "access");
        return createToken(claims, user.getEmail(), accessTokenExpiration);
    }

    // Helper method to determine redirect path
    private String determineRedirectPath(Profile profile, Role role) {
        if (profile != null) {
            return "/dashboard";
        } else if (role != null && "APPLICANT".equals(role.getName())) {
            return "/applicant-portal";
        } else {
            return "/dashboard";
        }
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token");
        }
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String email) {
        final String tokenEmail = extractUsername(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }

    public Boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    public Boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // Additional methods for refresh token support
    public Boolean validateRefreshToken(String token) {
        try {
            return !isTokenExpired(token) && isRefreshToken(token);
        } catch (Exception e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    public String refreshAccessToken(String refreshToken, User user) {
        if (!validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // Generate new access token with updated user information
        return generateAccessToken(user);
    }

    public Date getTokenExpiration(String token) {
        return extractExpiration(token);
    }

    public long getTimeUntilExpiration(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    // Additional extraction methods for the new claims
    public String extractProfile(String token) {
        return extractClaim(token, claims -> claims.get("profile", String.class));
    }

    public Integer extractProfileId(String token) {
        return extractClaim(token, claims -> claims.get("profileId", Integer.class));
    }

    public Integer extractProfileLevel(String token) {
        return extractClaim(token, claims -> claims.get("profileLevel", Integer.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Integer extractRoleId(String token) {
        return extractClaim(token, claims -> claims.get("roleId", Integer.class));
    }

    public String extractRedirectTo(String token) {
        return extractClaim(token, claims -> claims.get("redirectTo", String.class));
    }

    public Integer extractCompanyId(String token) {
        return extractClaim(token, claims -> claims.get("companyId", Integer.class));
    }

    public String extractCompanyName(String token) {
        return extractClaim(token, claims -> claims.get("companyName", String.class));
    }
}