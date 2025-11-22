package com.voidvault.model;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * Immutable record representing a single page of vault storage.
 * Uses Java 21 record syntax for concise data representation.
 *
 * @param pageNumber The page number (1-indexed)
 * @param contents   Array of ItemStacks representing the page contents (52 slots for PAGED mode)
 */
public record VaultPage(
        int pageNumber,
        ItemStack[] contents
) {
    /**
     * Compact constructor with validation and defensive copying.
     */
    public VaultPage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        }
        if (contents == null) {
            contents = new ItemStack[52];
        } else {
            // Create a defensive copy to prevent external modification
            contents = Arrays.copyOf(contents, contents.length);
        }
    }

    /**
     * Creates a new empty VaultPage with the specified page number.
     *
     * @param pageNumber The page number
     * @return A new VaultPage with empty contents
     */
    public static VaultPage createEmpty(int pageNumber) {
        return new VaultPage(pageNumber, new ItemStack[52]);
    }

    /**
     * Creates a new VaultPage with a specific size.
     *
     * @param pageNumber The page number
     * @param size       The size of the contents array
     * @return A new VaultPage with the specified size
     */
    public static VaultPage createWithSize(int pageNumber, int size) {
        return new VaultPage(pageNumber, new ItemStack[size]);
    }

    /**
     * Retrieves an item from a specific slot.
     *
     * @param slot The slot index (0-indexed)
     * @return The ItemStack at the slot, or null if empty or out of bounds
     */
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= contents.length) {
            return null;
        }
        return contents[slot];
    }

    /**
     * Creates a new VaultPage with an item set at a specific slot.
     * This maintains immutability by returning a new instance.
     *
     * @param slot The slot index (0-indexed)
     * @param item The ItemStack to set (null to clear)
     * @return A new VaultPage with the updated item
     */
    public VaultPage withItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= contents.length) {
            return this; // Return unchanged if slot is invalid
        }
        ItemStack[] newContents = Arrays.copyOf(contents, contents.length);
        newContents[slot] = item;
        return new VaultPage(pageNumber, newContents);
    }

    /**
     * Gets the size of the contents array.
     *
     * @return The number of slots in this page
     */
    public int getSize() {
        return contents.length;
    }

    /**
     * Checks if the page is completely empty.
     *
     * @return true if all slots are null, false otherwise
     */
    public boolean isEmpty() {
        for (ItemStack item : contents) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts the number of non-null items in the page.
     *
     * @return The count of items in the page
     */
    public int getItemCount() {
        int count = 0;
        for (ItemStack item : contents) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Creates a mutable copy of the contents array.
     * Use this when you need to modify the contents directly.
     *
     * @return A new array containing copies of the contents
     */
    public ItemStack[] copyContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /**
     * Clears all items from the page.
     *
     * @return A new VaultPage with all slots cleared
     */
    public VaultPage clear() {
        return new VaultPage(pageNumber, new ItemStack[contents.length]);
    }
    
    /**
     * Sorts and organizes items in the page.
     * Groups similar items together and sorts by material name.
     * Modifies the contents array in place.
     */
    public void sort() {
        // Collect all non-null items
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        
        // Sort items by material name
        items.sort((a, b) -> a.getType().name().compareTo(b.getType().name()));
        
        // Clear contents
        Arrays.fill(contents, null);
        
        // Place sorted items back
        int index = 0;
        for (ItemStack item : items) {
            if (index >= contents.length) break;
            contents[index++] = item;
        }
    }
    
    /**
     * Sets an item at a specific slot.
     * Modifies the contents array in place.
     *
     * @param slot The slot index (0-indexed)
     * @param item The ItemStack to set (null to clear)
     */
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < contents.length) {
            contents[slot] = item;
        }
    }
}
