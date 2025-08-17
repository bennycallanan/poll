package com.conaxgames.poll.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwy])");

    public static LocalDateTime parseDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration cannot be null or empty");
        }
        
        LocalDateTime now = LocalDateTime.now();
        Matcher matcher = DURATION_PATTERN.matcher(duration.toLowerCase());
        
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration format. Use: <number><unit> (e.g., 1d, 2h, 30m)");
        }
        
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);
        
        switch (unit) {
            case "s":
                return now.plus(amount, ChronoUnit.SECONDS);
            case "m":
                return now.plus(amount, ChronoUnit.MINUTES);
            case "h":
                return now.plus(amount, ChronoUnit.HOURS);
            case "d":
                return now.plus(amount, ChronoUnit.DAYS);
            case "w":
                return now.plus(amount * 7, ChronoUnit.DAYS);
            case "y":
                return now.plus(amount, ChronoUnit.YEARS);
            default:
                throw new IllegalArgumentException("Unknown time unit: " + unit);
        }
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }
        
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    public static String getTimeRemaining(LocalDateTime target) {
        if (target == null) {
            return "Unknown";
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(target)) {
            return "Expired";
        }
        
        long seconds = java.time.Duration.between(now, target).getSeconds();
        
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h";
        } else {
            return (seconds / 86400) + "d";
        }
    }

    public static boolean isValidDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            return false;
        }
        
        return DURATION_PATTERN.matcher(duration.toLowerCase()).matches();
    }
}
