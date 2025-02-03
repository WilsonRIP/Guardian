package com.anticheat.guardian.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
@Setter
public class PlayerData {
    private final Player player;
    
    // Movement data
    private Location lastLocation;
    private Location lastGroundLocation;
    private boolean onGround;
    private long lastGroundTime;
    private double lastDeltaY;
    
    // Combat data
    private long lastAttackTime;
    private int hitCount;
    private double lastReachDistance;
    private int consecutiveHits;
    
    // Violation data
    private int movementVL;
    private int combatVL;
    
    public PlayerData(Player player) {
        this.player = player;
        this.lastLocation = player.getLocation();
        this.lastGroundLocation = player.getLocation();
        this.onGround = player.getLocation().getBlock().getRelative(0, -1, 0).getType().isSolid();
        this.lastGroundTime = System.currentTimeMillis();
        this.hitCount = 0;
        this.consecutiveHits = 0;
    }
    
    public void resetViolations() {
        this.movementVL = 0;
        this.combatVL = 0;
    }
    
    public void incrementMovementVL() {
        this.movementVL++;
    }
    
    public void incrementCombatVL() {
        this.combatVL++;
    }

    public void incrementHitCount() {
        this.hitCount++;
        this.consecutiveHits++;
    }

    public void resetHitCount() {
        this.hitCount = 0;
        this.consecutiveHits = 0;
    }
} 