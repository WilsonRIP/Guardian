package com.anticheat.guardian.checks.player;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimerA extends Check {
    
    private static final int SAMPLE_SIZE = 40;
    private static final double MAX_SPEED = 1.03; // Allow 3% variance
    private static final double MIN_SPEED = 0.97;
    private static final long TICK_TIME = 50; // Minecraft runs at 20 TPS (50ms per tick)
    
    private final Map<UUID, Deque<Long>> moveTimestamps = new HashMap<>();
    
    public TimerA(Guardian plugin) {
        super(plugin, "Timer", "A", CheckCategory.PLAYER, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Initialize or get the player's timestamp queue
        Deque<Long> timestamps = moveTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        
        // Add current timestamp
        timestamps.add(now);
        
        // Keep only the last SAMPLE_SIZE timestamps
        while (timestamps.size() > SAMPLE_SIZE) {
            timestamps.poll();
        }
        
        // Need enough samples to check
        if (timestamps.size() >= SAMPLE_SIZE) {
            // Calculate average time between moves
            long timeSpan = now - timestamps.peek();
            double averageTime = (double) timeSpan / (timestamps.size() - 1);
            
            // Calculate speed multiplier (how fast they're moving compared to normal)
            double speedMultiplier = TICK_TIME / averageTime;
            
            // Check if speed is outside acceptable range
            if (speedMultiplier > MAX_SPEED || speedMultiplier < MIN_SPEED) {
                fail(player, data,
                    String.format("Timer speed: %.2fx (%.2f ms)", 
                    speedMultiplier, averageTime),
                    Math.min(Math.abs(1 - speedMultiplier) * 2, 2.0)
                );
                
                // Reset their timestamps
                timestamps.clear();
                
                // Cancel the event if speed is significantly off
                if (speedMultiplier > MAX_SPEED * 1.5 || speedMultiplier < MIN_SPEED * 0.5) {
                    event.setCancelled(true);
                }
            }
            
            // Debug info
            debug(player, String.format("Timer speed: %.2fx (%.2f ms)", speedMultiplier, averageTime));
        }
    }
    
    public void cleanup(UUID uuid) {
        moveTimestamps.remove(uuid);
    }
} 