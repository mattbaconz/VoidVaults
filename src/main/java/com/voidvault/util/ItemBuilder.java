package com.voidvault.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent API builder for creating ItemStacks with custom properties.
 * Supports display names, lore, amounts, and glow effects with color code translation.
 */
public class ItemBuilder {
    
    private final ItemStack item;
    private final ItemMeta meta;
    
    /**
     * Create a new ItemBuilder for the specified material.
     *
     * @param material The material for the item
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    /**
     * Create a new ItemBuilder from an existing ItemStack.
     *
     * @param item The base ItemStack
     */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    /**
     * Set the display name of the item.
     * Supports legacy color codes (&amp;) and automatically removes italic formatting.
     *
     * @param name The display name with color codes
     * @return This builder for chaining
     */
    public ItemBuilder displayName(String name) {
        if (meta != null && name != null) {
            // Translate color codes and convert to Component
            Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(name)
                .decoration(TextDecoration.ITALIC, false);
            meta.displayName(component);
        }
        return this;
    }
    
    /**
     * Set the lore of the item.
     * Supports legacy color codes (&amp;) and automatically removes italic formatting.
     *
     * @param lore The lore lines with color codes
     * @return This builder for chaining
     */
    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) {
                Component component = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(line)
                    .decoration(TextDecoration.ITALIC, false);
                components.add(component);
            }
            meta.lore(components);
        }
        return this;
    }
    
    /**
     * Add a single line to the lore.
     * Supports legacy color codes (&amp;) and automatically removes italic formatting.
     *
     * @param line The lore line with color codes
     * @return This builder for chaining
     */
    public ItemBuilder addLoreLine(String line) {
        if (meta != null && line != null) {
            List<Component> currentLore = meta.lore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            }
            
            Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(line)
                .decoration(TextDecoration.ITALIC, false);
            currentLore.add(component);
            meta.lore(currentLore);
        }
        return this;
    }
    
    /**
     * Set the amount of items in the stack.
     *
     * @param amount The amount (1-64)
     * @return This builder for chaining
     */
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }
    
    /**
     * Add a glow effect to the item.
     * This is achieved by adding a hidden enchantment.
     *
     * @param glow Whether the item should glow
     * @return This builder for chaining
     */
    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            // Add a fake enchantment and hide it
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    
    /**
     * Make the item unbreakable.
     *
     * @param unbreakable Whether the item should be unbreakable
     * @return This builder for chaining
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            if (unbreakable) {
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
        }
        return this;
    }
    
    /**
     * Add item flags to hide certain attributes.
     *
     * @param flags The flags to add
     * @return This builder for chaining
     */
    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }
    
    /**
     * Set the custom model data for the item.
     *
     * @param data The custom model data value
     * @return This builder for chaining
     */
    public ItemBuilder customModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }
    
    /**
     * Build and return the final ItemStack.
     *
     * @return The constructed ItemStack
     */
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
