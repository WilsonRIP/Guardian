package com.anticheat.guardian.checks.combat;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class AutoClickerA extends Check {
    
    private static final int MAX_CPS = 20; // Maximum clicks per second
    private static final int SAMPLE_SIZE = 20; // Number of clicks to analyze
    private static final double MAX_DEVIATION = 0.5; // Maximum allowed standard deviation
    private static final long MIN_CLICK_DELAY = 50L; // Minimum time between clicks (ms)
    
    private final Map<UUID, LinkedList<Long>> clickTimes = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public AutoClickerA(Guardian plugin) {
        super(plugin, "AutoClicker", "A", CheckCategory.COMBAT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerInteractEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        // Only check left clicks
        if (event.getAction() != Action.LEFT_CLICK_AIR && 
            event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Check minimum delay between clicks
        Long lastClick = lastClickTime.get(uuid);
        if (lastClick != null) {
            long timeDiff = now - lastClick;
            if (timeDiff < MIN_CLICK_DELAY) {
                fail(player, data,
                    String.format("Clicking too fast: %dms (min: %dms)", 
                        timeDiff, MIN_CLICK_DELAY),
                    1.0
                );
                event.setCancelled(true);
                return;
            }
        }
        
        // Update click history
        LinkedList<Long> times = clickTimes.computeIfAbsent(uuid, k -> new LinkedList<>());
        times.add(now);
        
        // Keep only the last SAMPLE_SIZE clicks
        while (times.size() > SAMPLE_SIZE) {
            times.removeFirst();
        }
        
        // Only analyze if we have enough samples
        if (times.size() >= SAMPLE_SIZE) {
            analyzeClickPattern(player, data, times);
        }
        
        lastClickTime.put(uuid, now);
    }
    
    private void analyzeClickPattern(Player player, PlayerData data, LinkedList<Long> times) {
        UUID uuid = player.getUniqueId();
        
        // Calculate CPS
        long timeSpan = times.getLast() - times.getFirst();
        double cps = (times.size() - 1) * 1000.0 / timeSpan;
        
        if (cps > MAX_CPS) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            fail(player, data,
                String.format("CPS too high: %.1f (max: %d)", cps, MAX_CPS),
                Math.min(vl / 5.0, 2.0)
            );
            violations.put(uuid, vl);
            return;
        }
        
        // Calculate click intervals
        List<Long> intervals = new ArrayList<>();
        Iterator<Long> iterator = times.iterator();
        Long previous = iterator.next();
        
        while (iterator.hasNext()) {
            Long current = iterator.next();
            intervals.add(current - previous);
            previous = current;
        }
        
        // Calculate standard deviation of intervals
        double mean = intervals.stream().mapToLong(Long::valueOf).average().orElse(0);
        double variance = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - mean, 2))
            .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // Check for suspicious consistency
        if (stdDev < mean * MAX_DEVIATION) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            fail(player, data,
                String.format("Click pattern too consistent: %.2fms deviation", stdDev),
                Math.min(vl / 5.0, 2.0)
            );
            violations.put(uuid, vl);
        } else {
            // Decrease violations on legitimate clicks
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
    }
    
    public void cleanup(UUID uuid) {
        clickTimes.remove(uuid);
        lastClickTime.remove(uuid);
        violations.remove(uuid);
    }
} 