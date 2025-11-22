package com.voidvault.config;

/**
 * Enum representing the two operational modes of VoidVault.
 * SIMPLE mode provides a straightforward vault with size-based permissions.
 * PAGED mode provides a feature-rich vault with multiple pages and utility functions.
 */
public enum PluginMode {
    /**
     * Simple mode: Single-page vault without control bar buttons.
     * Size determined by voidvault.slots.X permissions (9-54 slots).
     */
    SIMPLE,
    
    /**
     * Paged mode: Fixed 54-slot inventory with 50 storage slots and 4 control bar slots.
     * Supports multiple pages, sorting, quick deposit, and other QoL features.
     */
    PAGED;
    
    /**
     * Parse a string into a PluginMode, defaulting to SIMPLE if invalid.
     *
     * @param value the string value to parse
     * @return the corresponding PluginMode, or SIMPLE if invalid
     */
    public static PluginMode fromString(String value) {
        if (value == null) {
            return SIMPLE;
        }
        
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SIMPLE;
        }
    }
}
