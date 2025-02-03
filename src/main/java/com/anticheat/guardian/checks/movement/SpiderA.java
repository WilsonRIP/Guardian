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

public class SpiderA extends Check {
    
    private static final double MAX_CLIMB_SPEED = 0.2; // Maximum vertical climbing speed
    private static final double MIN_WALL_DISTANCE = 0.3; // Minimum distance to consider wall climbing
    private static final int MAX_CLIMB_TIME = 40; // Maximum ticks allowed for climbing
    
    private final Map<UUID, Integer> climbTime = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public SpiderA(Guardian plugin) {
        super(plugin, "Spider", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        UUID uuid = player.getUniqueId();
        
        // Skip if player is using legitimate climbing methods
        if (isLegitimateClimbing(player)) {
            resetData(uuid);
            return;
        }
        
        // Check vertical movement
        double deltaY = to.getY() - from.getY();
        if (deltaY > 0) {
            // Check if player is near a wall
            if (isNearWall(to)) {
                int time = climbTime.getOrDefault(uuid, 0) + 1;
                
                // Check climbing speed and duration
                if (deltaY > MAX_CLIMB_SPEED || time > MAX_CLIMB_TIME) {
                    int vl = violations.getOrDefault(uuid, 0) + 1;
                    
                    fail(player, data,
                        String.format("Illegal climbing: %.2f blocks/tick (%d ticks)", 
                            deltaY, time),
                        Math.min(vl / 2.0, 2.0)
                    );
                    
                    event.setCancelled(true);
                    violations.put(uuid, vl);
                    climbTime.put(uuid, 0);
                    return;
                }
                
                climbTime.put(uuid, time);
            } else {
                resetData(uuid);
            }
        } else {
            // Decrease climb time when not moving up
            climbTime.put(uuid, Math.max(0, climbTime.getOrDefault(uuid, 0) - 1));
        }
        
        // Decrease violations over time
        if (Math.random() < 0.1) { // 10% chance per tick
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
    }
    
    private boolean isLegitimateClimbing(Player player) {
        Location loc = player.getLocation();
        Block block = loc.getBlock();
        
        // Check for legitimate climbing methods
        return player.isFlying() ||
               player.isGliding() ||
               block.getType() == Material.LADDER ||
               block.getType() == Material.VINE ||
               block.getType() == Material.SCAFFOLDING ||
               block.getType() == Material.TWISTING_VINES ||
               block.getType() == Material.WEEPING_VINES ||
               player.hasPotionEffect(PotionEffectType.LEVITATION);
    }
    
    private boolean isNearWall(Location location) {
        // Check surrounding blocks for walls
        Block block = location.getBlock();
        
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, 
                                             BlockFace.EAST, BlockFace.WEST}) {
            Block relative = block.getRelative(face);
            if (isClimbableWall(relative)) {
                // Calculate distance to wall
                Vector wallCenter = relative.getLocation().add(0.5, 0.5, 0.5).toVector();
                Vector playerPos = location.toVector();
                double distance = wallCenter.subtract(playerPos).length();
                
                if (distance <= MIN_WALL_DISTANCE) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isClimbableWall(Block block) {
        return block.getType().isSolid() && 
               !block.isLiquid() &&
               block.getType() != Material.LADDER &&
               block.getType() != Material.VINE &&
               block.getType() != Material.SCAFFOLDING &&
               !block.getType().name().contains("STAIRS") &&
               !block.getType().name().contains("SLAB");
    }
    
    private void resetData(UUID uuid) {
        climbTime.remove(uuid);
    }
    
    public void cleanup(UUID uuid) {
        climbTime.remove(uuid);
        violations.remove(uuid);
    }
} 