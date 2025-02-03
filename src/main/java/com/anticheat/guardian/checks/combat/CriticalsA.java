package com.anticheat.guardian.checks.combat;

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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CriticalsA extends Check {
    
    private static final double MIN_FALL_DISTANCE = 0.3; // Minimum fall distance for criticals
    private static final double MAX_FALL_DISTANCE = 0.4; // Maximum fall distance for criticals
    private static final long MIN_JUMP_TIME = 500L; // Minimum time between jumps (ms)
    
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public CriticalsA(Guardian plugin) {
        super(plugin, "Criticals", "A", CheckCategory.COMBAT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, EntityDamageByEntityEvent event) {
        if (player.hasPermission("guardian.bypass") || 
            player.getGameMode() == GameMode.CREATIVE) return;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Check if conditions allow for criticals
        if (!canPerformCritical(player)) {
            return;
        }
        
        // Check fall distance for critical hit
        double fallDistance = player.getFallDistance();
        if (fallDistance > 0 && !isValidCriticalFall(fallDistance)) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("Invalid critical hit: %.2f blocks", fallDistance),
                Math.min(vl / 2.0, 2.0)
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
            return;
        }
        
        // Check jump timing
        Long lastJump = lastJumpTime.get(uuid);
        if (lastJump != null) {
            long timeDiff = now - lastJump;
            if (timeDiff < MIN_JUMP_TIME) {
                int vl = violations.getOrDefault(uuid, 0) + 1;
                
                fail(player, data,
                    String.format("Jump too fast: %dms (min: %dms)", 
                        timeDiff, MIN_JUMP_TIME),
                    Math.min(vl / 2.0, 2.0)
                );
                
                event.setCancelled(true);
                violations.put(uuid, vl);
                return;
            }
        }
        
        // Update jump time if it was a valid critical
        if (fallDistance > 0) {
            lastJumpTime.put(uuid, now);
        } else {
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
    }
    
    private boolean canPerformCritical(Player player) {
        // Check if player is in a state where criticals are possible
        Location loc = player.getLocation();
        Block block = loc.getBlock();
        
        if (player.isInWater() || 
            block.getType() == Material.LAVA || 
            block.getType() == Material.COBWEB || 
            player.isGliding() || 
            player.isFlying() ||
            player.hasPotionEffect(PotionEffectType.BLINDNESS) ||
            player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return false;
        }
        
        // Check if player is on ladder or vine
        if (block.getType() == Material.LADDER || 
            block.getType() == Material.VINE) {
            return false;
        }
        
        // Check if player has blocks above
        Block above = block.getRelative(BlockFace.UP, 2);
        if (above.getType().isSolid()) {
            return false;
        }
        
        return true;
    }
    
    private boolean isValidCriticalFall(double fallDistance) {
        return fallDistance >= MIN_FALL_DISTANCE && 
               fallDistance <= MAX_FALL_DISTANCE;
    }
    
    public void cleanup(UUID uuid) {
        lastJumpTime.remove(uuid);
        violations.remove(uuid);
    }
} 