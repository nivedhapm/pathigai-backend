package com.nivedha.pathigai.auth.services.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class DateTimeUtils {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public LocalDateTime nowInIndia() {
        return LocalDateTime.now(INDIA_ZONE);
    }

    public LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.plusMinutes(minutes);
    }

    public LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return dateTime.plusHours(hours);
    }

    public LocalDateTime addDays(LocalDateTime dateTime, long days) {
        return dateTime.plusDays(days);
    }

    public boolean isExpired(LocalDateTime expiryTime) {
        return expiryTime.isBefore(now());
    }

    public boolean isExpiredInIndia(LocalDateTime expiryTime) {
        return expiryTime.isBefore(nowInIndia());
    }

    public long minutesUntilExpiry(LocalDateTime expiryTime) {
        return ChronoUnit.MINUTES.between(now(), expiryTime);
    }

    public long secondsUntilExpiry(LocalDateTime expiryTime) {
        return ChronoUnit.SECONDS.between(now(), expiryTime);
    }

    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public String formatDateTime(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public LocalDateTime parseDateTime(String dateTimeString) {
        try {
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
        } catch (Exception e) {
            log.error("Failed to parse datetime string: {}", dateTimeString, e);
            throw new IllegalArgumentException("Invalid datetime format: " + dateTimeString);
        }
    }

    public LocalDateTime parseDateTime(String dateTimeString, String pattern) {
        try {
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            log.error("Failed to parse datetime string: {} with pattern: {}", dateTimeString, pattern, e);
            throw new IllegalArgumentException("Invalid datetime format: " + dateTimeString);
        }
    }

    public boolean isWithinRange(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        return !target.isBefore(start) && !target.isAfter(end);
    }

    public boolean isRecentlyCreated(LocalDateTime createdTime, long maxAgeMinutes) {
        LocalDateTime threshold = now().minusMinutes(maxAgeMinutes);
        return createdTime.isAfter(threshold);
    }

    public LocalDateTime getStartOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }

    public LocalDateTime getEndOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atTime(23, 59, 59, 999999999);
    }

    public long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofEpochSecond(epochMilli / 1000, (int) (epochMilli % 1000) * 1000000, ZoneOffset.UTC);
    }
}