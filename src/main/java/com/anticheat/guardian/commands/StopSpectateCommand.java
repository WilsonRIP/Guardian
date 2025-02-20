package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StopSpectateCommand extends GuardianCommand {
    
    public StopSpectateCommand(Guardian plugin) {
        super(plugin, "guardian.spectate", true);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        if (player.getGameMode() != GameMode.SPECTATOR) {
            sendMessage(sender, "commands.spectate.not-spectating");
            return true;
        }
        
        // Restore previous gamemode
        GameMode previousMode = SpectateCommand.getPreviousGameMode(player.getUniqueId());
        player.setGameMode(previousMode);
        SpectateCommand.clearPreviousGameMode(player.getUniqueId());
        
        sendMessage(sender, "commands.spectate.stop", "{gamemode}", previousMode.toString().toLowerCase());
        
        return true;
    }
} 