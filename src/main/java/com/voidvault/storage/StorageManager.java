package com.voidvault.storage;

import com.voidvault.model.PlayerVaultData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing vault data persistence.
 * All implementations must support asynchronous operations to prevent main thread blocking.
 * <p>
 * Implementations: YamlStorage, MySqlStorage
 */
public interface StorageManager {

    /**
     * Asynchronously loads a player's vault data from storage.
     *
     * @param playerId The UUID of the player
     * @return A CompletableFuture containing the player's vault data
     */
    CompletableFuture<PlayerVaultData> loadPlayerData(UUID playerId);

    /**
     * Asynchronously saves a player's vault data to storage.
     *
     * @param playerId The UUID of the player
     * @param data     The vault data to save
     * @return A CompletableFuture that completes when the save operation finishes
     */
    CompletableFuture<Void> savePlayerData(UUID playerId, PlayerVaultData data);

    /**
     * Asynchronously saves all cached player data to storage.
     * This is typically called during auto-save or plugin shutdown.
     *
     * @return A CompletableFuture that completes when all save operations finish
     */
    CompletableFuture<Void> saveAll();

    /**
     * Closes the storage manager and releases any resources.
     * This should be called during plugin shutdown.
     */
    void close();

    /**
     * Initializes the storage backend.
     * This is called during plugin startup.
     *
     * @return A CompletableFuture that completes when initialization finishes
     */
    CompletableFuture<Void> initialize();
}
