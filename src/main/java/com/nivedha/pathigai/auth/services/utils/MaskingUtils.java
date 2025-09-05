package com.nivedha.pathigai.auth.services.utils;

import org.springframework.stereotype.Component;

@Component
public class MaskingUtils {

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 10) {
            return phone;
        }

        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() >= 10) {
            String last2 = digits.substring(digits.length() - 2);
            return "+91 " + "*".repeat(8) + last2;
        }

        return phone;
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 2) {
            return email;
        }

        String maskedLocal = localPart.charAt(0) +
                "*".repeat(localPart.length() - 2) +
                localPart.charAt(localPart.length() - 1);

        return maskedLocal + "@" + domainPart;
    }
}