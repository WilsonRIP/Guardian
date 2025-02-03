package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCommand extends GuardianCommand {
    
    public DebugCommand(Guardian plugin) {
        super(plugin, "guardian.debug", false);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, ChatColor.RED + "Usage: /guardian debug <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, ChatColor.RED + "Player not found!");
            return true;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sendMessage(sender, ChatColor.RED + "No data found for " + target.getName());
            return true;
        }
        
        // Send detailed information
        sendMessage(sender, ChatColor.YELLOW + "Debug Information for " + target.getName() + ":");
        sendMessage(sender, ChatColor.GRAY + "Location: " + ChatColor.WHITE + formatLocation(data.getLastLocation()));
        sendMessage(sender, ChatColor.GRAY + "Last Ground: " + ChatColor.WHITE + formatLocation(data.getLastGroundLocation()));
        sendMessage(sender, ChatColor.GRAY + "On Ground: " + ChatColor.WHITE + data.isOnGround());
        sendMessage(sender, ChatColor.GRAY + "Last Ground Time: " + ChatColor.WHITE + (System.currentTimeMillis() - data.getLastGroundTime()) + "ms ago");
        sendMessage(sender, ChatColor.GRAY + "Last Delta Y: " + ChatColor.WHITE + String.format("%.3f", data.getLastDeltaY()));
        sendMessage(sender, ChatColor.GRAY + "Combat Stats:");
        sendMessage(sender, ChatColor.GRAY + " - Last Attack: " + ChatColor.WHITE + (System.currentTimeMillis() - data.getLastAttackTime()) + "ms ago");
        sendMessage(sender, ChatColor.GRAY + " - Hit Count: " + ChatColor.WHITE + data.getHitCount());
        sendMessage(sender, ChatColor.GRAY + " - Consecutive Hits: " + ChatColor.WHITE + data.getConsecutiveHits());
        sendMessage(sender, ChatColor.GRAY + " - Last Reach: " + ChatColor.WHITE + String.format("%.2f", data.getLastReachDistance()));
        
        return true;
    }
    
    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "null";
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }
} 