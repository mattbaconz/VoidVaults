package com.voidvault;

import com.voidvault.command.EChestCommand;
import com.voidvault.command.VoidVaultCommand;
import com.voidvault.command.VoidVaultTabCompleter;
import com.voidvault.config.ConfigManager;
import com.voidvault.config.MessageManager;
import com.voidvault.integration.PlaceholderAPIHook;
import com.voidvault.listener.EnderChestListener;
import com.voidvault.listener.PlayerConnectionListener;
import com.voidvault.listener.SearchInputListener;
import com.voidvault.listener.VaultInventoryListener;
import com.voidvault.manager.CooldownManager;
import com.voidvault.manager.EconomyManager;
import com.voidvault.manager.PermissionManager;
import com.voidvault.manager.VaultManager;
import com.voidvault.storage.DataCache;
import com.voidvault.storage.MySqlStorage;
import com.voidvault.storage.StorageManager;
import com.voidvault.storage.YamlStorage;
import com.voidvault.util.SchedulerUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Main plugin class for VoidVault
 * Modern, performance-first Ender Chest replacement with dual-mode system
 */
public class VoidVaultPlugin extends JavaPlugin {

    // Core managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PermissionManager permissionManager;
    private CooldownManager cooldownManager;
    private EconomyManager economyManager;
    private VaultManager vaultManager;
    
    // Storage
    private DataCache dataCache;
    private StorageManager storageManager;
    
    // Integrations
    private PlaceholderAPIHook placeholderAPIHook;
    
    // Auto-save task
    private AutoSaveTask autoSaveTask;

    @Override
    public void onEnable() {
        getLogger().info("VoidVault is enabling...");
        
        try {
            // Initialize SchedulerUtil for Folia detection
            SchedulerUtil.init(this);
            
            // Initialize all managers
            initializeManagers();
            
            // Initialize storage
            initializeStorage();
            
            // Register event listeners
            registerListeners();
            
            // Register commands
            registerCommands();
            
            // Set up integrations
            setupIntegrations();
            
            // Start auto-save task
            startAutoSave();
            
            // Initialize bStats
            initializeMetrics();
            
            getLogger().info("VoidVault enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable VoidVault", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("VoidVault is disabling...");
        
        try {
            // Stop auto-save task
            if (autoSaveTask != null) {
                autoSaveTask.cancel();
                getLogger().info("Auto-save task stopped.");
            }
            
            // Perform synchronous save of all cached data
            if (storageManager != null && dataCache != null) {
                getLogger().info("Saving all vault data...");
                saveAllDataSync();
                getLogger().info("All vault data saved successfully.");
            }
            
            // Close storage manager
            if (storageManager != null) {
                storageManager.close();
            }
            
            // Stop cooldown cleanup task
            if (cooldownManager != null) {
                cooldownManager.shutdown();
            }
            
            getLogger().info("VoidVault disabled successfully.");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }
    
    /**
     * Initialize all manager instances.
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");
        
        // Configuration managers
        configManager = new ConfigManager(this);
        configManager.load();
        
        messageManager = new MessageManager(this);
        messageManager.load();
        
        // Data cache
        dataCache = new DataCache();
        
        // Permission manager (needs dataCache, so we'll initialize it after storage)
        // Temporarily set to null, will be initialized in initializeStorage()
        permissionManager = null;
        
        // Cooldown manager
        cooldownManager = new CooldownManager(this, configManager);
        
        // Economy manager
        economyManager = new EconomyManager(this);
        
        getLogger().info("Managers initialized.");
    }
    
    /**
     * Initialize storage backend based on configuration.
     */
    private void initializeStorage() {
        getLogger().info("Initializing storage...");
        
        String storageType = configManager.getStorageType();
        
        // Create appropriate storage implementation
        storageManager = switch (storageType) {
            case "MYSQL" -> {
                getLogger().info("Using MySQL storage backend");
                yield new MySqlStorage(this, dataCache);
            }
            case "YAML" -> {
                getLogger().info("Using YAML storage backend");
                yield new YamlStorage(this, dataCache);
            }
            default -> {
                getLogger().warning("Unknown storage type '" + storageType + "', defaulting to YAML");
                yield new YamlStorage(this, dataCache);
            }
        };
        
        // Initialize storage asynchronously
        storageManager.initialize()
            .exceptionally(ex -> {
                getLogger().log(Level.SEVERE, "Failed to initialize storage", ex);
                return null;
            });
        
        // Initialize permission manager (needs dataCache)
        permissionManager = new PermissionManager(configManager, dataCache);
        
        // Initialize vault manager (depends on storage and permission manager)
        vaultManager = new VaultManager(this, configManager, messageManager, 
            permissionManager, storageManager, dataCache);
        
        getLogger().info("Storage initialized.");
    }
    
    /**
     * Register all event listeners.
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");
        
        // Ender chest interaction listener
        getServer().getPluginManager().registerEvents(
            new EnderChestListener(vaultManager, messageManager, getLogger()), this);
        
        // Vault inventory interaction listener
        getServer().getPluginManager().registerEvents(
            new VaultInventoryListener(vaultManager, getLogger()), this);
        
        // Player connection listener
        getServer().getPluginManager().registerEvents(
            new PlayerConnectionListener(storageManager, dataCache, vaultManager, getLogger()), this);
        
        // Search input listener
        getServer().getPluginManager().registerEvents(
            new SearchInputListener(vaultManager.getSearchManager(), vaultManager, messageManager), this);
        
        getLogger().info("Event listeners registered.");
    }
    
    /**
     * Register all commands and tab completers.
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");
        
        // Register /voidvaults command
        PluginCommand voidVaultCmd = getCommand("voidvaults");
        if (voidVaultCmd != null) {
            VoidVaultCommand voidVaultExecutor = new VoidVaultCommand(
                this, configManager, messageManager, vaultManager, dataCache, storageManager);
            voidVaultCmd.setExecutor(voidVaultExecutor);
            voidVaultCmd.setTabCompleter(new VoidVaultTabCompleter());
            getLogger().info("Successfully registered /voidvaults command with aliases: " + voidVaultCmd.getAliases());
        } else {
            getLogger().severe("Failed to register /voidvaults command - command not found in plugin.yml");
        }
        
        // Register /echest command (with aliases /pv and /vault)
        PluginCommand echestCmd = getCommand("echest");
        if (echestCmd != null) {
            EChestCommand echestExecutor = new EChestCommand(
                this, vaultManager, permissionManager, cooldownManager, economyManager, messageManager);
            echestCmd.setExecutor(echestExecutor);
            getLogger().info("Successfully registered /echest command with aliases: " + echestCmd.getAliases());
            getLogger().info("Remote access commands available: /echest, /pv, /vault");
        } else {
            getLogger().severe("Failed to register /echest command - command not found in plugin.yml");
            getLogger().severe("Remote access will NOT work! Check plugin.yml for 'echest' command definition");
        }
        
        getLogger().info("Commands registered.");
    }
    
    /**
     * Set up integrations with external plugins (Vault, PlaceholderAPI).
     */
    private void setupIntegrations() {
        getLogger().info("Setting up integrations...");
        
        // Initialize economy integration (Vault)
        economyManager.initialize();
        
        // Initialize PlaceholderAPI integration (only if PlaceholderAPI is present)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderAPIHook = new PlaceholderAPIHook(this, permissionManager, vaultManager);
                placeholderAPIHook.initialize();
            } catch (Exception e) {
                getLogger().warning("Failed to initialize PlaceholderAPI integration: " + e.getMessage());
                getLogger().info("PlaceholderAPI features will be disabled.");
            }
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholder features disabled.");
        }
        
        getLogger().info("Integrations set up.");
    }
    
    /**
     * Initialize bStats metrics.
     */
    private void initializeMetrics() {
        try {
            Metrics metrics = new Metrics(this, 28100);
            getLogger().info("bStats metrics initialized.");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }
    }
    
    /**
     * Start the auto-save task that periodically saves dirty player data.
     */
    private void startAutoSave() {
        int intervalMinutes = configManager.getAutoSaveInterval();
        long intervalTicks = intervalMinutes * 60L * 20L; // Convert minutes to ticks
        
        getLogger().info("Starting auto-save task with interval: " + intervalMinutes + " minutes");
        
        autoSaveTask = new AutoSaveTask(this, storageManager, dataCache, getLogger());
        
        // Schedule repeating task with initial delay equal to the interval
        SchedulerUtil.runAsyncRepeating(this, autoSaveTask, intervalTicks, intervalTicks);
    }
    
    /**
     * Perform a synchronous save of all cached vault data.
     * This is called during plugin shutdown to prevent data loss.
     */
    private void saveAllDataSync() {
        // Get all cached players
        var cachedPlayers = dataCache.getCachedPlayers();
        
        if (cachedPlayers.isEmpty()) {
            getLogger().info("No cached data to save.");
            return;
        }
        
        getLogger().info("Saving data for " + cachedPlayers.size() + " players...");
        
        // Create a list of save futures
        var saveFutures = cachedPlayers.stream()
            .map(playerId -> {
                return dataCache.get(playerId)
                    .map(data -> storageManager.savePlayerData(playerId, data)
                        .exceptionally(ex -> {
                            getLogger().severe("Failed to save data for player " + playerId + ": " + ex.getMessage());
                            return null;
                        }))
                    .orElse(CompletableFuture.completedFuture(null));
            })
            .toArray(CompletableFuture[]::new);
        
        // Wait for all saves to complete
        try {
            CompletableFuture.allOf(saveFutures).join();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during synchronous save", e);
        }
    }
    
    // Public getters for managers (if needed by other components)
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public VaultManager getVaultManager() {
        return vaultManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public DataCache getDataCache() {
        return dataCache;
    }
}
