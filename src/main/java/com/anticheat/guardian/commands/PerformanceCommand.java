package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class PerformanceCommand extends GuardianCommand {
    
    private static long lastCheck = System.currentTimeMillis();
    private static int totalChecks = 0;
    
    public PerformanceCommand(Guardian plugin) {
        super(plugin, "guardian.performance", false);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        long uptime = System.currentTimeMillis() - lastCheck;
        double checksPerSec = totalChecks / (uptime / 1000.0);
        
        sendMessage(sender, ChatColor.YELLOW + "Guardian Performance Metrics:");
        sendMessage(sender, ChatColor.GRAY + "Memory Usage: " + ChatColor.WHITE + 
                   usedMemory + "MB/" + totalMemory + "MB");
        sendMessage(sender, ChatColor.GRAY + "Checks/Second: " + ChatColor.WHITE + 
                   String.format("%.2f", checksPerSec));
        sendMessage(sender, ChatColor.GRAY + "Total Checks: " + ChatColor.WHITE + totalChecks);
        sendMessage(sender, ChatColor.GRAY + "Active Players: " + ChatColor.WHITE + 
                   plugin.getPlayerDataManager().getActivePlayerCount());
        
        return true;
    }
    
    public static void incrementChecks() {
        totalChecks++;
    }
    
    public static void resetChecks() {
        lastCheck = System.currentTimeMillis();
    }
} 