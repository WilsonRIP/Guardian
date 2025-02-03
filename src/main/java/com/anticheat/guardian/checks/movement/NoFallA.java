package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class NoFallA extends Check {
    
    private static final double FALL_DAMAGE_THRESHOLD = 3.0;
    private static final double GRAVITY = 0.08;
    private static final double AIR_RESISTANCE = 0.02;
    
    public NoFallA(Guardian plugin) {
        super(plugin, "NoFall", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle() || 
            player.isSwimming() || player.isInWater() || player.hasPermission("guardian.bypass")) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        
        double deltaY = to.getY() - from.getY();
        double lastDeltaY = data.getLastDeltaY();
        
        // Check for sudden changes in fall speed
        if (lastDeltaY < -FALL_DAMAGE_THRESHOLD) {
            double expectedDeltaY = lastDeltaY - GRAVITY;
            double difference = Math.abs(deltaY - expectedDeltaY);
            
            // Allow for some variance due to air resistance
            if (difference > AIR_RESISTANCE && !data.isOnGround()) {
                // Check if player suddenly stopped falling
                if (deltaY > expectedDeltaY + 0.1) {
                    fail(player, data,
                        String.format("Invalid fall motion: %.3f > %.3f (diff: %.3f)", 
                        deltaY, expectedDeltaY, difference),
                        Math.min(difference, 2.0)
                    );
                    
                    // Force the player to take fall damage
                    double fallDistance = -lastDeltaY * 10; // Convert to approximate blocks
                    player.damage(fallDistance - 3.0); // Minecraft's fall damage formula
                }
            }
        }
        
        // Check for ground spoofing
        if (data.isOnGround() && deltaY < -0.4 && !isValidGround(player.getLocation())) {
            fail(player, data, "Ground spoofing detected", 1.0);
            data.setOnGround(false);
        }
        
        data.setLastDeltaY(deltaY);
    }
    
    private boolean isValidGround(Location location) {
        double expand = 0.3;
        for (double x = -expand; x <= expand; x += expand) {
            for (double z = -expand; z <= expand; z += expand) {
                if (location.clone().add(x, -0.5, z).getBlock().getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
} 