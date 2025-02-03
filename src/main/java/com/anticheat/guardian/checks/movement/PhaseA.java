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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhaseA extends Check {
    
    private static final double MIN_DISTANCE = 0.1; // Minimum distance to check for phase
    private static final int MAX_VIOLATIONS = 5; // Maximum violations before teleporting back
    
    private final Map<UUID, Location> lastSafeLocation = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public PhaseA(Guardian plugin) {
        super(plugin, "Phase", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        UUID uuid = player.getUniqueId();
        
        // Skip small movements
        if (from.distance(to) < MIN_DISTANCE) {
            return;
        }
        
        // Check if the player is moving through solid blocks
        if (isPhasing(from, to)) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("Phase detected: %d violations", vl),
                Math.min(vl / 2.0, 2.0)
            );
            
            // Teleport back after too many violations
            if (vl >= MAX_VIOLATIONS) {
                Location safe = lastSafeLocation.get(uuid);
                if (safe != null) {
                    event.setTo(safe);
                } else {
                    event.setCancelled(true);
                }
                violations.put(uuid, 0);
            } else {
                event.setCancelled(true);
                violations.put(uuid, vl);
            }
            return;
        }
        
        // Update safe location if not in a block
        if (!isInBlock(to)) {
            lastSafeLocation.put(uuid, to);
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
    }
    
    private boolean isPhasing(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();
        
        // Check points along the movement path
        for (double d = 0; d < distance; d += 0.1) {
            Location point = from.clone().add(direction.clone().multiply(d));
            if (isInSolidBlock(point.getBlock()) && !isValidPhaseBlock(point.getBlock())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isInBlock(Location location) {
        Block block = location.getBlock();
        if (!block.getType().isSolid()) return false;
        
        BoundingBox playerBox = BoundingBox.of(location, 0.3, 0.9, 0.3);
        BoundingBox blockBox = block.getBoundingBox();
        
        return playerBox.overlaps(blockBox);
    }
    
    private boolean isInSolidBlock(Block block) {
        return block.getType().isSolid() && !block.isLiquid();
    }
    
    private boolean isValidPhaseBlock(Block block) {
        Material type = block.getType();
        return type == Material.LADDER || 
               type == Material.VINE || 
               type == Material.WATER || 
               type == Material.LAVA ||
               type.name().contains("DOOR") ||
               type.name().contains("FENCE") ||
               type.name().contains("GATE") ||
               type.name().contains("WALL") ||
               type.name().contains("CARPET") ||
               type.name().contains("SIGN");
    }
    
    public void cleanup(UUID uuid) {
        lastSafeLocation.remove(uuid);
        violations.remove(uuid);
    }
} 