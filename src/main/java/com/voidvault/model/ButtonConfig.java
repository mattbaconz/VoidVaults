package com.voidvault.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable record representing GUI button configuration.
 * Uses Java 21 record syntax for concise configuration data.
 *
 * @param material    The material type for the button
 * @param displayName The display name of the button (supports color codes)
 * @param lore        The lore lines for the button (supports color codes)
 * @param glow        Whether the button should have a glow effect
 */
public record ButtonConfig(
        Material material,
        String displayName,
        List<String> lore,
        boolean glow
) {
    /**
     * Compact constructor with validation and defensive copying.
     */
    public ButtonConfig {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        if (displayName == null) {
            displayName = "";
        }
        if (lore == null) {
            lore = List.of();
        } else {
            // Create an immutable copy of the lore list
            lore = List.copyOf(lore);
        }
    }

    /**
     * Creates a simple ButtonConfig with just material and display name.
     *
     * @param material    The material type
     * @param displayName The display name
     * @return A new ButtonConfig instance
     */
    public static ButtonConfig simple(Material material, String displayName) {
        return new ButtonConfig(material, displayName, List.of(), false);
    }

    /**
     * Creates a ButtonConfig with material, display name, and glow.
     *
     * @param material    The material type
     * @param displayName The display name
     * @param glow        Whether to add glow effect
     * @return A new ButtonConfig instance
     */
    public static ButtonConfig withGlow(Material material, String displayName, boolean glow) {
        return new ButtonConfig(material, displayName, List.of(), glow);
    }

    /**
     * Converts this ButtonConfig to an ItemStack.
     * Applies color codes, lore, and glow effect.
     *
     * @return A new ItemStack representing this button
     */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Apply display name with color code translation
            if (!displayName.isEmpty()) {
                meta.setDisplayName(translateColorCodes(displayName));
            }

            // Apply lore with color code translation
            if (!lore.isEmpty()) {
                List<String> translatedLore = new ArrayList<>();
                for (String line : lore) {
                    translatedLore.add(translateColorCodes(line));
                }
                meta.setLore(translatedLore);
            }

            // Apply glow effect if enabled
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a new ButtonConfig with updated display name.
     *
     * @param newDisplayName The new display name
     * @return A new ButtonConfig instance with updated display name
     */
    public ButtonConfig withDisplayName(String newDisplayName) {
        return new ButtonConfig(material, newDisplayName, lore, glow);
    }

    /**
     * Creates a new ButtonConfig with updated lore.
     *
     * @param newLore The new lore lines
     * @return A new ButtonConfig instance with updated lore
     */
    public ButtonConfig withLore(List<String> newLore) {
        return new ButtonConfig(material, displayName, newLore, glow);
    }

    /**
     * Creates a new ButtonConfig with updated glow setting.
     *
     * @param newGlow The new glow setting
     * @return A new ButtonConfig instance with updated glow
     */
    public ButtonConfig withGlow(boolean newGlow) {
        return new ButtonConfig(material, displayName, lore, newGlow);
    }

    /**
     * Creates a new ButtonConfig with an additional lore line.
     *
     * @param loreLine The lore line to add
     * @return A new ButtonConfig instance with the added lore line
     */
    public ButtonConfig addLoreLine(String loreLine) {
        List<String> newLore = new ArrayList<>(lore);
        newLore.add(loreLine);
        return new ButtonConfig(material, displayName, newLore, glow);
    }

    /**
     * Translates color codes in a string.
     * Converts & codes to § codes for Minecraft color formatting.
     *
     * @param text The text to translate
     * @return The text with translated color codes
     */
    private static String translateColorCodes(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('&', '§');
    }
}
