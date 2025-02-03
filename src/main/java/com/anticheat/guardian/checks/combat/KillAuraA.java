package com.anticheat.guardian.checks.combat;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class KillAuraA extends Check {
    
    private static final int MAX_ANGLE = 150;
    private static final long MIN_ATTACK_DELAY = 50L; // 50ms between attacks
    
    public KillAuraA(Guardian plugin) {
        super(plugin, "KillAura", "A", CheckCategory.COMBAT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        long now = System.currentTimeMillis();
        
        // Check attack speed
        long timeDelta = now - data.getLastAttackTime();
        if (timeDelta < MIN_ATTACK_DELAY) {
            fail(player, data, "Attack speed too high: " + timeDelta + "ms", 1.0);
            event.setCancelled(true);
            return;
        }
        
        // Check attack angle
        Location playerLoc = player.getLocation();
        Vector playerDirection = playerLoc.getDirection();
        Vector toTarget = target.getLocation().toVector().subtract(playerLoc.toVector()).normalize();
        
        double angle = Math.toDegrees(Math.acos(playerDirection.dot(toTarget)));
        
        if (angle > MAX_ANGLE) {
            fail(player, data, "Invalid attack angle: " + String.format("%.1f", angle) + "°", 1.0);
            event.setCancelled(true);
            return;
        }
        
        // Check multi-aura (hitting multiple entities too quickly)
        if (data.getHitCount() > 2 && timeDelta < 100) {
            fail(player, data, "Multi-aura detected: " + data.getHitCount() + " hits", 1.0);
            event.setCancelled(true);
            return;
        }
        
        // Update data
        data.setLastAttackTime(now);
        data.incrementHitCount();
    }
} 