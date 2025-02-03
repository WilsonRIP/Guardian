package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCommand extends GuardianCommand {
    
    public DebugCommand(Guardian plugin) {
        super(plugin, "guardian.debug", false);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "commands.debug.usage");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "commands.player-not-found");
            return true;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sendMessage(sender, "commands.debug.no-data", "{player}", target.getName());
            return true;
        }
        
        // Send detailed information
        sendMessage(sender, "commands.debug.header", "{player}", target.getName());
        sendMessage(sender, "commands.debug.location", "{location}", formatLocation(data.getLastLocation()));
        sendMessage(sender, "commands.debug.last-ground", "{location}", formatLocation(data.getLastGroundLocation()));
        sendMessage(sender, "commands.debug.on-ground", "{value}", String.valueOf(data.isOnGround()));
        sendMessage(sender, "commands.debug.last-ground-time", "{time}", String.valueOf(System.currentTimeMillis() - data.getLastGroundTime()));
        sendMessage(sender, "commands.debug.last-delta-y", "{value}", String.format("%.3f", data.getLastDeltaY()));
        
        // Combat stats
        sendMessage(sender, "commands.debug.combat-stats.header");
        sendMessage(sender, "commands.debug.combat-stats.last-attack", "{time}", String.valueOf(System.currentTimeMillis() - data.getLastAttackTime()));
        sendMessage(sender, "commands.debug.combat-stats.hit-count", "{count}", String.valueOf(data.getHitCount()));
        sendMessage(sender, "commands.debug.combat-stats.consecutive-hits", "{count}", String.valueOf(data.getConsecutiveHits()));
        sendMessage(sender, "commands.debug.combat-stats.last-reach", "{distance}", String.format("%.2f", data.getLastReachDistance()));
        
        return true;
    }
    
    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }
} 