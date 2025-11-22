package com.voidvault.storage;

import com.voidvault.model.PlayerVaultData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache for player vault data.
 * Implements dirty tracking to optimize save operations by only persisting modified data.
 * <p>
 * This cache uses ConcurrentHashMap to ensure thread-safe operations without explicit locking.
 * Includes automatic cache size management to prevent memory issues.
 */
public class DataCache {

    /**
     * Main cache storing player vault data.
     * Key: Player UUID, Value: PlayerVaultData
     */
    private final Map<UUID, PlayerVaultData> cache;

    /**
     * Set of player UUIDs whose data has been modified since last save.
     * Used for efficient auto-save operations.
     */
    private final Set<UUID> dirtyPlayers;
    
    /**
     * Maximum cache size before warnings are logged.
     * This helps detect potential memory leaks.
     */
    private static final int CACHE_SIZE_WARNING_THRESHOLD = 1000;

    /**
     * Creates a new DataCache instance.
     */
    public DataCache() {
        this.cache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
    }

    /**
     * Retrieves player vault data from the cache.
     *
     * @param playerId The UUID of the player
     * @return An Optional containing the player's data if present, empty otherwise
     */
    public Optional<PlayerVaultData> get(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    /**
     * Stores player vault data in the cache.
     * This does NOT mark the player as dirty.
     *
     * @param playerId The UUID of the player
     * @param data     The vault data to cache
     */
    public void put(UUID playerId, PlayerVaultData data) {
        if (playerId == null || data == null) {
            throw new IllegalArgumentException("Player ID and data cannot be null");
        }
        cache.put(playerId, data);
        
        // Check cache size and log warning if threshold exceeded
        int currentSize = cache.size();
        if (currentSize > CACHE_SIZE_WARNING_THRESHOLD) {
            System.err.println("WARNING: DataCache size (" + currentSize + ") exceeds threshold (" + 
                CACHE_SIZE_WARNING_THRESHOLD + "). Possible memory leak or too many cached players.");
        }
    }

    /**
     * Marks a player's data as dirty (modified).
     * Dirty players will be included in the next save operation.
     *
     * @param playerId The UUID of the player
     */
    public void markDirty(UUID playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        dirtyPlayers.add(playerId);
    }

    /**
     * Removes the dirty flag from a player.
     * This should be called after successfully saving the player's data.
     *
     * @param playerId The UUID of the player
     */
    public void clearDirty(UUID playerId) {
        dirtyPlayers.remove(playerId);
    }

    /**
     * Checks if a player's data has been modified since last save.
     *
     * @param playerId The UUID of the player
     * @return true if the player's data is dirty, false otherwise
     */
    public boolean isDirty(UUID playerId) {
        return dirtyPlayers.contains(playerId);
    }

    /**
     * Gets all player UUIDs whose data has been modified.
     * Returns a copy to prevent concurrent modification issues.
     *
     * @return A set of UUIDs for players with dirty data
     */
    public Set<UUID> getDirtyPlayers() {
        return new HashSet<>(dirtyPlayers);
    }

    /**
     * Removes a player's data from the cache.
     * Also removes the dirty flag if present.
     *
     * @param playerId The UUID of the player
     * @return The removed PlayerVaultData, or null if not present
     */
    public PlayerVaultData remove(UUID playerId) {
        dirtyPlayers.remove(playerId);
        return cache.remove(playerId);
    }

    /**
     * Checks if a player's data is currently cached.
     *
     * @param playerId The UUID of the player
     * @return true if the player's data is in cache, false otherwise
     */
    public boolean contains(UUID playerId) {
        return cache.containsKey(playerId);
    }

    /**
     * Gets all cached player UUIDs.
     * Returns a copy to prevent concurrent modification issues.
     *
     * @return A set of all cached player UUIDs
     */
    public Set<UUID> getCachedPlayers() {
        return new HashSet<>(cache.keySet());
    }

    /**
     * Gets the number of players currently cached.
     *
     * @return The cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Gets the number of players with dirty data.
     *
     * @return The number of dirty players
     */
    public int dirtyCount() {
        return dirtyPlayers.size();
    }

    /**
     * Clears all cached data and dirty flags.
     * This should only be used during plugin shutdown or reload.
     */
    public void clear() {
        cache.clear();
        dirtyPlayers.clear();
    }

    /**
     * Updates player vault data in the cache and marks it as dirty.
     * This is a convenience method combining put() and markDirty().
     *
     * @param playerId The UUID of the player
     * @param data     The vault data to cache
     */
    public void putAndMarkDirty(UUID playerId, PlayerVaultData data) {
        put(playerId, data);
        markDirty(playerId);
    }
    
    /**
     * Gets cache statistics for monitoring and debugging.
     *
     * @return A formatted string with cache statistics
     */
    public String getStatistics() {
        int totalCached = cache.size();
        int totalDirty = dirtyPlayers.size();
        int totalClean = totalCached - totalDirty;
        
        return String.format("Cache Stats: Total=%d, Clean=%d, Dirty=%d", 
            totalCached, totalClean, totalDirty);
    }
    
    /**
     * Removes all cached data for offline players.
     * This helps prevent memory leaks from players who disconnected without proper cleanup.
     *
     * @param onlinePlayerIds Set of UUIDs for currently online players
     * @return The number of entries removed
     */
    public int cleanupOfflinePlayers(Set<UUID> onlinePlayerIds) {
        int removedCount = 0;
        
        for (UUID playerId : getCachedPlayers()) {
            if (!onlinePlayerIds.contains(playerId)) {
                // Player is offline, remove from cache
                remove(playerId);
                removedCount++;
            }
        }
        
        return removedCount;
    }
}
