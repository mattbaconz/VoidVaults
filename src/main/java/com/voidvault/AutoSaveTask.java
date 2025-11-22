package com.voidvault;

import com.voidvault.storage.DataCache;
import com.voidvault.storage.StorageManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auto-save task that periodically saves dirty player vault data.
 * This task runs asynchronously to prevent main thread blocking.
 */
public class AutoSaveTask implements Runnable {
    
    private final VoidVaultPlugin plugin;
    private final StorageManager storageManager;
    private final DataCache dataCache;
    private final Logger logger;
    private volatile boolean cancelled = false;
    
    public AutoSaveTask(VoidVaultPlugin plugin, StorageManager storageManager, 
                       DataCache dataCache, Logger logger) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.dataCache = dataCache;
        this.logger = logger;
    }
    
    @Override
    public void run() {
        if (cancelled) {
            return;
        }
        
        try {
            performAutoSave();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during auto-save", e);
        }
    }
    
    /**
     * Perform the auto-save operation.
     * Saves only dirty players to optimize performance.
     */
    private void performAutoSave() {
        long startTime = System.currentTimeMillis();
        
        // Get all dirty players (snapshot to avoid concurrent modification)
        Set<UUID> dirtyPlayers = dataCache.getDirtyPlayers();
        
        if (dirtyPlayers.isEmpty()) {
            logger.fine("Auto-save: No dirty data to save");
            return;
        }
        
        logger.info("Auto-save: Saving data for " + dirtyPlayers.size() + " player(s)...");
        
        // Track successful and failed saves
        int[] successCount = {0};
        int[] failCount = {0};
        
        // Create save futures for all dirty players
        CompletableFuture<?>[] saveFutures = dirtyPlayers.stream()
            .map(playerId -> {
                return dataCache.get(playerId)
                    .map(data -> storageManager.savePlayerData(playerId, data)
                        .thenRun(() -> {
                            dataCache.clearDirty(playerId);
                            successCount[0]++;
                        })
                        .exceptionally(ex -> {
                            logger.severe("Auto-save: Failed to save data for player " + 
                                playerId + ": " + ex.getMessage());
                            failCount[0]++;
                            return null;
                        }))
                    .orElseGet(() -> {
                        logger.warning("Auto-save: No data found for dirty player " + playerId);
                        dataCache.clearDirty(playerId); // Clear dirty flag for missing data
                        return CompletableFuture.completedFuture(null);
                    });
            })
            .toArray(CompletableFuture[]::new);
        
        // Wait for all saves to complete with timeout handling
        CompletableFuture.allOf(saveFutures)
            .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .thenRun(() -> {
                long duration = System.currentTimeMillis() - startTime;
                logger.info(String.format("Auto-save: Completed in %dms (Success: %d, Failed: %d)", 
                    duration, successCount[0], failCount[0]));
            })
            .exceptionally(ex -> {
                logger.log(Level.SEVERE, "Auto-save: Error or timeout during save operations", ex);
                return null;
            });
    }
    
    /**
     * Cancel this auto-save task.
     * The task will stop executing on the next run.
     */
    public void cancel() {
        this.cancelled = true;
    }
    
    /**
     * Check if this task has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
