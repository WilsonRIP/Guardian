package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VelocityA extends Check {
    
    private static final double MIN_VELOCITY = 0.01; // Minimum velocity to check
    private static final double MIN_EXPECTED_MOTION = 0.5; // Minimum expected motion from knockback
    private static final int MAX_VELOCITY_TICKS = 20; // Maximum ticks to track velocity
    private static final double VERTICAL_VELOCITY_DIVISOR = 1.5; // Vertical velocity reduction
    
    private final Map<UUID, Vector> pendingVelocity = new HashMap<>();
    private final Map<UUID, Integer> velocityTicks = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public VelocityA(Guardian plugin) {
        super(plugin, "Velocity", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handleVelocity(Player player, PlayerData data, PlayerVelocityEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        Vector velocity = event.getVelocity();
        UUID uuid = player.getUniqueId();
        
        // Only check significant velocity changes
        if (velocity.lengthSquared() < MIN_VELOCITY) {
            return;
        }
        
        // Store the expected velocity
        pendingVelocity.put(uuid, velocity);
        velocityTicks.put(uuid, 0);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        Vector expectedVelocity = pendingVelocity.get(uuid);
        
        if (expectedVelocity != null) {
            int ticks = velocityTicks.getOrDefault(uuid, 0);
            
            if (ticks > MAX_VELOCITY_TICKS) {
                // Remove expired velocity
                pendingVelocity.remove(uuid);
                velocityTicks.remove(uuid);
                return;
            }
            
            Location from = event.getFrom();
            Location to = event.getTo();
            Vector movement = to.toVector().subtract(from.toVector());
            
            // Check if player took expected knockback
            if (!isValidVelocity(movement, expectedVelocity, ticks)) {
                int vl = violations.getOrDefault(uuid, 0) + 1;
                
                fail(player, data,
                    String.format("Invalid velocity: %.2f/%.2f (expected)",
                        movement.length(), expectedVelocity.length()),
                    Math.min(vl / 2.0, 2.0)
                );
                
                event.setCancelled(true);
                violations.put(uuid, vl);
            } else {
                violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
            }
            
            velocityTicks.put(uuid, ticks + 1);
        }
    }
    
    private boolean isValidVelocity(Vector actual, Vector expected, int ticks) {
        // Adjust expected velocity based on ticks passed
        double horizontalReduction = Math.pow(0.91, ticks); // Air resistance
        double verticalReduction = Math.pow(0.98, ticks); // Gravity
        
        Vector adjusted = expected.clone();
        
        // Apply horizontal reduction
        adjusted.setX(adjusted.getX() * horizontalReduction);
        adjusted.setZ(adjusted.getZ() * horizontalReduction);
        
        // Apply vertical reduction
        adjusted.setY((adjusted.getY() - 0.08) * verticalReduction);
        
        // Calculate minimum expected motion
        double expectedMotion = adjusted.length() * MIN_EXPECTED_MOTION;
        double actualMotion = actual.length();
        
        // Allow for some variance in vertical motion
        if (Math.abs(actual.getY()) > Math.abs(adjusted.getY()) / VERTICAL_VELOCITY_DIVISOR) {
            return true;
        }
        
        return actualMotion >= expectedMotion;
    }
    
    public void cleanup(UUID uuid) {
        pendingVelocity.remove(uuid);
        velocityTicks.remove(uuid);
        violations.remove(uuid);
    }
} 