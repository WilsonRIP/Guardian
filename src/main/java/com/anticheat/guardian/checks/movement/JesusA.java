package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JesusA extends Check {
    
    private static final double MAX_WATER_OFFSET = 0.1; // Maximum allowed distance above water
    private static final int MAX_TICKS_ON_WATER = 10; // Maximum ticks allowed on water surface
    private static final double WATER_SURFACE_Y = 0.85; // Y-level of water surface
    
    private final Map<UUID, Integer> ticksOnWater = new HashMap<>();
    private final Map<UUID, Location> lastWaterLocation = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public JesusA(Guardian plugin) {
        super(plugin, "Jesus", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        Location to = event.getTo();
        UUID uuid = player.getUniqueId();
        
        // Skip if player is using items that allow water walking
        if (isLegitimateWaterWalking(player)) {
            resetData(uuid);
            return;
        }
        
        Block block = to.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        
        // Check if player is on or near water
        if (isWater(block) || isWater(below)) {
            handleWaterMovement(player, data, event, uuid, to);
        } else {
            resetData(uuid);
        }
    }
    
    private void handleWaterMovement(Player player, PlayerData data, PlayerMoveEvent event,
                                   UUID uuid, Location to) {
        int ticks = ticksOnWater.getOrDefault(uuid, 0);
        Location lastLoc = lastWaterLocation.get(uuid);
        
        // Check vertical position relative to water surface
        double yOffset = to.getY() % 1;
        if (Math.abs(yOffset - WATER_SURFACE_Y) < MAX_WATER_OFFSET) {
            ticks++;
            
            // Check if player has been on water surface too long
            if (ticks > MAX_TICKS_ON_WATER) {
                fail(player, data,
                    String.format("Walking on water for too long: %d ticks", ticks),
                    Math.min(ticks / 20.0, 2.0)
                );
                event.setCancelled(true);
                ticks = 0;
            }
            
            // Check for invalid water movement patterns
            if (lastLoc != null) {
                checkWaterPattern(player, data, event, to, lastLoc);
            }
        } else {
            ticks = Math.max(0, ticks - 2); // Decrease ticks when not exactly on surface
        }
        
        ticksOnWater.put(uuid, ticks);
        lastWaterLocation.put(uuid, to);
    }
    
    private void checkWaterPattern(Player player, PlayerData data, PlayerMoveEvent event,
                                 Location current, Location last) {
        Vector velocity = current.toVector().subtract(last.toVector());
        
        // Check for invalid vertical movement in water
        if (velocity.getY() > 0 && !player.isSwimming()) {
            Block above = current.getBlock().getRelative(BlockFace.UP);
            if (!isWater(above)) {
                fail(player, data,
                    String.format("Invalid upward movement in water: %.2f", velocity.getY()),
                    1.0
                );
                event.setCancelled(true);
                return;
            }
        }
        
        // Check for horizontal speed in water
        double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + 
                                         velocity.getZ() * velocity.getZ());
        double maxSpeed = getMaxWaterSpeed(player);
        
        if (horizontalSpeed > maxSpeed) {
            fail(player, data,
                String.format("Moving too fast in water: %.2f (max: %.2f)", 
                    horizontalSpeed, maxSpeed),
                Math.min(horizontalSpeed / maxSpeed, 2.0)
            );
            event.setCancelled(true);
        }
    }
    
    private boolean isLegitimateWaterWalking(Player player) {
        // Check for items/effects that allow water walking
        return player.isFlying() || 
               player.isGliding() ||
               player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE) ||
               player.getInventory().getBoots() != null && 
               player.getInventory().getBoots().getType().name().equals("FROST_WALKER");
    }
    
    private double getMaxWaterSpeed(Player player) {
        double baseSpeed = 0.2;
        
        // Apply potion effects
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int level = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            baseSpeed *= (1.0 + 0.2 * level);
        }
        
        if (player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
            baseSpeed *= 1.4;
        }
        
        return baseSpeed;
    }
    
    private boolean isWater(Block block) {
        return block.getType() == Material.WATER;
    }
    
    private void resetData(UUID uuid) {
        ticksOnWater.remove(uuid);
        lastWaterLocation.remove(uuid);
    }
    
    public void cleanup(UUID uuid) {
        ticksOnWater.remove(uuid);
        lastWaterLocation.remove(uuid);
        violations.remove(uuid);
    }
} 