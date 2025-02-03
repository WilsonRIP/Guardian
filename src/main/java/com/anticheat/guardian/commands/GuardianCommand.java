package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
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
            MessageUtils.sendMessage(sender, "commands.player-only");
            return true;
        }
        
        if (permission != null && !sender.hasPermission(permission)) {
            MessageUtils.sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        return execute(sender, args);
    }
    
    protected abstract boolean execute(CommandSender sender, String[] args);
    
    protected void sendMessage(CommandSender sender, String message) {
        MessageUtils.sendMessage(sender, message);
    }
    
    protected void sendMessage(CommandSender sender, String message, String... replacements) {
        MessageUtils.sendMessage(sender, message, replacements);
    }
} 