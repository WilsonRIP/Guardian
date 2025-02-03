package com.anticheat.guardian.checks.player;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryA extends Check {
    
    private static final long MIN_CLICK_DELAY = 50L; // Minimum time between clicks
    private static final int MAX_ACTIONS_PER_SECOND = 15;
    private static final long CHECK_INTERVAL = 1000L; // 1 second
    
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Integer> clickCounter = new HashMap<>();
    private final Map<UUID, Long> lastCounterReset = new HashMap<>();
    
    public InventoryA(Guardian plugin) {
        super(plugin, "Inventory", "A", CheckCategory.PLAYER, 5.0, false);
    }
    
    public void handleClick(Player player, PlayerData data, InventoryClickEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Check click speed
        Long last = lastClickTime.get(uuid);
        if (last != null) {
            long timeDiff = now - last;
            if (timeDiff < MIN_CLICK_DELAY) {
                fail(player, data, 
                    String.format("Click too fast: %dms (min: %dms)", timeDiff, MIN_CLICK_DELAY),
                    1.0
                );
                event.setCancelled(true);
                return;
            }
        }
        
        // Update click counter
        Long lastReset = lastCounterReset.get(uuid);
        if (lastReset == null || now - lastReset > CHECK_INTERVAL) {
            clickCounter.put(uuid, 1);
            lastCounterReset.put(uuid, now);
        } else {
            int clicks = clickCounter.getOrDefault(uuid, 0) + 1;
            if (clicks > MAX_ACTIONS_PER_SECOND) {
                fail(player, data,
                    String.format("Too many inventory actions: %d/s", clicks),
                    Math.min((clicks - MAX_ACTIONS_PER_SECOND) / 5.0, 2.0)
                );
                event.setCancelled(true);
                return;
            }
            clickCounter.put(uuid, clicks);
        }
        
        // Check for invalid item movements
        if (event.getCurrentItem() != null && event.getCursor() != null) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor = event.getCursor();
            
            // Check for impossible stack sizes
            if (current.getAmount() > current.getMaxStackSize() || 
                cursor.getAmount() > cursor.getMaxStackSize()) {
                fail(player, data, "Invalid stack size detected", 2.0);
                event.setCancelled(true);
                return;
            }
            
            // Check for impossible item combinations
            if (isImpossibleCombination(current, cursor)) {
                fail(player, data, "Invalid item combination", 2.0);
                event.setCancelled(true);
                return;
            }
        }
        
        lastClickTime.put(uuid, now);
    }
    
    public void handleDrag(Player player, PlayerData data, InventoryDragEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        // Check for dragging too many items at once
        if (event.getNewItems().size() > 9) { // More than one row at once is suspicious
            fail(player, data, 
                String.format("Dragged too many slots: %d", event.getNewItems().size()),
                1.0
            );
            event.setCancelled(true);
        }
    }
    
    private boolean isImpossibleCombination(ItemStack item1, ItemStack item2) {
        // Different types can't be combined
        if (item1.getType() != item2.getType()) return true;
        
        // Check for items that can't be stacked
        if (!item1.getType().isBlock() && !item1.getType().isItem()) return true;
        
        // Check for items with different metadata/durability
        if (item1.getDurability() != item2.getDurability()) return true;
        
        return false;
    }
    
    public void cleanup(UUID uuid) {
        lastClickTime.remove(uuid);
        clickCounter.remove(uuid);
        lastCounterReset.remove(uuid);
    }
} 