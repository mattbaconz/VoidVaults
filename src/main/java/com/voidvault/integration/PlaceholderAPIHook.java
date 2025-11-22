package com.voidvault.integration;

import com.voidvault.manager.PermissionManager;
import com.voidvault.manager.VaultManager;
import com.voidvault.gui.PagedVaultGUI;
import com.voidvault.gui.VaultGUI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * PlaceholderAPI integration for VoidVaults.
 * Provides placeholders for displaying vault information in other plugins.
 * <p>
 * Supported placeholders:
 * - %voidvaults_current_page% - The current page number the player is viewing
 * - %voidvaults_max_pages% - The maximum number of pages the player can access
 * - %voidvaults_max_slots% - The maximum number of slots the player can access
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    private final Plugin plugin;
    private final Logger logger;
    private final PermissionManager permissionManager;
    private final VaultManager vaultManager;
    private boolean registered;
    
    /**
     * Constructor for PlaceholderAPIHook.
     *
     * @param plugin            The plugin instance
     * @param permissionManager Permission manager for calculating max slots/pages
     * @param vaultManager      Vault manager for getting current page information
     */
    public PlaceholderAPIHook(Plugin plugin, PermissionManager permissionManager, VaultManager vaultManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.permissionManager = permissionManager;
        this.vaultManager = vaultManager;
        this.registered = false;
    }
    
    /**
     * Initialize and register the PlaceholderAPI expansion.
     * Should be called during plugin enable after PlaceholderAPI is loaded.
     * Handles the case where PlaceholderAPI is not present gracefully.
     *
     * @return true if registration was successful, false otherwise
     */
    public boolean initialize() {
        // Check if PlaceholderAPI is present
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            logger.info("PlaceholderAPI not found. Placeholder features disabled.");
            registered = false;
            return false;
        }
        
        try {
            // Register the expansion
            if (register()) {
                registered = true;
                logger.info("PlaceholderAPI integration enabled. Registered placeholders: " +
                    "%voidvaults_current_page%, %voidvaults_max_pages%, %voidvaults_max_slots%");
                return true;
            } else {
                logger.warning("Failed to register PlaceholderAPI expansion.");
                registered = false;
                return false;
            }
        } catch (Exception e) {
            logger.warning("Error initializing PlaceholderAPI integration: " + e.getMessage());
            registered = false;
            return false;
        }
    }
    
    /**
     * Check if the PlaceholderAPI integration is active.
     *
     * @return true if the expansion is registered
     */
    public boolean isActive() {
        return registered;
    }
    
    /**
     * Unregister the expansion.
     * Should be called during plugin disable.
     */
    public void shutdown() {
        if (registered) {
            unregister();
            registered = false;
            logger.info("PlaceholderAPI integration disabled.");
        }
    }
    
    /**
     * Get the identifier for this expansion.
     * This is what players will use in placeholders: %voidvaults_...%
     *
     * @return The identifier "voidvaults"
     */
    @Override
    public @NotNull String getIdentifier() {
        return "voidvaults";
    }
    
    /**
     * Get the author of this expansion.
     *
     * @return The plugin author
     */
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    /**
     * Get the version of this expansion.
     * Uses the plugin version.
     *
     * @return The plugin version
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    /**
     * Indicate that this expansion should persist through reloads.
     *
     * @return true to persist
     */
    @Override
    public boolean persist() {
        return true;
    }
    
    /**
     * Handle placeholder requests.
     * Processes the placeholder identifier and returns the appropriate value.
     *
     * @param player     The player requesting the placeholder
     * @param identifier The placeholder identifier (without %voidvaults_ prefix)
     * @return The placeholder value, or null if the placeholder is not recognized
     */
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return null;
        }
        
        return switch (identifier.toLowerCase()) {
            case "current_page" -> getCurrentPage(player);
            case "max_pages" -> String.valueOf(permissionManager.getMaxPages(player));
            case "max_slots" -> String.valueOf(permissionManager.getMaxSlots(player));
            default -> null;
        };
    }
    
    /**
     * Get the current page number the player is viewing.
     * Returns "1" if the player doesn't have a vault open.
     *
     * @param player The player to check
     * @return The current page number as a string
     */
    private String getCurrentPage(Player player) {
        VaultGUI gui = vaultManager.getOpenGUI(player);
        
        if (gui instanceof PagedVaultGUI pagedGui) {
            return String.valueOf(pagedGui.getPage());
        }
        
        // Default to page 1 if no vault is open or in SIMPLE mode
        return "1";
    }
}
