package com.nivedha.pathigai.auth.dto.request;

/**
 * Base class for requests that include reCAPTCHA token
 * Extend this class for login/signup requests that need reCAPTCHA verification
 */
public class RecaptchaRequest {

    private String recaptchaToken;

    public String getRecaptchaToken() {
        return recaptchaToken;
    }

    public void setRecaptchaToken(String recaptchaToken) {
        this.recaptchaToken = recaptchaToken;
    }
}
