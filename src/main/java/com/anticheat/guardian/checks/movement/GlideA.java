package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class GlideA extends Check {
    
    private static final double MIN_FALL_SPEED = -3.92; // Maximum vanilla fall speed
    private static final double MAX_GLIDE_SPEED = -0.15; // Maximum elytra glide speed
    private static final double MIN_GLIDE_SPEED = -0.98; // Minimum elytra glide speed
    private static final int SAMPLE_SIZE = 10; // Number of movements to analyze
    private static final double GRAVITY = -0.08; // Vanilla gravity constant
    private static final double AIR_RESISTANCE = 0.98; // Air resistance factor
    
    private final Map<UUID, LinkedList<Double>> fallSpeedSamples = new HashMap<>();
    private final Map<UUID, Vector> lastVelocity = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public GlideA(Guardian plugin) {
        super(plugin, "Glide", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass") || 
            player.getGameMode() == GameMode.CREATIVE || 
            player.getGameMode() == GameMode.SPECTATOR) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        UUID uuid = player.getUniqueId();
        
        // Skip if player is using legitimate means of slow falling
        if (isLegitimateSlowFall(player)) {
            resetData(uuid);
            return;
        }
        
        // Calculate vertical movement
        double verticalSpeed = to.getY() - from.getY();
        
        // Skip if not falling
        if (verticalSpeed >= 0) {
            resetData(uuid);
            return;
        }
        
        // Get fall speed samples
        LinkedList<Double> samples = fallSpeedSamples.computeIfAbsent(uuid, 
            k -> new LinkedList<>());
        samples.add(verticalSpeed);
        
        // Keep only recent samples
        while (samples.size() > SAMPLE_SIZE) {
            samples.removeFirst();
        }
        
        // Only check if we have enough samples
        if (samples.size() >= SAMPLE_SIZE) {
            checkGlidePattern(player, data, event, samples);
        }
        
        // Update last velocity
        Vector velocity = to.toVector().subtract(from.toVector());
        lastVelocity.put(uuid, velocity);
    }
    
    private void checkGlidePattern(Player player, PlayerData data, PlayerMoveEvent event,
                                 LinkedList<Double> samples) {
        UUID uuid = player.getUniqueId();
        
        // Calculate statistics
        double sum = 0;
        double sumSquares = 0;
        
        for (double speed : samples) {
            sum += speed;
            sumSquares += speed * speed;
        }
        
        double mean = sum / samples.size();
        double variance = (sumSquares / samples.size()) - (mean * mean);
        double stdDev = Math.sqrt(variance);
        
        // Get expected fall speed
        double expectedSpeed = calculateExpectedFallSpeed(player);
        
        // Check for invalid fall speed
        if (mean > expectedSpeed && stdDev < 0.1) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("Invalid fall speed: %.3f (±%.3f)", mean, stdDev),
                Math.min(vl / 2.0, 2.0)
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
            return;
        }
        
        // Check for elytra glide speed
        if (player.isGliding() && (mean > MAX_GLIDE_SPEED || mean < MIN_GLIDE_SPEED)) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("Invalid glide speed: %.3f (min: %.2f, max: %.2f)", 
                    mean, MIN_GLIDE_SPEED, MAX_GLIDE_SPEED),
                Math.min(vl / 2.0, 2.0)
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
            return;
        }
        
        // Check for consistent fall speed (glide hack)
        if (stdDev < 0.01 && mean < -0.1) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("Suspicious glide pattern: %.3f (±%.3f)", mean, stdDev),
                Math.min(vl / 2.0, 2.0)
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
            return;
        }
        
        // Decrease violations on valid movement
        violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
    }
    
    private boolean isLegitimateSlowFall(Player player) {
        // Check for legitimate means of slow falling
        return player.isGliding() ||
               player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
               player.hasPotionEffect(PotionEffectType.LEVITATION) ||
               isInLiquid(player) ||
               isOnClimbable(player) ||
               hasBlocksAround(player);
    }
    
    private boolean isInLiquid(Player player) {
        Block block = player.getLocation().getBlock();
        return block.isLiquid();
    }
    
    private boolean isOnClimbable(Player player) {
        Block block = player.getLocation().getBlock();
        return block.getType() == Material.LADDER || 
               block.getType() == Material.VINE ||
               block.getType() == Material.TWISTING_VINES ||
               block.getType() == Material.WEEPING_VINES;
    }
    
    private boolean hasBlocksAround(Player player) {
        Location loc = player.getLocation();
        Block block = loc.getBlock();
        
        // Check surrounding blocks
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, 
                                             BlockFace.EAST, BlockFace.WEST}) {
            if (block.getRelative(face).getType().isSolid()) {
                return true;
            }
        }
        
        return false;
    }
    
    private double calculateExpectedFallSpeed(Player player) {
        Vector lastVel = lastVelocity.get(player.getUniqueId());
        if (lastVel == null) {
            return MIN_FALL_SPEED;
        }
        
        // Apply gravity and air resistance
        double newSpeed = (lastVel.getY() + GRAVITY) * AIR_RESISTANCE;
        
        // Clamp to maximum fall speed
        return Math.max(newSpeed, MIN_FALL_SPEED);
    }
    
    private void resetData(UUID uuid) {
        fallSpeedSamples.remove(uuid);
        lastVelocity.remove(uuid);
    }
    
    public void cleanup(UUID uuid) {
        fallSpeedSamples.remove(uuid);
        lastVelocity.remove(uuid);
        violations.remove(uuid);
    }
} 