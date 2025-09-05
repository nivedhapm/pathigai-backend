package com.nivedha.pathigai.auth.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class RecaptchaService {

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${app.recaptcha.secret-key}")
    private String secretKey;

    @Value("${app.recaptcha.enabled:false}")
    private boolean recaptchaEnabled;

    private final WebClient webClient;

    public RecaptchaService() {
        this.webClient = WebClient.builder()
                .baseUrl(RECAPTCHA_VERIFY_URL)
                .build();
    }

    public boolean verifyRecaptcha(String recaptchaToken, String clientIp) {
        // If reCAPTCHA is disabled, always return true (for development)
        if (!recaptchaEnabled) {
            log.info("reCAPTCHA verification is disabled");
            return true;
        }

        // If no token provided, fail verification
        if (recaptchaToken == null || recaptchaToken.trim().isEmpty()) {
            log.warn("reCAPTCHA token is empty");
            return false;
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("secret", secretKey);
            formData.add("response", recaptchaToken);
            if (clientIp != null && !clientIp.isEmpty()) {
                formData.add("remoteip", clientIp);
            }

            RecaptchaResponse response = webClient
                    .post()
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(RecaptchaResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("reCAPTCHA verification successful for IP: {}", clientIp);
                return true;
            } else {
                log.warn("reCAPTCHA verification failed. Response: {}", response);
                return false;
            }

        } catch (Exception e) {
            log.error("Error during reCAPTCHA verification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract client IP address from request
     */
    public String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Data
    private static class RecaptchaResponse {
        @JsonProperty("success")
        private boolean success;

        @JsonProperty("challenge_ts")
        private String challengeTimestamp;

        @JsonProperty("hostname")
        private String hostname;

        @JsonProperty("error-codes")
        private String[] errorCodes;

        @JsonProperty("score")
        private Double score; // For reCAPTCHA v3

        @JsonProperty("action")
        private String action; // For reCAPTCHA v3
    }
}