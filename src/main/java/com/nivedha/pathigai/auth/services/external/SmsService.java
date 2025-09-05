//package com.nivedha.pathigai.services.external;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//@Service
//@RequiredArgsConstructor

//public class SmsService {
//
//    private final RestTemplate restTemplate;
//
//    @Value("${app.sms.fast2sms.api-key}")
//    private String apiKey;
//
//    @Value("${app.sms.fast2sms.sender-id}")
//    private String senderId;
//
//    @Value("${app.sms.enabled:true}")
//    private boolean smsEnabled;
//
//    private static final String FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2";
//
//    public void sendSms(String phoneNumber, String message) {
//        if (!smsEnabled) {
//            log.info("SMS service disabled. Would send SMS to {}: {}", phoneNumber, message);
//            return;
//        }
//
//        try {
//            log.info("Sending SMS to: {}", phoneNumber);
//
//            // Remove +91 prefix for Fast2SMS
//            String normalizedPhone = phoneNumber.replace("+91", "");
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//            headers.set("authorization", apiKey);
//
//            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//            body.add("variables_values", message);
//            body.add("route", "otp");
//            body.add("numbers", normalizedPhone);
//
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
//
//            String response = restTemplate.postForObject(FAST2SMS_URL, request, String.class);
//            log.info("SMS sent successfully. Response: {}", response);
//
//        } catch (Exception e) {
//            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
//            throw new RuntimeException("Failed to send SMS", e);
//        }
//    }
//}

package com.nivedha.pathigai.auth.services.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.verification.dev-mode:false}")
    private boolean devMode;

    public void sendSms(String phoneNumber, String message) {
        if (devMode) {
            log.info("=== DEV MODE SMS ===");
            log.info("Phone: {}", phoneNumber);
            log.info("Message: {}", message);
            log.info("=== SMS SENT (DEV) ===");
            return;
        }

        if (!smsEnabled) {
            log.info("SMS service disabled - would send to {}: {}", phoneNumber, message);
            return;
        }

        try {
            log.info("Sending SMS to: {}", phoneNumber);

            // TODO: Implement your actual SMS provider here
            // For now, just log the message
            log.info("SMS sent successfully to: {} with message: {}", phoneNumber, message);

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
}