package com.voidvault.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages search/filter state for players viewing their vaults.
 */
public class SearchManager {
    
    private final Map<UUID, SearchState> activeSearches = new HashMap<>();
    
    /**
     * Start a search session for a player.
     *
     * @param player The player starting the search
     * @param page   The page they're searching on
     */
    public void startSearch(Player player, int page) {
        activeSearches.put(player.getUniqueId(), new SearchState(page));
    }
    
    /**
     * Set the search query for a player.
     *
     * @param player The player
     * @param query  The search query
     */
    public void setSearchQuery(Player player, String query) {
        SearchState state = activeSearches.get(player.getUniqueId());
        if (state != null) {
            state.query = query.toLowerCase();
        }
    }
    
    /**
     * Get the active search query for a player.
     *
     * @param player The player
     * @return The search query, or null if no active search
     */
    public String getSearchQuery(Player player) {
        SearchState state = activeSearches.get(player.getUniqueId());
        return state != null ? state.query : null;
    }
    
    /**
     * Check if a player has an active search.
     *
     * @param player The player
     * @return true if the player has an active search
     */
    public boolean hasActiveSearch(Player player) {
        return activeSearches.containsKey(player.getUniqueId());
    }
    
    /**
     * Check if a player is waiting to enter a search query.
     *
     * @param player The player
     * @return true if waiting for input
     */
    public boolean isWaitingForInput(Player player) {
        SearchState state = activeSearches.get(player.getUniqueId());
        return state != null && state.query == null;
    }
    
    /**
     * Clear the search for a player.
     *
     * @param player The player
     */
    public void clearSearch(Player player) {
        activeSearches.remove(player.getUniqueId());
    }
    
    /**
     * Get the page number for an active search.
     *
     * @param player The player
     * @return The page number, or -1 if no active search
     */
    public int getSearchPage(Player player) {
        SearchState state = activeSearches.get(player.getUniqueId());
        return state != null ? state.page : -1;
    }
    
    /**
     * Internal class to track search state.
     */
    private static class SearchState {
        int page;
        String query;
        
        SearchState(int page) {
            this.page = page;
            this.query = null;
        }
    }
}
