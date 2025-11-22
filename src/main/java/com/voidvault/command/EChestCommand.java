package com.voidvault.command;

import com.voidvault.config.MessageManager;
import com.voidvault.manager.CooldownManager;
import com.voidvault.manager.EconomyManager;
import com.voidvault.manager.PermissionManager;
import com.voidvault.manager.VaultManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Command executor for remote vault access.
 * Handles /echest, /pv, and /vault commands.
 * Implements permission checks, cooldown enforcement, and economy integration.
 */
public class EChestCommand implements CommandExecutor {
    private final Plugin plugin;
    private final Logger logger;
    private final VaultManager vaultManager;
    private final PermissionManager permissionManager;
    private final CooldownManager cooldownManager;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    
    public EChestCommand(Plugin plugin, VaultManager vaultManager, PermissionManager permissionManager,
                         CooldownManager cooldownManager, EconomyManager economyManager,
                         MessageManager messageManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.vaultManager = vaultManager;
        this.permissionManager = permissionManager;
        this.cooldownManager = cooldownManager;
        this.economyManager = economyManager;
        this.messageManager = messageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Must be a player
        if (!(sender instanceof Player player)) {
            logger.fine("Non-player sender attempted to use remote access command: " + sender.getName());
            messageManager.send(sender, "commands.player-only");
            return true;
        }
        
        logger.fine("Player " + player.getName() + " attempting remote vault access via /" + label);
        
        // Check remote access permission
        if (!permissionManager.hasRemoteAccess(player)) {
            logger.fine("Player " + player.getName() + " denied remote access: missing voidvaults.remote permission");
            messageManager.send(player, "commands.no-permission");
            return true;
        }
        
        // Check cooldown (unless player has bypass permission)
        if (!permissionManager.canBypassCooldown(player)) {
            if (cooldownManager.isOnCooldown(player)) {
                long remainingSeconds = cooldownManager.getRemainingSeconds(player);
                logger.fine("Player " + player.getName() + " on cooldown: " + remainingSeconds + " seconds remaining");
                messageManager.send(player, "remote-access.on-cooldown",
                    MessageManager.placeholders()
                        .add("seconds", remainingSeconds)
                        .build());
                return true;
            }
        } else {
            logger.fine("Player " + player.getName() + " bypassing cooldown check");
        }
        
        // Check and charge economy fee if enabled
        double economyFee = plugin.getConfig().getDouble("remote-access.economy-fee", 0.0);
        if (economyManager.isEnabled() && economyFee > 0) {
            logger.fine("Economy integration enabled, checking balance for " + player.getName() + " (fee: " + economyFee + ")");
            
            if (!economyManager.hasBalance(player, economyFee)) {
                String formattedAmount = economyManager.formatAmount(economyFee);
                logger.fine("Player " + player.getName() + " has insufficient funds for remote access");
                messageManager.send(player, "remote-access.insufficient-funds",
                    MessageManager.placeholders()
                        .add("amount", formattedAmount)
                        .build());
                return true;
            }
            
            // Withdraw the fee
            if (!economyManager.withdraw(player, economyFee)) {
                logger.warning("Failed to withdraw economy fee from " + player.getName() + " (amount: " + economyFee + ")");
                messageManager.send(player, "error.economy-transaction-failed");
                return true;
            }
            
            // Notify player of charge
            String formattedAmount = economyManager.formatAmount(economyFee);
            logger.fine("Charged " + formattedAmount + " to " + player.getName() + " for remote access");
            messageManager.send(player, "remote-access.fee-charged",
                MessageManager.placeholders()
                    .add("amount", formattedAmount)
                    .build());
        } else {
            logger.fine("Economy integration disabled or fee is 0, skipping fee check");
        }
        
        // Set cooldown (unless player has bypass permission)
        if (!permissionManager.canBypassCooldown(player)) {
            cooldownManager.setCooldown(player);
            logger.fine("Cooldown set for " + player.getName());
        }
        
        // Play chest opening sound at player location
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        
        // Store fee for potential refund
        final double chargedFee = economyFee;
        final boolean feeWasCharged = economyManager.isEnabled() && economyFee > 0;
        
        // Open vault asynchronously
        logger.info("Opening remote vault for " + player.getName());
        messageManager.send(player, "vault.opening");
        
        vaultManager.openVault(player, 1).exceptionally(ex -> {
            // Detailed error logging for different failure scenarios
            String errorType = ex.getClass().getSimpleName();
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            
            logger.severe("=== Remote Vault Access Failure ===");
            logger.severe("Player: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
            logger.severe("Command: /" + label);
            logger.severe("Error Type: " + errorType);
            logger.severe("Error Message: " + errorMessage);
            logger.severe("Stack trace:");
            ex.printStackTrace();
            logger.severe("===================================");
            
            // Send user-friendly error message
            messageManager.send(player, "error.load-failed");
            
            // Attempt to refund the economy fee if vault opening failed
            if (feeWasCharged) {
                boolean refunded = economyManager.deposit(player, chargedFee);
                if (refunded) {
                    String formattedAmount = economyManager.formatAmount(chargedFee);
                    logger.info("Refunded " + formattedAmount + " to " + player.getName() + " due to vault opening failure");
                    messageManager.send(player, "remote-access.fee-refunded",
                        MessageManager.placeholders()
                            .add("amount", formattedAmount)
                            .build());
                } else {
                    logger.warning("Failed to refund fee to " + player.getName() + 
                        " - manual intervention may be required");
                }
            }
            
            // Clear cooldown since the operation failed
            if (!permissionManager.canBypassCooldown(player)) {
                cooldownManager.clearCooldown(player);
                logger.info("Cleared cooldown for " + player.getName() + " due to vault opening failure");
            }
            
            return null;
        }).thenRun(() -> {
            // Success logging
            logger.info("Successfully opened remote vault for " + player.getName());
        });
        
        return true;
    }
}
