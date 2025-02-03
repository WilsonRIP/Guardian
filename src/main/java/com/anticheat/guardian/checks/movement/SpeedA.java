package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;

public class SpeedA extends Check {
    
    private static final double BASE_SPEED = 0.2873;
    private static final double SPRINT_MODIFIER = 1.3;
    
    public SpeedA(Guardian plugin) {
        super(plugin, "Speed", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        
        Vector velocity = to.toVector().subtract(from.toVector());
        double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        
        // Calculate maximum allowed speed
        double maxSpeed = BASE_SPEED;
        
        // Apply sprint modifier
        if (player.isSprinting()) {
            maxSpeed *= SPRINT_MODIFIER;
        }
        
        // Apply potion effects
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int level = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            maxSpeed *= 1.0 + (0.2 * level);
        }
        
        // Check for blocks that affect movement
        Block below = to.getBlock().getRelative(BlockFace.DOWN);
        Block at = to.getBlock();
        
        if (below.getType() == Material.SOUL_SAND || 
            at.getType() == Material.SOUL_SAND) {
            maxSpeed *= 0.4; // Soul sand significantly reduces speed
        }
        
        // Add tolerance for server lag
        maxSpeed *= 1.1;
        
        // Check if speed exceeds maximum
        if (horizontalSpeed > maxSpeed) {
            fail(player, data, 
                String.format("Speed: %.3f > %.3f", horizontalSpeed, maxSpeed),
                Math.min(horizontalSpeed - maxSpeed, 2.0)
            );
            
            // Only cancel if significantly over limit
            if (horizontalSpeed > maxSpeed * 1.5) {
                event.setCancelled(true);
                player.teleport(data.getLastGroundLocation());
            }
        }
        
        // Update last location
        data.setLastLocation(to);
    }
} 