package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ViolationsCommand extends GuardianCommand {
    
    public ViolationsCommand(Guardian plugin) {
        super(plugin, "guardian.violations", false);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, ChatColor.RED + "Usage: /guardian violations <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, ChatColor.RED + "Player not found!");
            return true;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sendMessage(sender, ChatColor.RED + "No violation data found for " + target.getName());
            return true;
        }
        
        sendMessage(sender, ChatColor.YELLOW + "Violations for " + target.getName() + ":");
        sendMessage(sender, ChatColor.GRAY + "Movement: " + ChatColor.RED + data.getMovementVL());
        sendMessage(sender, ChatColor.GRAY + "Combat: " + ChatColor.RED + data.getCombatVL());
        sendMessage(sender, ChatColor.GRAY + "Recent Hits: " + ChatColor.RED + data.getHitCount());
        sendMessage(sender, ChatColor.GRAY + "Last Reach Distance: " + ChatColor.RED + String.format("%.2f", data.getLastReachDistance()));
        
        return true;
    }
} 