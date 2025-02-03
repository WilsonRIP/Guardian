package com.anticheat.guardian.managers;

import com.anticheat.guardian.data.PlayerData;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class PlayerDataManager {
    
    // Kept for future functionality (config handling, logging, etc.)
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    
    public void createData(Player player) {
        playerDataMap.put(player.getUniqueId(), new PlayerData(player));
    }
    
    public void removeData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }
    
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }
    
    public void cleanup() {
        playerDataMap.clear();
    }

    public int getActivePlayerCount() {
        return playerDataMap.size();
    }

    public void cleanupAll() {
        playerDataMap.clear();
    }
} 