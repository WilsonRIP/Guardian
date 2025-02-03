package com.anticheat.guardian.checks.player;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastBreakA extends Check {
    
    private static final double BREAK_TIME_BUFFER = 0.1; // 10% buffer for network latency
    private final Map<UUID, Map<Block, Long>> blockDigStartTimes = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public FastBreakA(Guardian plugin) {
        super(plugin, "FastBreak", "A", CheckCategory.PLAYER, 5.0, false);
    }
    
    public void handleDigStart(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        blockDigStartTimes.computeIfAbsent(uuid, k -> new HashMap<>())
            .put(block, System.currentTimeMillis());
    }
    
    public void handle(Player player, PlayerData data, BlockBreakEvent event) {
        if (player.hasPermission("guardian.bypass") || 
            player.getGameMode() == GameMode.CREATIVE) return;
        
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();
        Map<Block, Long> playerDigTimes = blockDigStartTimes.get(uuid);
        
        if (playerDigTimes == null || !playerDigTimes.containsKey(block)) {
            // No start time recorded - likely started digging before check was active
            return;
        }
        
        long startTime = playerDigTimes.get(block);
        long breakTime = System.currentTimeMillis() - startTime;
        double expectedTime = calculateExpectedBreakTime(player, block);
        
        // Apply buffer to account for network latency
        expectedTime *= (1 - BREAK_TIME_BUFFER);
        
        if (breakTime < expectedTime) {
            int vl = violations.getOrDefault(uuid, 0) + 1;
            double multiplier = Math.min(vl / 5.0, 2.0);
            
            fail(player, data,
                String.format("Block broken too fast: %.1fs (expected: %.1fs)",
                    breakTime / 1000.0, expectedTime / 1000.0),
                multiplier
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
        } else {
            violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
        }
        
        playerDigTimes.remove(block);
    }
    
    private double calculateExpectedBreakTime(Player player, Block block) {
        double baseTime = getBaseBreakTime(block.getType());
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Tool multiplier
        double toolMultiplier = getToolMultiplier(tool, block.getType());
        
        // Efficiency enchantment
        int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
        double efficiencyMultiplier = (efficiencyLevel > 0) ? 
            1.0 + (efficiencyLevel * efficiencyLevel + 1) : 1.0;
        
        // Haste effect
        double hasteMultiplier = 1.0;
        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            int amplifier = player.getPotionEffect(PotionEffectType.HASTE).getAmplifier();
            hasteMultiplier = 1.0 + (0.2 * (amplifier + 1));
        }
        
        // Mining fatigue effect
        double fatigueMultiplier = 1.0;
        if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
            int amplifier = player.getPotionEffect(PotionEffectType.MINING_FATIGUE).getAmplifier();
            fatigueMultiplier = Math.pow(0.3, amplifier + 1);
        }
        
        // Underwater check
        double waterMultiplier = player.isInWater() && 
            !player.hasPotionEffect(PotionEffectType.WATER_BREATHING) ? 5.0 : 1.0;
        
        return baseTime / (toolMultiplier * efficiencyMultiplier * hasteMultiplier * 
            fatigueMultiplier * waterMultiplier);
    }
    
    private double getBaseBreakTime(Material material) {
        // Base breaking times in milliseconds
        switch (material) {
            case DIRT:
            case GRASS_BLOCK:
            case SAND:
            case GRAVEL:
                return 750;
            case STONE:
            case COBBLESTONE:
                return 1500;
            case OBSIDIAN:
                return 9400;
            case DIAMOND_BLOCK:
            case EMERALD_BLOCK:
                return 2500;
            default:
                return 1000;
        }
    }
    
    private double getToolMultiplier(ItemStack tool, Material block) {
        if (tool == null) return 1.0;
        
        Material toolType = tool.getType();
        String blockName = block.name();
        
        // Diamond tools
        if (blockName.contains("STONE") || blockName.contains("ORE")) {
            if (toolType == Material.DIAMOND_PICKAXE) return 8.0;
            if (toolType == Material.IRON_PICKAXE) return 6.0;
            if (toolType == Material.STONE_PICKAXE) return 4.0;
            if (toolType == Material.WOODEN_PICKAXE) return 2.0;
        }
        
        // Shovel for dirt/sand
        if (blockName.contains("DIRT") || blockName.contains("SAND")) {
            if (toolType == Material.DIAMOND_SHOVEL) return 8.0;
            if (toolType == Material.IRON_SHOVEL) return 6.0;
            if (toolType == Material.STONE_SHOVEL) return 4.0;
            if (toolType == Material.WOODEN_SHOVEL) return 2.0;
        }
        
        return 1.0;
    }
    
    public void cleanup(UUID uuid) {
        blockDigStartTimes.remove(uuid);
        violations.remove(uuid);
    }
} 