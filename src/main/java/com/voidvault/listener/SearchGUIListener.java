package com.voidvault.listener;

import com.voidvault.gui.SearchGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Listens for interactions with the SearchGUI.
 */
public class SearchGUIListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (event.getView().getTitle().equals("§5§lSearch Vault")) {
            event.setCancelled(true);
            
            // The SearchGUI handles its own clicks, but we need to route them
            // This is handled by checking the inventory holder or title
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        if (event.getView().getTitle().equals("§5§lSearch Vault")) {
            // Search GUI closed
        }
    }
}
