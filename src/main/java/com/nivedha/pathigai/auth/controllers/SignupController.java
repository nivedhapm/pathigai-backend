package com.nivedha.pathigai.auth.controllers;

import com.nivedha.pathigai.auth.dto.request.SignupRegisterRequest;
import com.nivedha.pathigai.auth.dto.request.SignupCompleteRequest;
import com.nivedha.pathigai.auth.dto.response.SignupRegisterResponse;
import com.nivedha.pathigai.auth.dto.response.SignupCompleteResponse;
import com.nivedha.pathigai.auth.services.SignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/signup")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class SignupController {

    private final SignupService signupService;

    @PostMapping("/register")
    public ResponseEntity<SignupRegisterResponse> registerUser(
            @Valid @RequestBody SignupRegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received signup registration request for email: {}", request.getEmail());
            String userIpAddress = httpRequest.getRemoteAddr();  // Or use a utility method for real IP
            SignupRegisterResponse response = signupService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Signup registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    SignupRegisterResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during signup registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    SignupRegisterResponse.builder()
                            .message("Registration failed. Please try again.")
                            .build()
            );
        }
    }


    @PostMapping("/complete")
    public ResponseEntity<SignupCompleteResponse> completeSignup(@Valid @RequestBody SignupCompleteRequest request) {
        try {
            log.info("Received signup completion request for user ID: {}", request.getUserId());
            SignupCompleteResponse response = signupService.completeSignup(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Signup completion failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    SignupCompleteResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during signup completion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    SignupCompleteResponse.builder()
                            .success(false)
                            .message("Signup completion failed. Please try again.")
                            .build()
            );
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        try {
            boolean exists = signupService.checkEmailExists(email);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking email existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("exists", false));
        }
    }

    @GetMapping("/check-phone")
    public ResponseEntity<Map<String, Boolean>> checkPhoneExists(@RequestParam String phone) {
        try {
            boolean exists = signupService.checkPhoneExists(phone);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking phone existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("exists", false));
        }
    }
}