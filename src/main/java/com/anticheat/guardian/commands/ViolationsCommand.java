package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ViolationsCommand extends GuardianCommand {
    
    public ViolationsCommand(Guardian plugin) {
        super(plugin, "guardian.violations", false);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "commands.violations.usage");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "commands.player-not-found");
            return true;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sendMessage(sender, "commands.violations.no-data", "{player}", target.getName());
            return true;
        }
        
        sendMessage(sender, "commands.violations.header", "{player}", target.getName());
        sendMessage(sender, "commands.violations.movement", "{vl}", String.valueOf(data.getMovementVL()));
        sendMessage(sender, "commands.violations.combat", "{vl}", String.valueOf(data.getCombatVL()));
        sendMessage(sender, "commands.violations.recent-hits", "{count}", String.valueOf(data.getHitCount()));
        sendMessage(sender, "commands.violations.reach-distance", "{distance}", String.format("%.2f", data.getLastReachDistance()));
        
        return true;
    }
} 