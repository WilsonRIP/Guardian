package com.anticheat.guardian.checks.combat;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ReachA extends Check {
    
    private static final double MAX_REACH = 3.1; // Vanilla reach is ~3.0 blocks
    private static final double CREATIVE_REACH = 4.5; // Creative mode has longer reach
    private static final double HITBOX_EXPANSION = 0.1; // Compensation for latency
    private static final double MAX_PING_COMPENSATION = 0.4; // Maximum reach addition for high ping
    
    public ReachA(Guardian plugin) {
        super(plugin, "Reach", "A", CheckCategory.COMBAT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) return;
        
        // Get base reach limit
        double maxReach = player.getGameMode() == GameMode.CREATIVE ? CREATIVE_REACH : MAX_REACH;
        
        // Get eye locations and target hitbox
        Location eyeLocation = player.getEyeLocation();
        BoundingBox targetBox = target.getBoundingBox().expand(HITBOX_EXPANSION);
        
        // Calculate minimum reach distance using raytrace
        double distance = calculateMinimumDistance(eyeLocation, targetBox);
        
        // Apply ping compensation (20 ticks = 1 second, assume 0.1 blocks per tick of latency)
        double pingCompensation = Math.min(player.getPing() / 200.0, MAX_PING_COMPENSATION);
        maxReach += pingCompensation;
        
        // Check if reach exceeds limit
        if (distance > maxReach) {
            fail(player, data,
                String.format("Reach: %.2f > %.2f (ping comp: %.2f)", 
                distance, maxReach - pingCompensation, pingCompensation),
                Math.min(distance - maxReach, 2.0)
            );
            event.setCancelled(true);
            return;
        }
        
        // Update reach statistics
        data.setLastReachDistance(distance);
    }
    
    private double calculateMinimumDistance(Location eyeLocation, BoundingBox targetBox) {
        Vector eyePos = eyeLocation.toVector();
        Vector direction = eyeLocation.getDirection();
        
        // Check each corner of the hitbox
        double minDistance = Double.MAX_VALUE;
        
        double[][] corners = {
            {targetBox.getMinX(), targetBox.getMinY(), targetBox.getMinZ()},
            {targetBox.getMinX(), targetBox.getMinY(), targetBox.getMaxZ()},
            {targetBox.getMinX(), targetBox.getMaxY(), targetBox.getMinZ()},
            {targetBox.getMinX(), targetBox.getMaxY(), targetBox.getMaxZ()},
            {targetBox.getMaxX(), targetBox.getMinY(), targetBox.getMinZ()},
            {targetBox.getMaxX(), targetBox.getMinY(), targetBox.getMaxZ()},
            {targetBox.getMaxX(), targetBox.getMaxY(), targetBox.getMinZ()},
            {targetBox.getMaxX(), targetBox.getMaxY(), targetBox.getMaxZ()}
        };
        
        for (double[] corner : corners) {
            Vector cornerVec = new Vector(corner[0], corner[1], corner[2]);
            Vector toCorner = cornerVec.subtract(eyePos);
            
            // Project the corner vector onto the look direction
            double dot = toCorner.dot(direction);
            Vector projected = direction.clone().multiply(dot);
            
            // Calculate the minimum distance to this corner
            double distance = toCorner.subtract(projected).length();
            minDistance = Math.min(minDistance, distance);
        }
        
        return minDistance;
    }
} 