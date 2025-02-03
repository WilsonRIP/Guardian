package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class FlightA extends Check {
    
    private static final double MAX_ASCEND_SPEED = 0.42;
    private static final double GRAVITY = 0.08;
    private static final double AIR_FRICTION = 0.02;
    
    public FlightA(Guardian plugin) {
        super(plugin, "Flight", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle() || 
            player.isSwimming() || isInClimbable(player)) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        
        double deltaY = to.getY() - from.getY();
        double lastDeltaY = data.getLastDeltaY();
        
        // Check for instant y-change
        if (deltaY > MAX_ASCEND_SPEED && !isNearGround(player)) {
            fail(player, data, 
                String.format("Ascend speed too high: %.3f > %.3f", deltaY, MAX_ASCEND_SPEED),
                Math.min(deltaY - MAX_ASCEND_SPEED, 2.0)
            );
            event.setCancelled(true);
            player.teleport(data.getLastGroundLocation());
            return;
        }
        
        // Check for invalid gravity
        if (!data.isOnGround()) {
            double expectedDeltaY = lastDeltaY - GRAVITY;
            double difference = Math.abs(deltaY - expectedDeltaY);
            
            if (difference > AIR_FRICTION && !isNearGround(player)) {
                fail(player, data,
                    String.format("Invalid gravity: %.3f != %.3f (diff: %.3f)", 
                    deltaY, expectedDeltaY, difference),
                    Math.min(difference, 2.0)
                );
                event.setCancelled(true);
                player.teleport(data.getLastGroundLocation());
            }
        }
        
        // Update data
        data.setLastDeltaY(deltaY);
    }
    
    private boolean isNearGround(Player player) {
        Location loc = player.getLocation();
        double expand = 0.3;
        
        for (double x = -expand; x <= expand; x += expand) {
            for (double z = -expand; z <= expand; z += expand) {
                if (loc.clone().add(x, -0.5, z).getBlock().getType().isSolid()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isInClimbable(Player player) {
        Block block = player.getLocation().getBlock();
        return block.getType() == Material.LADDER || 
               block.getType() == Material.VINE || 
               block.getType() == Material.SCAFFOLDING;
    }
} 