package com.anticheat.guardian;

import com.anticheat.guardian.data.PlayerData;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

@RequiredArgsConstructor
public class PlayerListener implements Listener {
    
    private final Guardian plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().createData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().removeData(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        
        if (data != null) {
            // Trigger movement-related checks
            plugin.getCheckManager().runMovementChecks(player, data, event);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
            
            if (data != null) {
                // Trigger combat-related checks
                plugin.getCheckManager().runCombatChecks(player, data, event);
            }
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

            if (data != null) {
                // Trigger regen checks
                plugin.getCheckManager().runRegenChecks(player, data, event);
            }
        }
    }
} 