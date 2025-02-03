package com.anticheat.guardian.checks.player;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScaffoldA extends Check {
    
    private static final long MIN_PLACE_DELAY = 50L; // Minimum time between block placements
    private static final double MAX_ANGLE = 90.0; // Maximum angle for legitimate block placement
    
    private final Map<UUID, Long> lastPlaceTime = new HashMap<>();
    private final Map<UUID, Location> lastPlaceLocation = new HashMap<>();
    private final Map<UUID, Integer> placementStreak = new HashMap<>();
    
    public ScaffoldA(Guardian plugin) {
        super(plugin, "Scaffold", "A", CheckCategory.PLAYER, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, BlockPlaceEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Block block = event.getBlock();
        
        // Check placement speed
        Long lastPlace = lastPlaceTime.get(uuid);
        if (lastPlace != null) {
            long timeDiff = now - lastPlace;
            if (timeDiff < MIN_PLACE_DELAY) {
                fail(player, data,
                    String.format("Block placed too fast: %dms", timeDiff),
                    1.0
                );
                event.setCancelled(true);
                return;
            }
        }
        
        // Check placement angle
        Vector playerDirection = player.getLocation().getDirection();
        Vector toBlock = block.getLocation().toVector()
            .subtract(player.getEyeLocation().toVector()).normalize();
        double angle = Math.toDegrees(Math.acos(playerDirection.dot(toBlock)));
        
        if (angle > MAX_ANGLE) {
            fail(player, data,
                String.format("Invalid placement angle: %.1f°", angle),
                1.0
            );
            event.setCancelled(true);
            return;
        }
        
        // Check placement patterns
        Location lastLoc = lastPlaceLocation.get(uuid);
        if (lastLoc != null) {
            // Check for impossible bridging
            if (isImpossibleBridging(player, block, lastLoc)) {
                fail(player, data, "Impossible bridging pattern", 1.0);
                event.setCancelled(true);
                return;
            }
            
            // Check placement streak
            if (isConsistentPlacement(player, block, lastLoc)) {
                int streak = placementStreak.getOrDefault(uuid, 0) + 1;
                if (streak > 10) { // More than 10 perfect placements is suspicious
                    fail(player, data,
                        String.format("Too consistent placement pattern: %d blocks", streak),
                        Math.min(streak / 10.0, 2.0)
                    );
                    event.setCancelled(true);
                    placementStreak.put(uuid, 0);
                    return;
                }
                placementStreak.put(uuid, streak);
            } else {
                placementStreak.put(uuid, 0);
            }
        }
        
        // Update tracking data
        lastPlaceTime.put(uuid, now);
        lastPlaceLocation.put(uuid, block.getLocation());
    }
    
    private boolean isImpossibleBridging(Player player, Block current, Location last) {
        // Check if player is bridging outward
        if (current.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
            // Calculate horizontal distance from player to block
            double distance = Math.sqrt(
                Math.pow(current.getX() - player.getLocation().getX(), 2) +
                Math.pow(current.getZ() - player.getLocation().getZ(), 2)
            );
            
            // Player must be close enough to place the block
            if (distance > 2.5) {
                return true;
            }
            
            // Check if player is shifting (sneaking)
            if (!player.isSneaking()) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isConsistentPlacement(Player player, Block current, Location last) {
        // Check if blocks are placed in a perfect line
        double deltaX = Math.abs(current.getX() - last.getX());
        double deltaY = Math.abs(current.getY() - last.getY());
        double deltaZ = Math.abs(current.getZ() - last.getZ());
        
        // Perfect straight line placement
        return (deltaX == 1 && deltaY == 0 && deltaZ == 0) ||
               (deltaX == 0 && deltaY == 0 && deltaZ == 1);
    }
    
    public void cleanup(UUID uuid) {
        lastPlaceTime.remove(uuid);
        lastPlaceLocation.remove(uuid);
        placementStreak.remove(uuid);
    }
} 