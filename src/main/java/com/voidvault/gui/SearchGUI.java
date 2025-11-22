package com.voidvault.gui;

import com.voidvault.manager.VaultManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Search GUI for filtering vault items.
 * Provides a user-friendly interface with predefined search options and chat input.
 */
public class SearchGUI {
    
    private final Player player;
    private final int page;
    private final VaultManager vaultManager;
    private final Inventory inventory;
    
    public SearchGUI(Player player, int page, VaultManager vaultManager) {
        this.player = player;
        this.page = page;
        this.vaultManager = vaultManager;
        this.inventory = Bukkit.createInventory(null, 27, "§5§lSearch Vault");
    }
    
    /**
     * Open the search GUI for the player.
     */
    public void open() {
        setupInventory();
        player.openInventory(inventory);
    }
    
    /**
     * Set up the search GUI inventory with buttons.
     */
    private void setupInventory() {
        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        
        // Quick search options
        inventory.setItem(10, createItem(Material.DIAMOND, "§b§lDiamond Items",
            Arrays.asList("§7Search for diamond items")));
        
        inventory.setItem(11, createItem(Material.IRON_INGOT, "§7§lIron Items",
            Arrays.asList("§7Search for iron items")));
        
        inventory.setItem(12, createItem(Material.GOLD_INGOT, "§e§lGold Items",
            Arrays.asList("§7Search for gold items")));
        
        inventory.setItem(13, createItem(Material.NETHERITE_INGOT, "§8§lNetherite Items",
            Arrays.asList("§7Search for netherite items")));
        
        inventory.setItem(14, createItem(Material.WOODEN_SWORD, "§6§lTools & Weapons",
            Arrays.asList("§7Search for tools and weapons")));
        
        inventory.setItem(15, createItem(Material.DIAMOND_CHESTPLATE, "§9§lArmor",
            Arrays.asList("§7Search for armor pieces")));
        
        inventory.setItem(16, createItem(Material.COBBLESTONE, "§7§lBlocks",
            Arrays.asList("§7Search for blocks")));
        
        // Custom search button
        inventory.setItem(22, createItem(Material.NAME_TAG, "§e§lCustom Search",
            Arrays.asList("§7Click to type your own search", "§7Type in chat after clicking")));
        
        // Clear filter button
        inventory.setItem(24, createItem(Material.BARRIER, "§c§lClear Filter",
            Arrays.asList("§7Click to show all items")));
        
        // Cancel button
        inventory.setItem(26, createItem(Material.RED_STAINED_GLASS_PANE, "§c§lCancel",
            Arrays.asList("§7Return to vault")));
    }
    
    /**
     * Handle click events in the search GUI.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 10 -> processSearch("diamond");
            case 11 -> processSearch("iron");
            case 12 -> processSearch("gold");
            case 13 -> processSearch("netherite");
            case 14 -> processSearch("sword pickaxe axe shovel hoe");
            case 15 -> processSearch("helmet chestplate leggings boots");
            case 16 -> processSearch("stone dirt cobblestone");
            case 22 -> promptCustomSearch();
            case 24 -> clearFilter();
            case 26 -> cancel();
        }
    }
    
    /**
     * Handle inventory close event.
     */
    public void handleClose(InventoryCloseEvent event) {
        // Clean up if needed
    }
    
    /**
     * Prompt player to type custom search in chat.
     */
    private void promptCustomSearch() {
        player.closeInventory();
        vaultManager.getSearchManager().startSearch(player, page);
        player.sendMessage("§5§l[VoidVault] §bType your search query in chat (or 'cancel' to exit):");
    }
    
    /**
     * Process the search query.
     */
    private void processSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            cancel();
            return;
        }
        
        // Set search query in manager
        vaultManager.getSearchManager().startSearch(player, page);
        vaultManager.getSearchManager().setSearchQuery(player, query.trim());
        
        // Close GUI and reopen vault with filter
        player.closeInventory();
        player.sendMessage("§5§l[VoidVault] §bSearching for: §e" + query.trim());
        vaultManager.openVault(player, page);
    }
    
    /**
     * Clear the search filter.
     */
    private void clearFilter() {
        vaultManager.getSearchManager().clearSearch(player);
        player.closeInventory();
        player.sendMessage("§5§l[VoidVault] §aSearch filter cleared!");
        vaultManager.openVault(player, page);
    }
    
    /**
     * Cancel and return to vault.
     */
    private void cancel() {
        player.closeInventory();
        vaultManager.openVault(player, page);
    }
    
    /**
     * Create an ItemStack with display name and lore.
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Get the inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }
}
