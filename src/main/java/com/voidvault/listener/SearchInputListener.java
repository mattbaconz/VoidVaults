package com.voidvault.listener;

import com.voidvault.config.MessageManager;
import com.voidvault.manager.SearchManager;
import com.voidvault.manager.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import static com.voidvault.config.MessageManager.placeholders;

/**
 * Listens for chat input when a player is entering a search query.
 */
public class SearchInputListener implements Listener {
    
    private final SearchManager searchManager;
    private final VaultManager vaultManager;
    private final MessageManager messageManager;
    
    public SearchInputListener(SearchManager searchManager, VaultManager vaultManager, 
                               MessageManager messageManager) {
        this.searchManager = searchManager;
        this.vaultManager = vaultManager;
        this.messageManager = messageManager;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is waiting for search input
        if (!searchManager.isWaitingForInput(player)) {
            return;
        }
        
        // Cancel the chat event
        event.setCancelled(true);
        
        String input = event.getMessage().trim();
        
        // Check for cancel command
        if (input.equalsIgnoreCase("cancel")) {
            searchManager.clearSearch(player);
            messageManager.send(player, "vault.search-cancelled");
            return;
        }
        
        // Set the search query
        searchManager.setSearchQuery(player, input);
        
        // Get the page they were on
        int page = searchManager.getSearchPage(player);
        
        // Reopen the vault with the search filter
        vaultManager.openVault(player, page);
        
        // Send confirmation message
        messageManager.send(player, "vault.search-active", 
            placeholders().add("query", input).build());
    }
}
