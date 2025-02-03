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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastLadderA extends Check {
    
    private static final double MAX_LADDER_SPEED = 0.12; // Maximum vanilla ladder speed
    private static final double MAX_LADDER_SPEED_JUMP = 0.15; // Maximum speed when jumping on ladder
    private static final double MAX_LADDER_SPEED_SNEAK = 0.08; // Maximum speed when sneaking
    private static final int SAMPLE_SIZE = 5; // Number of movements to analyze
    
    private final Map<UUID, Double[]> speedSamples = new HashMap<>();
    private final Map<UUID, Integer> sampleIndex = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public FastLadderA(Guardian plugin) {
        super(plugin, "FastLadder", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        UUID uuid = player.getUniqueId();
        
        // Check if player is on ladder
        if (!isOnLadder(player)) {
            resetData(uuid);
            return;
        }
        
        // Calculate vertical speed
        double verticalSpeed = Math.abs(to.getY() - from.getY());
        
        // Skip if not moving vertically
        if (verticalSpeed == 0) {
            return;
        }
        
        // Get speed samples for this player
        Double[] samples = speedSamples.computeIfAbsent(uuid, k -> new Double[SAMPLE_SIZE]);
        int index = sampleIndex.getOrDefault(uuid, 0);
        
        // Store the speed sample
        samples[index] = verticalSpeed;
        index = (index + 1) % SAMPLE_SIZE;
        sampleIndex.put(uuid, index);
        
        // Only check once we have enough samples
        if (hasEnoughSamples(samples)) {
            double averageSpeed = calculateAverageSpeed(samples);
            double maxAllowedSpeed = getMaxAllowedSpeed(player, data);
            
            if (averageSpeed > maxAllowedSpeed) {
                int vl = violations.getOrDefault(uuid, 0) + 1;
                
                fail(player, data,
                    String.format("Ladder speed too high: %.3f (max: %.3f)", 
                        averageSpeed, maxAllowedSpeed),
                    Math.min(vl / 2.0, 2.0)
                );
                
                event.setCancelled(true);
                violations.put(uuid, vl);
                return;
            }
            
            // Decrease violations on valid movement
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
    }
    
    private boolean isOnLadder(Player player) {
        Location loc = player.getLocation();
        Block block = loc.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        
        return isLadderMaterial(block.getType()) || 
               isLadderMaterial(below.getType());
    }
    
    private boolean isLadderMaterial(Material material) {
        return material == Material.LADDER || 
               material == Material.VINE || 
               material == Material.TWISTING_VINES || 
               material == Material.WEEPING_VINES;
    }
    
    private double getMaxAllowedSpeed(Player player, PlayerData data) {
        double maxSpeed = MAX_LADDER_SPEED;
        
        // Adjust for player state
        if (player.isSneaking()) {
            maxSpeed = MAX_LADDER_SPEED_SNEAK;
        } else if (!data.isOnGround()) {
            maxSpeed = MAX_LADDER_SPEED_JUMP;
        }
        
        // Adjust for speed effect
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int level = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            maxSpeed *= (1.0 + 0.2 * level);
        }
        
        return maxSpeed;
    }
    
    private boolean hasEnoughSamples(Double[] samples) {
        for (Double sample : samples) {
            if (sample == null) return false;
        }
        return true;
    }
    
    private double calculateAverageSpeed(Double[] samples) {
        double sum = 0;
        for (Double sample : samples) {
            sum += sample;
        }
        return sum / samples.length;
    }
    
    private void resetData(UUID uuid) {
        speedSamples.remove(uuid);
        sampleIndex.remove(uuid);
    }
    
    public void cleanup(UUID uuid) {
        speedSamples.remove(uuid);
        sampleIndex.remove(uuid);
        violations.remove(uuid);
    }
} 