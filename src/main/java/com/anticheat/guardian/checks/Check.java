package com.anticheat.guardian.checks;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.data.PlayerData;
import lombok.Getter;
import org.bukkit.ChatColor;
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
        
        // Format alert message
        String alert = String.format(
            "%s%s failed %s %s check (VL: %.1f/%s) %s",
            ChatColor.RED,
            player.getName(),
            name,
            type,
            vl,
            maxVL,
            ChatColor.GRAY + "(" + info + ")"
        );
        
        // Broadcast to staff
        plugin.broadcastAlert(alert);
        
        // Log to console if severe
        if (vl >= maxVL) {
            plugin.getLogger().warning(ChatColor.stripColor(alert));
        }
    }
    
    protected void debug(Player player, String message) {
        plugin.getLogger().info("[Debug] [" + name + "] " + player.getName() + ": " + message);
    }
} 