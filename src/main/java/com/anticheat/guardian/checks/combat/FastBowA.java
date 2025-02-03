package com.anticheat.guardian.checks.combat;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastBowA extends Check {
    
    private static final long MIN_DRAW_TIME = 300L; // Minimum time to draw bow (ms)
    private static final float MIN_FORCE = 0.1f; // Minimum force for a valid shot
    private static final float MAX_FORCE_PER_TICK = 0.01f; // Maximum force increase per tick
    private static final int SAMPLE_SIZE = 5; // Number of shots to analyze
    
    private final Map<UUID, Long> drawStartTime = new HashMap<>();
    private final Map<UUID, Float[]> forceSamples = new HashMap<>();
    private final Map<UUID, Integer> sampleIndex = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public FastBowA(Guardian plugin) {
        super(plugin, "FastBow", "A", CheckCategory.COMBAT, 5.0, false);
    }
    
    public void handleBowStart(Player player) {
        UUID uuid = player.getUniqueId();
        drawStartTime.put(uuid, System.currentTimeMillis());
    }
    
    public void handle(Player player, PlayerData data, EntityShootBowEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        float force = event.getForce();
        
        // Skip weak shots
        if (force < MIN_FORCE) {
            return;
        }
        
        // Check draw time
        Long startTime = drawStartTime.get(uuid);
        if (startTime != null) {
            long drawTime = System.currentTimeMillis() - startTime;
            
            // Check if draw time is too short
            if (drawTime < MIN_DRAW_TIME) {
                int vl = violations.getOrDefault(uuid, 0) + 1;
                fail(player, data,
                    String.format("Bow drawn too fast: %dms (min: %dms)", 
                        drawTime, MIN_DRAW_TIME),
                    Math.min(vl / 2.0, 2.0)
                );
                event.setCancelled(true);
                violations.put(uuid, vl);
                return;
            }
            
            float expectedForce = calculateExpectedForce(drawTime);
            
            // Get force samples for this player
            Float[] samples = forceSamples.computeIfAbsent(uuid, k -> new Float[SAMPLE_SIZE]);
            int index = sampleIndex.getOrDefault(uuid, 0);
            
            // Store the force sample
            samples[index] = force;
            index = (index + 1) % SAMPLE_SIZE;
            sampleIndex.put(uuid, index);
            
            // Check for invalid force
            if (force > expectedForce) {
                // Only flag if we have enough samples showing consistent abuse
                if (hasEnoughSamples(samples) && isConsistentlyFast(samples, expectedForce)) {
                    int vl = violations.getOrDefault(uuid, 0) + 1;
                    
                    fail(player, data,
                        String.format("Bow drawn too fast: %.2f force in %dms", 
                            force, drawTime),
                        Math.min(vl / 2.0, 2.0)
                    );
                    
                    event.setCancelled(true);
                    violations.put(uuid, vl);
                    return;
                }
            } else {
                violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
            }
            
            sampleIndex.put(uuid, index);
        }
        
        // Check arrow velocity
        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            double velocity = arrow.getVelocity().length();
            double maxVelocity = getMaxArrowVelocity(player, force);
            
            if (velocity > maxVelocity) {
                int vl = violations.getOrDefault(uuid, 0) + 1;
                
                fail(player, data,
                    String.format("Arrow velocity too high: %.2f (max: %.2f)", 
                        velocity, maxVelocity),
                    Math.min(vl / 2.0, 2.0)
                );
                
                event.setCancelled(true);
                violations.put(uuid, vl);
            }
        }
        
        // Clear draw start time
        drawStartTime.remove(uuid);
    }
    
    private float calculateExpectedForce(long drawTime) {
        // Convert draw time to ticks
        int ticks = (int) (drawTime / 50); // 50ms per tick
        return Math.min(1.0f, ticks * MAX_FORCE_PER_TICK);
    }
    
    private boolean hasEnoughSamples(Float[] samples) {
        for (Float sample : samples) {
            if (sample == null) return false;
        }
        return true;
    }
    
    private boolean isConsistentlyFast(Float[] samples, float expectedForce) {
        int fastShots = 0;
        for (Float force : samples) {
            if (force > expectedForce) {
                fastShots++;
            }
        }
        return fastShots >= (samples.length * 0.8); // 80% of shots are too fast
    }
    
    private double getMaxArrowVelocity(Player player, float force) {
        double baseVelocity = 3.0 * force; // Base arrow velocity
        
        // Check for Power enchantment
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow.getType() != Material.BOW) {
            bow = player.getInventory().getItemInOffHand();
        }
        
        if (bow.getType() == Material.BOW) {
            int powerLevel = bow.getEnchantmentLevel(Enchantment.POWER);
            if (powerLevel > 0) {
                baseVelocity *= (1.0 + 0.25 * (powerLevel + 1));
            }
        }
        
        return baseVelocity;
    }
    
    public void cleanup(UUID uuid) {
        drawStartTime.remove(uuid);
        forceSamples.remove(uuid);
        sampleIndex.remove(uuid);
        violations.remove(uuid);
    }
} 