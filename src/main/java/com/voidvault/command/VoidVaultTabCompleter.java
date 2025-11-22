package com.voidvault.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion handler for VoidVaults commands.
 * Provides context-aware suggestions for subcommands, player names, and numeric values.
 */
public class VoidVaultTabCompleter implements TabCompleter {
    
    // Subcommands for /voidvaults
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "open", "reload", "setslots", "setpages", "stats", "statistics", "cleancache"
    );
    
    // Slot size suggestions
    private static final List<String> SLOT_SUGGESTIONS = Arrays.asList(
        "9", "18", "27", "36", "45", "52", "54", "0"
    );
    
    // Page number suggestions
    private static final List<String> PAGE_SUGGESTIONS = Arrays.asList(
        "1", "2", "3", "4", "5", "0"
    );
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Handle /voidvaults command (and aliases)
        if (command.getName().equalsIgnoreCase("voidvaults") || 
            command.getName().equalsIgnoreCase("vvs") ||
            command.getName().equalsIgnoreCase("vv")) {
            return handleVoidVaultCompletion(sender, args);
        }
        
        // Handle /echest, /pv, /vault commands (no arguments needed)
        if (command.getName().equalsIgnoreCase("echest") ||
            command.getName().equalsIgnoreCase("pv") ||
            command.getName().equalsIgnoreCase("vault")) {
            return completions; // No tab completion for these commands
        }
        
        return completions;
    }
    
    /**
     * Handle tab completion for /voidvaults command.
     */
    private List<String> handleVoidVaultCompletion(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // First argument: subcommand
        if (args.length == 1) {
            return filterMatches(SUBCOMMANDS, args[0]);
        }
        
        // Get the subcommand
        String subcommand = args[0].toLowerCase();
        
        return switch (subcommand) {
            case "open" -> handleOpenCompletion(sender, args);
            case "setslots" -> handleSetSlotsCompletion(sender, args);
            case "setpages" -> handleSetPagesCompletion(sender, args);
            case "reload", "stats", "statistics", "cleancache" -> completions; // No arguments
            default -> completions;
        };
    }
    
    /**
     * Handle tab completion for /voidvaults open <player> [page]
     */
    private List<String> handleOpenCompletion(CommandSender sender, String[] args) {
        // Second argument: player name
        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        
        // Third argument: page number
        if (args.length == 3) {
            return filterMatches(PAGE_SUGGESTIONS, args[2]);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Handle tab completion for /voidvaults setslots <player> <amount>
     */
    private List<String> handleSetSlotsCompletion(CommandSender sender, String[] args) {
        // Second argument: player name
        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        
        // Third argument: slot amount
        if (args.length == 3) {
            return filterMatches(SLOT_SUGGESTIONS, args[2]);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Handle tab completion for /voidvaults setpages <player> <amount>
     */
    private List<String> handleSetPagesCompletion(CommandSender sender, String[] args) {
        // Second argument: player name
        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        
        // Third argument: page amount
        if (args.length == 3) {
            return filterMatches(PAGE_SUGGESTIONS, args[2]);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get list of online player names that match the partial input.
     */
    private List<String> getOnlinePlayerNames(String partial) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Filter a list of strings to only include those that start with the partial input.
     */
    private List<String> filterMatches(List<String> options, String partial) {
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
}
