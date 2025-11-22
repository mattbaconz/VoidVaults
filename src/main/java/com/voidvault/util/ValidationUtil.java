package com.voidvault.util;

import java.util.regex.Pattern;

/**
 * Utility class for validating and sanitizing user inputs.
 * Provides methods for validating player names, numeric inputs, and command arguments.
 */
public class ValidationUtil {
    
    // Minecraft player name pattern: 3-16 characters, alphanumeric and underscore only
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    
    // Pattern for detecting potentially dangerous characters in command arguments
    private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile("[;&|`$(){}\\[\\]<>'\"]");
    
    /**
     * Validate a Minecraft player name.
     * Player names must be 3-16 characters long and contain only alphanumeric characters and underscores.
     *
     * @param playerName The player name to validate
     * @return true if the player name is valid, false otherwise
     */
    public static boolean isValidPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return PLAYER_NAME_PATTERN.matcher(playerName).matches();
    }
    
    /**
     * Validate and parse an integer from a string.
     *
     * @param input The string to parse
     * @return The parsed integer, or null if invalid
     */
    public static Integer parseInteger(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Validate and parse an integer within a specific range.
     *
     * @param input The string to parse
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return The parsed integer if valid and within range, null otherwise
     */
    public static Integer parseIntegerInRange(String input, int min, int max) {
        Integer value = parseInteger(input);
        if (value == null) {
            return null;
        }
        
        if (value < min || value > max) {
            return null;
        }
        
        return value;
    }
    
    /**
     * Validate and parse a positive integer (greater than 0).
     *
     * @param input The string to parse
     * @return The parsed positive integer, or null if invalid or not positive
     */
    public static Integer parsePositiveInteger(String input) {
        Integer value = parseInteger(input);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }
    
    /**
     * Validate and parse a non-negative integer (0 or greater).
     *
     * @param input The string to parse
     * @return The parsed non-negative integer, or null if invalid or negative
     */
    public static Integer parseNonNegativeInteger(String input) {
        Integer value = parseInteger(input);
        if (value == null || value < 0) {
            return null;
        }
        return value;
    }
    
    /**
     * Validate and parse a double from a string.
     *
     * @param input The string to parse
     * @return The parsed double, or null if invalid
     */
    public static Double parseDouble(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Validate and parse a positive double (greater than 0).
     *
     * @param input The string to parse
     * @return The parsed positive double, or null if invalid or not positive
     */
    public static Double parsePositiveDouble(String input) {
        Double value = parseDouble(input);
        if (value == null || value <= 0.0) {
            return null;
        }
        return value;
    }
    
    /**
     * Sanitize a command argument by removing potentially dangerous characters.
     * This helps prevent command injection and other security issues.
     *
     * @param argument The argument to sanitize
     * @return The sanitized argument, or empty string if input is null
     */
    public static String sanitizeArgument(String argument) {
        if (argument == null) {
            return "";
        }
        
        // Remove dangerous characters
        String sanitized = DANGEROUS_CHARS_PATTERN.matcher(argument).replaceAll("");
        
        // Trim whitespace
        return sanitized.trim();
    }
    
    /**
     * Check if a string contains potentially dangerous characters.
     *
     * @param input The string to check
     * @return true if dangerous characters are found, false otherwise
     */
    public static boolean containsDangerousCharacters(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return DANGEROUS_CHARS_PATTERN.matcher(input).find();
    }
    
    /**
     * Validate that a string is not null or empty.
     *
     * @param input The string to validate
     * @return true if the string is not null and not empty, false otherwise
     */
    public static boolean isNotEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }
    
    /**
     * Validate a page number for vault access.
     * Page numbers must be positive integers.
     *
     * @param input The page number string to validate
     * @return The validated page number, or null if invalid
     */
    public static Integer validatePageNumber(String input) {
        return parsePositiveInteger(input);
    }
    
    /**
     * Validate a slot count for vault configuration.
     * Valid slot counts are: 9, 18, 27, 36, 45, 54
     *
     * @param input The slot count string to validate
     * @return The validated slot count, or null if invalid
     */
    public static Integer validateSlotCount(String input) {
        Integer slots = parsePositiveInteger(input);
        if (slots == null) {
            return null;
        }
        
        // Check if it's a valid slot count
        return switch (slots) {
            case 9, 18, 27, 36, 45, 54 -> slots;
            default -> null;
        };
    }
    
    /**
     * Validate a page count for vault configuration.
     * Page counts must be positive integers with a reasonable maximum.
     *
     * @param input The page count string to validate
     * @param maxPages The maximum allowed pages
     * @return The validated page count, or null if invalid
     */
    public static Integer validatePageCount(String input, int maxPages) {
        return parseIntegerInRange(input, 1, maxPages);
    }
    
    /**
     * Truncate a string to a maximum length.
     *
     * @param input The string to truncate
     * @param maxLength The maximum length
     * @return The truncated string, or the original if shorter than maxLength
     */
    public static String truncate(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }
}
