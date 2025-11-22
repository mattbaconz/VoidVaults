package com.voidvault.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages loading and sending of localized messages with color code and placeholder support.
 * Handles messages.yml with all player-facing messages.
 */
public class MessageManager {
    private final Plugin plugin;
    private final Logger logger;
    private final File messagesFile;
    private FileConfiguration messages;
    
    // Legacy color code serializer for & codes
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();
    
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }
    
    /**
     * Load or reload messages from messages.yml.
     * Generates default messages.yml if it doesn't exist.
     */
    public void load() {
        // Create messages.yml if it doesn't exist
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            logger.info("Created default messages.yml");
        }
        
        // Load messages from file
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults from jar
        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                messages.setDefaults(defaultMessages);
            }
        } catch (IOException e) {
            logger.warning("Could not load default messages: " + e.getMessage());
        }
        
        logger.info("Messages loaded successfully.");
    }
    
    /**
     * Reload messages from disk.
     */
    public void reload() {
        load();
        logger.info("Messages reloaded.");
    }
    
    /**
     * Get a raw message string from messages.yml.
     *
     * @param key the message key (e.g., "commands.reload-success")
     * @return the raw message string, or the key if not found
     */
    public String getRaw(String key) {
        String message = messages.getString(key);
        if (message == null) {
            logger.warning("Missing message key: " + key);
            return key;
        }
        return message;
    }
    
    /**
     * Get a message with placeholders replaced.
     *
     * @param key the message key
     * @param placeholders map of placeholder names to values
     * @return the formatted message string
     */
    public String get(String key, Map<String, String> placeholders) {
        String message = getRaw(key);
        
        // Replace prefix placeholder
        String prefix = getRaw("prefix");
        message = message.replace("{prefix}", prefix);
        
        // Replace custom placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * Get a message without placeholders.
     *
     * @param key the message key
     * @return the formatted message string
     */
    public String get(String key) {
        return get(key, null);
    }
    
    /**
     * Send a message to a command sender with color codes translated.
     *
     * @param sender the command sender
     * @param key the message key
     * @param placeholders map of placeholder names to values
     */
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = get(key, placeholders);
        Component component = LEGACY_SERIALIZER.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Send a message to a command sender without placeholders.
     *
     * @param sender the command sender
     * @param key the message key
     */
    public void send(CommandSender sender, String key) {
        send(sender, key, null);
    }
    
    /**
     * Send a message to a player with color codes translated.
     *
     * @param player the player
     * @param key the message key
     * @param placeholders map of placeholder names to values
     */
    public void send(Player player, String key, Map<String, String> placeholders) {
        send((CommandSender) player, key, placeholders);
    }
    
    /**
     * Send a message to a player without placeholders.
     *
     * @param player the player
     * @param key the message key
     */
    public void send(Player player, String key) {
        send(player, key, null);
    }
    
    /**
     * Create a placeholder map builder for convenient placeholder creation.
     *
     * @return a new PlaceholderBuilder
     */
    public static PlaceholderBuilder placeholders() {
        return new PlaceholderBuilder();
    }
    
    /**
     * Builder class for creating placeholder maps.
     */
    public static class PlaceholderBuilder {
        private final Map<String, String> placeholders = new HashMap<>();
        
        /**
         * Add a placeholder.
         *
         * @param key the placeholder key (without braces)
         * @param value the placeholder value
         * @return this builder
         */
        public PlaceholderBuilder add(String key, String value) {
            placeholders.put(key, value);
            return this;
        }
        
        /**
         * Add a placeholder with an integer value.
         *
         * @param key the placeholder key (without braces)
         * @param value the placeholder value
         * @return this builder
         */
        public PlaceholderBuilder add(String key, int value) {
            return add(key, String.valueOf(value));
        }
        
        /**
         * Add a placeholder with a double value.
         *
         * @param key the placeholder key (without braces)
         * @param value the placeholder value
         * @return this builder
         */
        public PlaceholderBuilder add(String key, double value) {
            return add(key, String.valueOf(value));
        }
        
        /**
         * Add a placeholder with a long value.
         *
         * @param key the placeholder key (without braces)
         * @param value the placeholder value
         * @return this builder
         */
        public PlaceholderBuilder add(String key, long value) {
            return add(key, String.valueOf(value));
        }
        
        /**
         * Build the placeholder map.
         *
         * @return the placeholder map
         */
        public Map<String, String> build() {
            return placeholders;
        }
    }
}
