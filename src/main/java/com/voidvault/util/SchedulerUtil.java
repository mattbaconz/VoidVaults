package com.voidvault.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for scheduling tasks with Folia compatibility.
 * Automatically detects whether the server is running Folia or standard Paper
 * and uses the appropriate scheduler API.
 */
public class SchedulerUtil {
    
    private static boolean isFolia = false;
    private static boolean initialized = false;
    
    /**
     * Initialize the scheduler utility by detecting the server type.
     * This must be called during plugin initialization.
     *
     * @param plugin The plugin instance
     */
    public static void init(Plugin plugin) {
        if (initialized) {
            return;
        }
        
        try {
            // Try to load Folia-specific class
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            plugin.getLogger().info("Detected Folia server - using regionized scheduler");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            plugin.getLogger().info("Detected Paper server - using standard scheduler");
        }
        
        initialized = true;
    }
    
    /**
     * Check if the server is running Folia.
     *
     * @return true if Folia is detected, false otherwise
     */
    public static boolean isFolia() {
        return isFolia;
    }
    
    /**
     * Run a task asynchronously.
     * Uses the appropriate async scheduler based on server type.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        if (isFolia) {
            // Folia: Use async scheduler
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            // Paper: Use standard async task
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    /**
     * Run a task synchronously on the main thread (Paper) or entity thread (Folia).
     * For Folia, this requires a player context to determine the correct region.
     *
     * @param plugin The plugin instance
     * @param player The player for entity context (required for Folia)
     * @param task The task to run
     */
    public static void runSync(Plugin plugin, Player player, Runnable task) {
        if (isFolia) {
            // Folia: Use entity scheduler for the player's region
            player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            // Paper: Use standard sync task
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run a task synchronously on the main thread (Paper only).
     * This method should only be used when no player context is available.
     * On Folia, this will use the global region scheduler.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     */
    public static void runSync(Plugin plugin, Runnable task) {
        if (isFolia) {
            // Folia: Use global region scheduler
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            // Paper: Use standard sync task
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run a task asynchronously after a delay.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delay The delay in ticks (Paper) or milliseconds (Folia)
     */
    public static void runAsyncDelayed(Plugin plugin, Runnable task, long delay) {
        if (isFolia) {
            // Folia: Use async scheduler with delay in milliseconds
            long delayMs = delay * 50; // Convert ticks to milliseconds
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), 
                delayMs, TimeUnit.MILLISECONDS);
        } else {
            // Paper: Use standard delayed async task (delay in ticks)
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }
    
    /**
     * Run a task synchronously after a delay.
     *
     * @param plugin The plugin instance
     * @param player The player for entity context (required for Folia)
     * @param task The task to run
     * @param delay The delay in ticks
     */
    public static void runSyncDelayed(Plugin plugin, Player player, Runnable task, long delay) {
        if (isFolia) {
            // Folia: Use entity scheduler with delay
            player.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delay);
        } else {
            // Paper: Use standard delayed sync task
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
    
    /**
     * Run a task synchronously after a delay (no player context).
     *
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delay The delay in ticks
     */
    public static void runSyncDelayed(Plugin plugin, Runnable task, long delay) {
        if (isFolia) {
            // Folia: Use global region scheduler with delay
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delay);
        } else {
            // Paper: Use standard delayed sync task
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
    
    /**
     * Run a repeating task asynchronously.
     *
     * @param plugin The plugin instance
     * @param task The task to run
     * @param delay The initial delay in ticks (Paper) or milliseconds (Folia)
     * @param period The period between executions in ticks (Paper) or milliseconds (Folia)
     */
    public static void runAsyncRepeating(Plugin plugin, Runnable task, long delay, long period) {
        if (isFolia) {
            // Folia: Use async scheduler with repeating task
            long delayMs = delay * 50; // Convert ticks to milliseconds
            long periodMs = period * 50; // Convert ticks to milliseconds
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), 
                delayMs, periodMs, TimeUnit.MILLISECONDS);
        } else {
            // Paper: Use standard repeating async task
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        }
    }
}
