package com.nivedha.pathigai.config;

import com.nivedha.pathigai.auth.entities.User;
import com.nivedha.pathigai.auth.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        try {
            String authHeader = request.getHeader("Authorization");
            log.info("🔐 JWT Filter - {} {}", method, requestUri);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("❌ No JWT token found in Authorization header");
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            log.info("🎫 JWT token received: {}...", token.substring(0, Math.min(20, token.length())));

            try {
                String email = jwtConfig.extractUsername(token);
                log.info("📧 Extracted email from token: {}", email);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.info("🔍 Processing JWT token for email: {}", email);

                    // Get user from database with eager loading of profile and role
                    Optional<User> userOpt = userRepository.findByEmailAndUserStatusWithProfileAndRole(email, User.UserStatus.ACTIVE);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        log.info("✅ User found in database: {} (ID: {})", user.getEmail(), user.getUserId());

                        // Validate token
                        if (jwtConfig.validateToken(token, email)) {
                            // Create authorities based on user's profile and role
                            List<SimpleGrantedAuthority> authorities = createAuthorities(user);

                            // Create UserDetails object for Spring Security
                            org.springframework.security.core.userdetails.User userDetails =
                                new org.springframework.security.core.userdetails.User(
                                    user.getEmail(),
                                    user.getPasswordHash(),
                                    user.getEnabled(),
                                    true, // accountNonExpired
                                    true, // credentialsNonExpired
                                    user.getUserStatus() == User.UserStatus.ACTIVE, // accountNonLocked
                                    authorities
                                );

                            // Create authentication object with UserDetails
                            UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // Set authentication in security context
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.info("✅ JWT authentication successful for user: {} with authorities: {}", email, authorities);
                        } else {
                            log.error("❌ JWT token validation FAILED for user: {}", email);
                        }
                    } else {
                        log.error("❌ User NOT FOUND in database for email: {}", email);
                    }
                } else if (email == null) {
                    log.error("❌ Failed to extract email from JWT token");
                } else {
                    log.debug("⚠️ User already authenticated or email is null");
                }
            } catch (Exception tokenException) {
                log.error("❌ JWT token parsing/validation error: {}", tokenException.getMessage(), tokenException);
            }

        } catch (Exception e) {
            log.error("❌ JWT authentication error: {}", e.getMessage(), e);
            // Don't fail the request, just continue without authentication
        }

        filterChain.doFilter(request, response);
    }

    protected List<SimpleGrantedAuthority> createAuthorities(User user) {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

        try {
            // Add profile-based authority (with safe access to eager-loaded field)
            if (user.getPrimaryProfile() != null) {
                String profileName = user.getPrimaryProfile().getName();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + profileName));
                log.debug("   🏷️ Added profile authority: ROLE_{}", profileName);
            }

            // Add role-based authority (with safe access to eager-loaded field)
            if (user.getPrimaryRole() != null) {
                String roleName = user.getPrimaryRole().getName();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                log.debug("   🏷️ Added role authority: ROLE_{}", roleName);
            }
        } catch (Exception e) {
            log.error("❌ Error creating authorities for user {}: {}", user.getEmail(), e.getMessage());
            // Add a default authority so authentication doesn't fail completely
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }
}
