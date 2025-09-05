package com.nivedha.pathigai.config;

import com.nivedha.pathigai.auth.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== VALIDATION EXCEPTIONS ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        BindingResult result = ex.getBindingResult();

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : result.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input data")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation error [{}]: {} at {}", errorId, fieldErrors, request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Parameter")
                .message("Required parameter '" + ex.getParameterName() + "' is missing")
                .path(request.getRequestURI())
                .build();

        log.warn("Missing parameter error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Type Mismatch")
                .message("Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'")
                .path(request.getRequestURI())
                .build();

        log.warn("Type mismatch error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // ========== AUTHENTICATION & AUTHORIZATION EXCEPTIONS ==========

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid credentials or authentication required")
                .path(request.getRequestURI())
                .build();

        log.warn("Authentication error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Invalid Credentials")
                .message("Invalid email or password")
                .path(request.getRequestURI())
                .build();

        log.warn("Bad credentials error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .build();

        log.warn("Access denied error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // ========== CUSTOM BUSINESS LOGIC EXCEPTIONS ==========

    @ExceptionHandler(com.nivedha.pathigai.auth.exceptions.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuthentication(
            com.nivedha.pathigai.auth.exceptions.AuthenticationException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Custom authentication error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ErrorResponse> handleVerification(
            VerificationException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Verification Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Verification error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("User Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("User not found error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Invalid Token")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Invalid token error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccountNotEnabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotEnabled(
            AccountNotEnabledException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Account Not Enabled")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Account not enabled error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttempts(
            TooManyAttemptsException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Attempts")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Too many attempts error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    // ========== DATABASE EXCEPTIONS ==========

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        String message = "Data constraint violation";

        // Check for common constraint violations
        if (ex.getMessage().contains("email")) {
            message = "Email already exists";
        } else if (ex.getMessage().contains("phone")) {
            message = "Phone number already exists";
        } else if (ex.getMessage().contains("company_name")) {
            message = "Company name already exists";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Data integrity violation [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ========== GENERAL EXCEPTIONS ==========

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Illegal argument error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Invalid State")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("Illegal state error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Runtime Error")
                .message("An unexpected error occurred. Please try again.")
                .path(request.getRequestURI())
                .build();

        log.error("Runtime error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        String errorId = UUID.randomUUID().toString();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please contact support if the problem persists.")
                .path(request.getRequestURI())
                .build();

        log.error("Unexpected error [{}]: {} at {}", errorId, ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ========== ERROR RESPONSE DTO ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String errorId;
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> fieldErrors;
    }
}