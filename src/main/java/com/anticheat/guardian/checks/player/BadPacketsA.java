package com.anticheat.guardian.checks.player;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class BadPacketsA extends Check {
    
    private static final int MAX_PITCH = 90;
    private static final int MIN_PITCH = -90;
    private static final double MAX_DELTA_YAW = 40.0;
    private static final int SAMPLE_SIZE = 20;
    private static final double MAX_TELEPORT_DISTANCE = 100.0;
    
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Float> lastPitch = new HashMap<>();
    private final Map<UUID, LinkedList<Double>> yawChanges = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public BadPacketsA(Guardian plugin) {
        super(plugin, "BadPackets", "A", CheckCategory.PLAYER, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass") || 
            player.getGameMode() == GameMode.SPECTATOR) return;
        
        Location to = event.getTo();
        Location from = event.getFrom();
        UUID uuid = player.getUniqueId();
        
        // Check for invalid pitch
        float pitch = to.getPitch();
        if (pitch > MAX_PITCH || pitch < MIN_PITCH) {
            fail(player, data,
                String.format("Invalid pitch angle: %.1f", pitch),
                1.0
            );
            event.setCancelled(true);
            return;
        }
        
        // Check for sudden teleports
        Location last = lastLocation.get(uuid);
        if (last != null && !from.getWorld().equals(to.getWorld())) {
            fail(player, data, "World change without teleport", 2.0);
            event.setCancelled(true);
            return;
        }
        
        if (last != null) {
            double distance = to.distance(last);
            if (distance > MAX_TELEPORT_DISTANCE && !player.isFlying()) {
                fail(player, data,
                    String.format("Suspicious teleport: %.1f blocks", distance),
                    Math.min(distance / MAX_TELEPORT_DISTANCE, 2.0)
                );
                event.setCancelled(true);
                return;
            }
        }
        
        // Check for impossible head rotations
        Float lastYawValue = lastYaw.get(uuid);
        if (lastYawValue != null) {
            float yaw = to.getYaw();
            double yawDelta = Math.abs(yaw - lastYawValue);
            
            // Normalize yaw delta
            if (yawDelta > 180) {
                yawDelta = 360 - yawDelta;
            }
            
            // Track yaw changes for pattern analysis
            LinkedList<Double> changes = yawChanges.computeIfAbsent(uuid, k -> new LinkedList<>());
            changes.add(yawDelta);
            
            // Keep only recent changes
            while (changes.size() > SAMPLE_SIZE) {
                changes.removeFirst();
            }
            
            // Analyze yaw changes for suspicious patterns
            if (changes.size() >= SAMPLE_SIZE) {
                analyzeRotationPattern(player, data, changes);
            }
            
            // Check for instant 180 turns
            if (yawDelta > MAX_DELTA_YAW && !player.isFlying()) {
                fail(player, data,
                    String.format("Suspicious rotation: %.1f°", yawDelta),
                    Math.min(yawDelta / MAX_DELTA_YAW, 2.0)
                );
                event.setCancelled(true);
                return;
            }
            
            lastYaw.put(uuid, yaw);
        }
        
        // Check for invalid movement vectors
        Vector velocity = to.toVector().subtract(from.toVector());
        if (velocity.lengthSquared() > 0) {
            checkMovementVector(player, data, event, velocity);
        }
        
        // Update tracking data
        lastLocation.put(uuid, to);
        lastYaw.put(uuid, to.getYaw());
        lastPitch.put(uuid, to.getPitch());
    }
    
    private void analyzeRotationPattern(Player player, PlayerData data, 
                                      LinkedList<Double> changes) {
        // Calculate average and variance of yaw changes
        double sum = 0;
        double sumSquares = 0;
        
        for (double change : changes) {
            sum += change;
            sumSquares += change * change;
        }
        
        double mean = sum / changes.size();
        double variance = (sumSquares / changes.size()) - (mean * mean);
        
        // Check for suspiciously consistent rotations
        if (variance < 0.1 && mean > 1.0) {
            fail(player, data,
                String.format("Suspicious rotation pattern: %.2f variance", variance),
                1.0
            );
        }
    }
    
    private void checkMovementVector(Player player, PlayerData data, 
                                   PlayerMoveEvent event, Vector velocity) {
        // Check for invalid vertical movement
        if (velocity.getY() > 0 && !data.isOnGround() && 
            !player.isInWater() && !data.isOnGround()) {
            double maxUpward = 0.42; // Maximum upward velocity from jumping
            
            if (velocity.getY() > maxUpward) {
                fail(player, data,
                    String.format("Invalid upward movement: %.2f", velocity.getY()),
                    Math.min(velocity.getY() / maxUpward, 2.0)
                );
                event.setCancelled(true);
                return;
            }
        }
        
        // Check for invalid horizontal movement
        double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + 
                                         velocity.getZ() * velocity.getZ());
        double maxSpeed = getMaxSpeed(player);
        
        if (horizontalSpeed > maxSpeed) {
            fail(player, data,
                String.format("Invalid movement speed: %.2f (max: %.2f)", 
                    horizontalSpeed, maxSpeed),
                Math.min(horizontalSpeed / maxSpeed, 2.0)
            );
            event.setCancelled(true);
        }
    }
    
    private double getMaxSpeed(Player player) {
        double baseSpeed = player.isFlying() ? 1.0 : 0.3;
        
        // Apply speed effect
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int level = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            baseSpeed *= (1.0 + 0.2 * level);
        }
        
        return baseSpeed;
    }
    
    public void cleanup(UUID uuid) {
        lastLocation.remove(uuid);
        lastYaw.remove(uuid);
        lastPitch.remove(uuid);
        yawChanges.remove(uuid);
        violations.remove(uuid);
    }
} 