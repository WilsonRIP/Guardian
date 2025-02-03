package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public abstract class GuardianCommand implements CommandExecutor {
    
    protected final Guardian plugin;
    protected final String permission;
    protected final boolean playerOnly;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (playerOnly && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
            return true;
        }
        
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        return execute(sender, args);
    }
    
    protected abstract boolean execute(CommandSender sender, String[] args);
    
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "Guardian" + ChatColor.GRAY + "] " + ChatColor.WHITE + message);
    }
} 