package com.voidvault.config;

import com.voidvault.model.ButtonConfig;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Manages loading, validation, and access to the plugin's configuration.
 * Handles config.yml with all plugin settings including mode, cooldowns, economy, and GUI items.
 */
public class ConfigManager {
    private final Plugin plugin;
    private final Logger logger;
    private FileConfiguration config;
    
    // Cached configuration values
    private PluginMode pluginMode;
    private String storageType;
    private int cooldownSeconds;
    private double economyFee;
    private int autoSaveInterval;
    private int defaultSlots;
    private int defaultPages;
    
    // Search settings
    private boolean searchGrayoutEnabled;
    private String searchMode;
    private boolean searchCaseSensitive;
    
    // Feature toggles
    private boolean sortEnabled;
    private boolean quickDepositEnabled;
    private boolean searchEnabled;
    private boolean lockedSlotsEnabled;
    
    // GUI titles
    private String simpleModeTitle;
    private String pagedModeTitle;
    private String adminModeTitle;
    
    // Button configurations
    private ButtonConfig lockedSlotItem;
    private ButtonConfig fillerBarItem;
    private ButtonConfig previousPageButton;
    private ButtonConfig nextPageButton;
    private ButtonConfig filterButton;
    private ButtonConfig searchButton;
    private ButtonConfig sortButton;
    private ButtonConfig quickDepositButton;
    private ButtonConfig filteredSlotItem;
    
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Load or reload the configuration from config.yml.
     * Generates default config if it doesn't exist.
     */
    public void load() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        // Reload config from disk
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load and validate all configuration values
        loadPluginMode();
        loadStorageSettings();
        loadRemoteAccessSettings();
        loadAutoSaveSettings();
        loadDefaultValues();
        loadGuiTitles();
        loadItemConfigurations();
        loadSearchSettings();
        loadFeatureToggles();
        
        logger.info("Configuration loaded successfully. Mode: " + pluginMode);
    }
    
    /**
     * Reload the configuration from disk.
     */
    public void reload() {
        load();
        logger.info("Configuration reloaded.");
    }
    
    private void loadPluginMode() {
        String modeString = config.getString("plugin-mode", "PAGED");
        pluginMode = PluginMode.fromString(modeString);
        
        if (!modeString.equalsIgnoreCase(pluginMode.name())) {
            logger.warning("Invalid plugin-mode value '" + modeString + "'. Defaulting to " + pluginMode);
        }
    }
    
    private void loadStorageSettings() {
        storageType = config.getString("storage-type", "YAML").toUpperCase();
        
        if (!storageType.equals("YAML") && !storageType.equals("MYSQL")) {
            logger.warning("Invalid storage-type '" + storageType + "'. Defaulting to YAML.");
            storageType = "YAML";
        }
    }
    
    private void loadRemoteAccessSettings() {
        cooldownSeconds = config.getInt("remote-access.cooldown-seconds", 60);
        economyFee = config.getDouble("remote-access.economy-fee", 100.0);
        
        if (cooldownSeconds < 0) {
            logger.warning("Invalid cooldown-seconds value. Defaulting to 60.");
            cooldownSeconds = 60;
        }
        
        if (economyFee < 0) {
            logger.warning("Invalid economy-fee value. Defaulting to 100.0.");
            economyFee = 100.0;
        }
    }
    
    private void loadAutoSaveSettings() {
        autoSaveInterval = config.getInt("auto-save-interval", 5);
        
        if (autoSaveInterval < 1) {
            logger.warning("Invalid auto-save-interval value. Defaulting to 5 minutes.");
            autoSaveInterval = 5;
        }
    }
    
    private void loadGuiTitles() {
        simpleModeTitle = config.getString("gui-titles.simple", "&5&lVault");
        pagedModeTitle = config.getString("gui-titles.paged", "&5&lVoidVault &8- &7Page {page}");
        adminModeTitle = config.getString("gui-titles.admin", "&5&l{target}'s Vault &8- &7Page {page}");
        
        // Translate color codes
        simpleModeTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', simpleModeTitle);
        pagedModeTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', pagedModeTitle);
        adminModeTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', adminModeTitle);
    }
    
    private void loadDefaultValues() {
        // Load mode-specific defaults
        PluginMode mode = getPluginMode();
        
        if (mode == PluginMode.SIMPLE) {
            // SIMPLE mode defaults
            defaultSlots = config.getInt("defaults.simple.slots", 27);
            defaultPages = 1; // SIMPLE mode always has 1 page
            
            // Validate SIMPLE mode slots (9-54)
            int[] validSlots = {9, 18, 27, 36, 45, 54};
            boolean validSlotCount = false;
            for (int validSlot : validSlots) {
                if (defaultSlots == validSlot) {
                    validSlotCount = true;
                    break;
                }
            }
            
            if (!validSlotCount) {
                logger.warning("Invalid defaults.simple.slots value '" + defaultSlots + "'. Must be one of: 9, 18, 27, 36, 45, 54. Defaulting to 27.");
                defaultSlots = 27;
            }
        } else {
            // PAGED mode defaults
            defaultSlots = config.getInt("defaults.paged.slots", 52);
            defaultPages = config.getInt("defaults.paged.pages", 1);
            
            // Validate PAGED mode slots (9-52)
            int[] validSlots = {9, 18, 27, 36, 45, 52};
            boolean validSlotCount = false;
            for (int validSlot : validSlots) {
                if (defaultSlots == validSlot) {
                    validSlotCount = true;
                    break;
                }
            }
            
            if (!validSlotCount) {
                logger.warning("Invalid defaults.paged.slots value '" + defaultSlots + "'. Must be one of: 9, 18, 27, 36, 45, 52. Defaulting to 52.");
                defaultSlots = 52;
            }
            
            // Validate default-pages is between 1 and 5
            if (defaultPages < 1 || defaultPages > 5) {
                logger.warning("Invalid defaults.paged.pages value '" + defaultPages + "'. Must be between 1 and 5. Defaulting to 1.");
                defaultPages = 1;
            }
        }
    }
    
    private void loadItemConfigurations() {
        lockedSlotItem = loadButtonConfig("items.locked-slot", 
            Material.RED_STAINED_GLASS_PANE, "&c&lLocked", 
            List.of("&7Upgrade your vault to", "&7unlock this slot!"), false);
            
        fillerBarItem = loadButtonConfig("items.filler-bar",
            Material.GRAY_STAINED_GLASS_PANE, " ",
            List.of(), false);
            
        previousPageButton = loadButtonConfig("items.previous-page",
            Material.ARROW, "&e&lPrevious Page",
            List.of("&7Click to go back"), false);
            
        nextPageButton = loadButtonConfig("items.next-page",
            Material.ARROW, "&e&lNext Page",
            List.of("&7Click to continue"), false);
            
        filterButton = loadButtonConfig("items.filter",
            Material.PAPER, "&d&lFilter Items",
            List.of("&7Filter by item type", "&7Right-click to clear filter"), false);
            
        searchButton = loadButtonConfig("items.search",
            Material.COMPASS, "&e&lSearch Items",
            List.of("&7Search for items", "&7Type in chat to search"), false);
            
        sortButton = loadButtonConfig("items.sort",
            Material.HOPPER, "&a&lSort Items",
            List.of("&7Organize your vault"), true);
            
        quickDepositButton = loadButtonConfig("items.quick-deposit",
            Material.CHEST, "&b&lQuick Deposit",
            List.of("&7Deposit matching items"), true);
            
        filteredSlotItem = loadButtonConfig("items.filtered-slot",
            Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7&oFiltered",
            List.of("&7This item doesn't match", "&7your search query"), false);
    }
    
    private void loadSearchSettings() {
        searchGrayoutEnabled = config.getBoolean("search.grayout-enabled", true);
        searchMode = config.getString("search.search-mode", "all");
        searchCaseSensitive = config.getBoolean("search.case-sensitive", false);
        
        if (!searchMode.equals("name") && !searchMode.equals("all")) {
            logger.warning("Invalid search-mode '" + searchMode + "'. Defaulting to 'all'.");
            searchMode = "all";
        }
    }
    
    private void loadFeatureToggles() {
        sortEnabled = config.getBoolean("features.sort-enabled", true);
        quickDepositEnabled = config.getBoolean("features.quick-deposit-enabled", true);
        searchEnabled = config.getBoolean("features.search-enabled", true);
        lockedSlotsEnabled = config.getBoolean("features.locked-slots-enabled", true);
    }
    
    private ButtonConfig loadButtonConfig(String path, Material defaultMaterial, 
                                         String defaultName, List<String> defaultLore, 
                                         boolean defaultGlow) {
        ConfigurationSection section = config.getConfigurationSection(path);
        
        if (section == null) {
            return new ButtonConfig(defaultMaterial, defaultName, defaultLore, defaultGlow);
        }
        
        Material material = Material.getMaterial(section.getString("material", defaultMaterial.name()));
        if (material == null) {
            logger.warning("Invalid material for " + path + ". Using default.");
            material = defaultMaterial;
        }
        
        String displayName = section.getString("display-name", defaultName);
        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty() && !defaultLore.isEmpty()) {
            lore = defaultLore;
        }
        boolean glow = section.getBoolean("glow", defaultGlow);
        
        return new ButtonConfig(material, displayName, lore, glow);
    }
    
    // Getters for configuration values
    
    public PluginMode getPluginMode() {
        return pluginMode;
    }
    
    public String getStorageType() {
        return storageType;
    }
    
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    public double getEconomyFee() {
        return economyFee;
    }
    
    public boolean isRemoteAccessEnabled() {
        return config.getBoolean("remote-access.enabled", true);
    }
    
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }
    
    public int getDefaultSlots() {
        return defaultSlots;
    }
    
    public int getDefaultPages() {
        return defaultPages;
    }
    
    public ButtonConfig getLockedSlotItem() {
        return lockedSlotItem;
    }
    
    public ButtonConfig getFillerBarItem() {
        return fillerBarItem;
    }
    
    public ButtonConfig getPreviousPageButton() {
        return previousPageButton;
    }
    
    public ButtonConfig getNextPageButton() {
        return nextPageButton;
    }
    
    public ButtonConfig getSortButton() {
        return sortButton;
    }
    
    public ButtonConfig getFilterButton() {
        return filterButton;
    }
    
    public ButtonConfig getSearchButton() {
        return searchButton;
    }
    
    public ButtonConfig getQuickDepositButton() {
        return quickDepositButton;
    }
    
    public ButtonConfig getFilteredSlotItem() {
        return filteredSlotItem;
    }
    
    // Search settings getters
    
    public boolean isSearchGrayoutEnabled() {
        return searchGrayoutEnabled;
    }
    
    public String getSearchMode() {
        return searchMode;
    }
    
    public boolean isSearchCaseSensitive() {
        return searchCaseSensitive;
    }
    
    // Feature toggle getters
    
    public boolean isSortEnabled() {
        return sortEnabled;
    }
    
    public boolean isQuickDepositEnabled() {
        return quickDepositEnabled;
    }
    
    public boolean isSearchEnabled() {
        return searchEnabled;
    }
    
    public boolean isLockedSlotsEnabled() {
        return lockedSlotsEnabled;
    }
    
    // MySQL configuration getters
    
    public String getMySqlHost() {
        return config.getString("mysql.host", "localhost");
    }
    
    public int getMySqlPort() {
        return config.getInt("mysql.port", 3306);
    }
    
    public String getMySqlDatabase() {
        return config.getString("mysql.database", "voidvault");
    }
    
    public String getMySqlUsername() {
        return config.getString("mysql.username", "root");
    }
    
    public String getMySqlPassword() {
        return config.getString("mysql.password", "password");
    }
    
    public int getMySqlPoolSize() {
        return config.getInt("mysql.pool-size", 10);
    }
    
    // GUI Title getters
    
    /**
     * Get the GUI title for SIMPLE mode.
     * Supports placeholders: {player}
     *
     * @param playerName The player's name
     * @return The formatted title
     */
    public String getSimpleModeTitle(String playerName) {
        return simpleModeTitle.replace("{player}", playerName);
    }
    
    /**
     * Get the GUI title for PAGED mode.
     * Supports placeholders: {player}, {page}, {maxPages}
     *
     * @param playerName The player's name
     * @param page The current page number
     * @param maxPages The maximum number of pages
     * @return The formatted title
     */
    public String getPagedModeTitle(String playerName, int page, int maxPages) {
        return pagedModeTitle
            .replace("{player}", playerName)
            .replace("{page}", String.valueOf(page))
            .replace("{maxPages}", String.valueOf(maxPages));
    }
    
    /**
     * Get the GUI title for admin viewing mode.
     * Supports placeholders: {player}, {target}, {page}, {maxPages}
     *
     * @param adminName The admin's name
     * @param targetName The target player's name
     * @param page The current page number
     * @param maxPages The maximum number of pages
     * @return The formatted title
     */
    public String getAdminModeTitle(String adminName, String targetName, int page, int maxPages) {
        return adminModeTitle
            .replace("{player}", adminName)
            .replace("{target}", targetName)
            .replace("{page}", String.valueOf(page))
            .replace("{maxPages}", String.valueOf(maxPages));
    }
}
