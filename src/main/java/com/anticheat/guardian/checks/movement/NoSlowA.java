package com.anticheat.guardian.checks.movement;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoSlowA extends Check {
    
    private static final double SNEAK_MULTIPLIER = 0.3;
    private static final double USING_ITEM_MULTIPLIER = 0.5;
    private static final double SOUL_SAND_MULTIPLIER = 0.4;
    private static final double HONEY_MULTIPLIER = 0.4;
    private static final double SWEET_BERRY_MULTIPLIER = 0.7;
    private static final double COBWEB_MULTIPLIER = 0.05;
    
    private final Map<UUID, Integer> violations = new HashMap<>();
    
    public NoSlowA(Guardian plugin) {
        super(plugin, "NoSlow", "A", CheckCategory.MOVEMENT, 5.0, false);
    }
    
    public void handle(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) return;
        
        // Skip if player is flying or using elytra
        if (player.isFlying() || player.isGliding()) return;
        
        Vector movement = event.getTo().toVector().subtract(event.getFrom().toVector());
        double horizontalSpeed = Math.sqrt(movement.getX() * movement.getX() + 
                                         movement.getZ() * movement.getZ());
        
        // Get base speed
        double maxSpeed = 0.2873; // Base walking speed
        
        // Apply relevant multipliers
        double multiplier = 1.0;
        
        // Check item usage
        ItemStack usingItem = player.getItemInUse();
        if (usingItem != null && isSlowingItem(usingItem.getType())) {
            multiplier *= USING_ITEM_MULTIPLIER;
        }
        
        // Check sneaking
        if (player.isSneaking()) {
            multiplier *= SNEAK_MULTIPLIER;
        }
        
        // Check block effects
        if (isOnSlowingBlock(player)) {
            multiplier *= getBlockSlowMultiplier(player);
        }
        
        // Check potion effects
        if (player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            int level = player.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier() + 1;
            multiplier *= (1.0 - (0.15 * level));
        }
        
        // Apply sprint modifier if sprinting
        if (player.isSprinting()) {
            maxSpeed *= 1.3;
        }
        
        // Calculate final maximum allowed speed
        double maxAllowedSpeed = maxSpeed * multiplier;
        
        // Add small buffer for server lag
        maxAllowedSpeed *= 1.1;
        
        // Check if speed exceeds maximum allowed
        if (horizontalSpeed > maxAllowedSpeed) {
            UUID uuid = player.getUniqueId();
            int vl = violations.getOrDefault(uuid, 0) + 1;
            
            fail(player, data,
                String.format("NoSlow violation: %.3f > %.3f (multiplier: %.2f)", 
                    horizontalSpeed, maxAllowedSpeed, multiplier),
                Math.min(vl / 2.0, 2.0)
            );
            
            event.setCancelled(true);
            violations.put(uuid, vl);
        }
    }
    
    private boolean isSlowingItem(Material material) {
        return material == Material.BOW || 
               material == Material.CROSSBOW ||
               material == Material.TRIDENT ||
               material == Material.SHIELD ||
               material.name().contains("SWORD") ||
               material.name().contains("AXE") ||
               material.isEdible();
    }
    
    private boolean isOnSlowingBlock(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        Material belowType = player.getLocation().subtract(0, 0.1, 0).getBlock().getType();
        
        return blockType == Material.COBWEB ||
               blockType == Material.SWEET_BERRY_BUSH ||
               blockType == Material.HONEY_BLOCK ||
               belowType == Material.SOUL_SAND ||
               belowType == Material.HONEY_BLOCK;
    }
    
    private double getBlockSlowMultiplier(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        Material belowType = player.getLocation().subtract(0, 0.1, 0).getBlock().getType();
        
        if (blockType == Material.COBWEB) return COBWEB_MULTIPLIER;
        if (blockType == Material.SWEET_BERRY_BUSH) return SWEET_BERRY_MULTIPLIER;
        if (blockType == Material.HONEY_BLOCK || belowType == Material.HONEY_BLOCK) 
            return HONEY_MULTIPLIER;
        if (belowType == Material.SOUL_SAND) return SOUL_SAND_MULTIPLIER;
        
        return 1.0;
    }
    
    public void cleanup(UUID uuid) {
        violations.remove(uuid);
    }
} 