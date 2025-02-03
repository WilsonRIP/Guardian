package com.anticheat.guardian.checks;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.data.PlayerData;
import com.anticheat.guardian.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public abstract class Check {
    
    protected final Guardian plugin;
    protected final String name;
    protected final String type;
    protected final CheckCategory category;
    protected final double maxVL;
    protected final boolean experimental;
    
    public Check(Guardian plugin, String name, String type, CheckCategory category, double maxVL, boolean experimental) {
        this.plugin = plugin;
        this.name = name;
        this.type = type;
        this.category = category;
        this.maxVL = maxVL;
        this.experimental = experimental;
    }
    
    protected void fail(Player player, PlayerData data, String info, double vl) {
        if (player.hasPermission("guardian.bypass")) return;
        
        switch (category) {
            case COMBAT:
                data.incrementCombatVL();
                break;
            case MOVEMENT:
                data.incrementMovementVL();
                break;
            default:
                break;
        }
        
        // Format alert message using messages.yml
        String alert = MessageUtils.getMessage("alerts.format", 
            "{player}", player.getName(),
            "{check}", name,
            "{type}", type,
            "{vl}", String.format("%.1f", vl),
            "{maxVL}", String.valueOf(maxVL),
            "{info}", info
        );
        
        // Broadcast to staff
        plugin.broadcastAlert(alert);
        
        // Log to console if severe
        if (vl >= maxVL) {
            String consoleMessage = MessageUtils.getMessage("alerts.console-format",
                "{player}", player.getName(),
                "{check}", name,
                "{type}", type,
                "{vl}", String.format("%.1f", vl),
                "{maxVL}", String.valueOf(maxVL),
                "{info}", info
            );
            plugin.getLogger().warning(MessageUtils.stripColor(consoleMessage));
        }
    }
    
    protected void debug(Player player, String message) {
        plugin.getLogger().info("[Debug] [" + name + "] " + player.getName() + ": " + message);
    }
} 