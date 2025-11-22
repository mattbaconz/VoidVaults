package com.voidvault.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record representing a cooldown entry for a player.
 * Uses Java 21 record syntax for concise cooldown tracking.
 *
 * @param playerId   The unique identifier of the player
 * @param expiryTime The timestamp (in milliseconds) when the cooldown expires
 */
public record CooldownEntry(
        UUID playerId,
        long expiryTime
) {
    /**
     * Compact constructor with validation.
     */
    public CooldownEntry {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        if (expiryTime < 0) {
            throw new IllegalArgumentException("Expiry time cannot be negative");
        }
    }

    /**
     * Creates a new CooldownEntry that expires after the specified duration.
     *
     * @param playerId        The player's UUID
     * @param durationSeconds The cooldown duration in seconds
     * @return A new CooldownEntry instance
     */
    public static CooldownEntry create(UUID playerId, int durationSeconds) {
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        return new CooldownEntry(playerId, expiryTime);
    }

    /**
     * Creates a new CooldownEntry with a specific expiry timestamp.
     *
     * @param playerId   The player's UUID
     * @param expiryTime The expiry timestamp in milliseconds
     * @return A new CooldownEntry instance
     */
    public static CooldownEntry withExpiry(UUID playerId, long expiryTime) {
        return new CooldownEntry(playerId, expiryTime);
    }

    /**
     * Checks if the cooldown has expired.
     *
     * @return true if the current time is past the expiry time, false otherwise
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryTime;
    }

    /**
     * Checks if the cooldown is still active.
     *
     * @return true if the cooldown has not expired, false otherwise
     */
    public boolean isActive() {
        return !isExpired();
    }

    /**
     * Gets the remaining time in milliseconds until the cooldown expires.
     *
     * @return The remaining time in milliseconds, or 0 if expired
     */
    public long getRemainingMillis() {
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets the remaining time in seconds until the cooldown expires.
     *
     * @return The remaining time in seconds, or 0 if expired
     */
    public long getRemainingSeconds() {
        return getRemainingMillis() / 1000;
    }

    /**
     * Gets the remaining time formatted as a human-readable string.
     *
     * @return A formatted string like "1m 30s" or "45s"
     */
    public String getFormattedRemaining() {
        long seconds = getRemainingSeconds();
        if (seconds <= 0) {
            return "0s";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", remainingSeconds);
        }
    }

    /**
     * Gets the expiry time as an Instant.
     *
     * @return The expiry time as an Instant
     */
    public Instant getExpiryInstant() {
        return Instant.ofEpochMilli(expiryTime);
    }

    /**
     * Creates a new CooldownEntry with an extended duration.
     *
     * @param additionalSeconds The number of seconds to add to the expiry time
     * @return A new CooldownEntry with extended expiry time
     */
    public CooldownEntry extend(int additionalSeconds) {
        long newExpiryTime = expiryTime + (additionalSeconds * 1000L);
        return new CooldownEntry(playerId, newExpiryTime);
    }

    /**
     * Compares this cooldown entry with another to determine which expires first.
     *
     * @param other The other CooldownEntry to compare with
     * @return A negative integer, zero, or a positive integer as this entry
     * expires before, at the same time as, or after the specified entry
     */
    public int compareExpiryTime(CooldownEntry other) {
        return Long.compare(this.expiryTime, other.expiryTime);
    }
}
