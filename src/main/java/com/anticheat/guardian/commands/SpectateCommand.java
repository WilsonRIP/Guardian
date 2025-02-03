package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectateCommand extends GuardianCommand {
    
    private static final Map<UUID, GameMode> previousGameModes = new HashMap<>();
    
    public SpectateCommand(Guardian plugin) {
        super(plugin, "guardian.spectate", true);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "commands.spectate.usage");
            return true;
        }
        
        Player staff = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            sendMessage(sender, "commands.player-not-found");
            return true;
        }
        
        if (target.equals(staff)) {
            sendMessage(sender, "commands.spectate.self");
            return true;
        }
        
        // Store previous gamemode
        previousGameModes.put(staff.getUniqueId(), staff.getGameMode());
        
        // Set up spectator mode
        staff.setGameMode(GameMode.SPECTATOR);
        staff.teleport(target.getLocation().add(0, 2, 0));
        
        sendMessage(sender, "commands.spectate.start", "{target}", target.getName());
        
        // Notify other staff members
        plugin.broadcastAlert(MessageUtils.getMessage("alerts.staff-spectate", 
            "{staff}", staff.getName(),
            "{target}", target.getName()
        ));
        
        return true;
    }
    
    public static GameMode getPreviousGameMode(UUID uuid) {
        return previousGameModes.getOrDefault(uuid, GameMode.SURVIVAL);
    }
    
    public static void clearPreviousGameMode(UUID uuid) {
        previousGameModes.remove(uuid);
    }
} 