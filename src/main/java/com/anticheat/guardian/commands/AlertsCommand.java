package com.anticheat.guardian.commands;

import com.anticheat.guardian.Guardian;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AlertsCommand extends GuardianCommand {
    
    private static final Set<UUID> alertsEnabled = new HashSet<>();
    
    public AlertsCommand(Guardian plugin) {
        super(plugin, "guardian.alerts", true);
    }
    
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        if (alertsEnabled.contains(uuid)) {
            alertsEnabled.remove(uuid);
            sendMessage(sender, "commands.alerts.disabled");
        } else {
            alertsEnabled.add(uuid);
            sendMessage(sender, "commands.alerts.enabled");
        }
        
        return true;
    }
    
    public static boolean hasAlertsEnabled(UUID uuid) {
        return alertsEnabled.contains(uuid);
    }
    
    public static Set<UUID> getAlertReceivers() {
        return new HashSet<>(alertsEnabled);
    }
} 