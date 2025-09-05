package com.nivedha.pathigai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application configuration class
 * Enables scheduling for session cleanup and other periodic tasks
 */
@Configuration
@EnableScheduling  // Enable @Scheduled annotations for session cleanup
public class AppConfig {
    // This class enables scheduling for SessionService cleanup tasks
    // Additional application-wide configurations can be added here
}