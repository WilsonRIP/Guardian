package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class StrafeA extends Check {
    
    private static final int SAMPLE_SIZE = 20;
    private static final double MAX_STRAFE_ANGLE = 90.0;
    private static final double MIN_STRAFE_SPEED = 0.15;
    private static final double PERFECT_ANGLE_THRESHOLD = 0.1;
    
    private final Map<UUID, LinkedList<Double>> strafeAngles = new HashMap<>();
    private final Map<UUID, Vector> lastMovement = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Integer> perfectAngles = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public StrafeA(Guardian plugin) {
        super(plugin, "Strafe", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        UUID uuid = player.getUniqueId();
        
        // Get movement vector
        Vector movement = to.toVector().subtract(from.toVector());
        double speed = Math.sqrt(movement.getX() * movement.getX() + 
                               movement.getZ() * movement.getZ());
        
        // Skip small movements
        if (speed < MIN_STRAFE_SPEED) {
            return;
        }
        
        // Get yaw change
        float yaw = to.getYaw();
        Float lastYawValue = lastYaw.get(uuid);
        
        if (lastYawValue != null) {
            Vector lastMove = lastMovement.get(uuid);
            if (lastMove != null) {
                // Calculate strafe angle
                double angle = calculateStrafeAngle(movement, lastMove);
                
                // Track angles for pattern analysis
                LinkedList<Double> angles = strafeAngles.computeIfAbsent(uuid, 
                    k -> new LinkedList<>());
                angles.add(angle);
                
                // Keep only recent angles
                while (angles.size() > SAMPLE_SIZE) {
                    angles.removeFirst();
                }
                
                // Check for invalid strafing
                if (angles.size() >= SAMPLE_SIZE) {
                    checkStrafePattern(player, data, event, uuid, angles);
                }
                
                // Check for perfect angles
                if (isPerfectAngle(angle)) {
                    int perfect = perfectAngles.getOrDefault(uuid, 0) + 1;
                    if (perfect > 5) {
                        int vl = violations.getOrDefault(uuid, 0) + 1;
                        
                        fail(player, data,
                            String.format("Too many perfect angles: %d", perfect),
                            Math.min(vl / 2.0, 2.0)
                        );
                        
                        event.setCancelled(true);
                        violations.put(uuid, vl);
                        perfectAngles.put(uuid, 0);
                    } else {
                        perfectAngles.put(uuid, perfect);
                    }
                } else {
                    perfectAngles.put(uuid, Math.max(0, perfectAngles.getOrDefault(uuid, 0) - 1));
                }
            }
        }
        
        // Update tracking data
        lastMovement.put(uuid, movement);
        lastYaw.put(uuid, yaw);
    }
    
    private double calculateStrafeAngle(Vector current, Vector last) {
        // Normalize vectors
        current = current.normalize();
        last = last.normalize();
        
        // Calculate angle between movements
        double dot = current.getX() * last.getX() + current.getZ() * last.getZ();
        double angle = Math.toDegrees(Math.acos(dot));
        
        return angle;
    }
    
    private void checkStrafePattern(Player player, PlayerData data, PlayerMoveEvent event,
                                  UUID uuid, LinkedList<Double> angles) {
        // Calculate statistics
        double sum = 0;
        double sumSquares = 0;
        
        for (double angle : angles) {
            sum += angle;
            sumSquares += angle * angle;
        }
        
        double mean = sum / angles.size();
        double variance = (sumSquares / angles.size()) - (mean * mean);
        double stdDev = Math.sqrt(variance);
        
        // Check for suspicious patterns
        if (stdDev < 1.0 && mean > MAX_STRAFE_ANGLE) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("Suspicious strafe pattern: %.2f° (±%.2f°)", 
                    mean, stdDev),
                Math.min(vl / 2.0, 2.0)
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
        } else {
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
    }
    
    private boolean isPerfectAngle(double angle) {
        // Check if angle is suspiciously close to common strafe angles
        double[] commonAngles = {0, 45, 90, 135, 180};
        
        for (double common : commonAngles) {
            if (Math.abs(angle - common) < PERFECT_ANGLE_THRESHOLD) {
                return true;
            }
        }
        
        return false;
    }
    
    public void cleanup(UUID uuid) {
        strafeAngles.remove(uuid);
        lastMovement.remove(uuid);
        lastYaw.remove(uuid);
        perfectAngles.remove(uuid);
        violations.remove(uuid);
    }
} 