package com.anticheat.guardian.managers;

import com.anticheat.guardian.checks.combat.AutoClickerA;
import com.anticheat.guardian.checks.combat.CriticalsA;
import com.anticheat.guardian.checks.combat.FastBowA;
import com.anticheat.guardian.checks.combat.KillAuraA;
import com.anticheat.guardian.checks.combat.ReachA;
import com.anticheat.guardian.checks.movement.FlightA;
import com.anticheat.guardian.checks.movement.GlideA;
import com.anticheat.guardian.checks.movement.JesusA;
import com.anticheat.guardian.checks.movement.NoFallA;
import com.anticheat.guardian.checks.movement.NoSlowA;
import com.anticheat.guardian.checks.movement.PhaseA;
import com.anticheat.guardian.checks.movement.SpeedA;
import com.anticheat.guardian.checks.movement.SpiderA;
import com.anticheat.guardian.checks.movement.StrafeA;
import com.anticheat.guardian.checks.movement.VelocityA;
import com.anticheat.guardian.checks.movement.FastLadderA;
import com.anticheat.guardian.checks.player.BadPacketsA;
import com.anticheat.guardian.checks.player.FastBreakA;
import com.anticheat.guardian.checks.player.InventoryA;
import com.anticheat.guardian.checks.player.ScaffoldA;
import com.anticheat.guardian.checks.player.TimerA;
import com.anticheat.guardian.checks.player.RegenA;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckManager {
    
    private final List<KillAuraA> combatChecks = new ArrayList<>();
    private final List<ReachA> reachChecks = new ArrayList<>();
    private final List<Object> movementChecks = new ArrayList<>();
    private final List<TimerA> timerChecks = new ArrayList<>();
    private final List<InventoryA> inventoryChecks = new ArrayList<>();
    private final List<ScaffoldA> scaffoldChecks = new ArrayList<>();
    private final List<FastBreakA> fastBreakChecks = new ArrayList<>();
    private final List<AutoClickerA> autoClickerChecks = new ArrayList<>();
    private final List<JesusA> jesusChecks = new ArrayList<>();
    private final List<BadPacketsA> badPacketsChecks = new ArrayList<>();
    private final List<PhaseA> phaseChecks = new ArrayList<>();
    private final List<VelocityA> velocityChecks = new ArrayList<>();
    private final List<CriticalsA> criticalsChecks = new ArrayList<>();
    private final List<SpiderA> spiderChecks = new ArrayList<>();
    private final List<StrafeA> strafeChecks = new ArrayList<>();
    private final List<FastLadderA> fastLadderChecks = new ArrayList<>();
    private final List<FastBowA> fastBowChecks = new ArrayList<>();
    private final List<GlideA> glideChecks = new ArrayList<>();
    private final List<NoSlowA> noSlowChecks = new ArrayList<>();
    private final List<RegenA> regenChecks = new ArrayList<>();
    
    public CheckManager() {
        // Register combat checks
        combatChecks.add(new KillAuraA(null));
        reachChecks.add(new ReachA(null));
        autoClickerChecks.add(new AutoClickerA(null));
        criticalsChecks.add(new CriticalsA(null));
        fastBowChecks.add(new FastBowA(null));
        
        // Register movement checks
        movementChecks.add(new SpeedA(null));
        movementChecks.add(new FlightA(null));
        movementChecks.add(new NoFallA(null));
        jesusChecks.add(new JesusA(null));
        phaseChecks.add(new PhaseA(null));
        velocityChecks.add(new VelocityA(null));
        spiderChecks.add(new SpiderA(null));
        strafeChecks.add(new StrafeA(null));
        fastLadderChecks.add(new FastLadderA(null));
        glideChecks.add(new GlideA(null));
        noSlowChecks.add(new NoSlowA(null));
        
        // Register player checks
        timerChecks.add(new TimerA(null));
        inventoryChecks.add(new InventoryA(null));
        scaffoldChecks.add(new ScaffoldA(null));
        fastBreakChecks.add(new FastBreakA(null));
        badPacketsChecks.add(new BadPacketsA(null));
        regenChecks.add(new RegenA(null));
    }
    
    public void runMovementChecks(Player player, PlayerData data, PlayerMoveEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (Object check : movementChecks) {
            if (check instanceof SpeedA) {
                ((SpeedA) check).handle(player, data, event);
            } else if (check instanceof FlightA) {
                ((FlightA) check).handle(player, data, event);
            } else if (check instanceof NoFallA) {
                ((NoFallA) check).handle(player, data, event);
            }
        }
        
        // Run timer checks on movement
        for (TimerA check : timerChecks) {
            check.handle(player, data, event);
        }
        
        // Run Jesus checks on movement
        for (JesusA check : jesusChecks) {
            check.handle(player, data, event);
        }
        
        // Run BadPackets checks on movement
        for (BadPacketsA check : badPacketsChecks) {
            check.handle(player, data, event);
        }
        
        // Run Phase checks on movement
        for (PhaseA check : phaseChecks) {
            check.handle(player, data, event);
        }
        
        // Run Velocity checks on movement
        for (VelocityA check : velocityChecks) {
            check.handle(player, data, event);
        }
        
        // Run Spider checks on movement
        for (SpiderA check : spiderChecks) {
            check.handle(player, data, event);
        }
        
        // Run Strafe checks on movement
        for (StrafeA check : strafeChecks) {
            check.handle(player, data, event);
        }
        
        // Run FastLadder checks on movement
        for (FastLadderA check : fastLadderChecks) {
            check.handle(player, data, event);
        }
        
        // Run Glide checks on movement
        for (GlideA check : glideChecks) {
            check.handle(player, data, event);
        }
        
        // Run NoSlow checks on movement
        for (NoSlowA check : noSlowChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void runCombatChecks(Player player, PlayerData data, EntityDamageByEntityEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (KillAuraA check : combatChecks) {
            check.handle(player, data, event);
        }
        
        for (ReachA check : reachChecks) {
            check.handle(player, data, event);
        }
        
        for (CriticalsA check : criticalsChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void runBowChecks(Player player, PlayerData data, EntityShootBowEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (FastBowA check : fastBowChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void runBowStartChecks(Player player) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (FastBowA check : fastBowChecks) {
            check.handleBowStart(player);
        }
    }
    
    public void runVelocityChecks(Player player, PlayerData data, PlayerVelocityEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (VelocityA check : velocityChecks) {
            check.handleVelocity(player, data, event);
        }
    }
    
    public void runInventoryChecks(Player player, PlayerData data, InventoryClickEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (InventoryA check : inventoryChecks) {
            check.handleClick(player, data, event);
        }
    }
    
    public void runInventoryDragChecks(Player player, PlayerData data, InventoryDragEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (InventoryA check : inventoryChecks) {
            check.handleDrag(player, data, event);
        }
    }
    
    public void runScaffoldChecks(Player player, PlayerData data, BlockPlaceEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (ScaffoldA check : scaffoldChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void runFastBreakChecks(Player player, PlayerData data, BlockBreakEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (FastBreakA check : fastBreakChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void runAutoClickerChecks(Player player, PlayerData data, PlayerInteractEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (AutoClickerA check : autoClickerChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void runRegenChecks(Player player, PlayerData data, EntityRegainHealthEvent event) {
        if (player.hasPermission("guardian.bypass")) {
            return;
        }
        
        for (RegenA check : regenChecks) {
            check.handle(player, data, event);
        }
    }
    
    public void cleanup(UUID uuid) {
        // Clean up any check-specific data
        for (TimerA check : timerChecks) {
            check.cleanup(uuid);
        }
        for (InventoryA check : inventoryChecks) {
            check.cleanup(uuid);
        }
        for (ScaffoldA check : scaffoldChecks) {
            check.cleanup(uuid);
        }
        for (FastBreakA check : fastBreakChecks) {
            check.cleanup(uuid);
        }
        for (AutoClickerA check : autoClickerChecks) {
            check.cleanup(uuid);
        }
        for (JesusA check : jesusChecks) {
            check.cleanup(uuid);
        }
        for (BadPacketsA check : badPacketsChecks) {
            check.cleanup(uuid);
        }
        for (PhaseA check : phaseChecks) {
            check.cleanup(uuid);
        }
        for (VelocityA check : velocityChecks) {
            check.cleanup(uuid);
        }
        for (CriticalsA check : criticalsChecks) {
            check.cleanup(uuid);
        }
        for (SpiderA check : spiderChecks) {
            check.cleanup(uuid);
        }
        for (StrafeA check : strafeChecks) {
            check.cleanup(uuid);
        }
        for (FastLadderA check : fastLadderChecks) {
            check.cleanup(uuid);
        }
        for (FastBowA check : fastBowChecks) {
            check.cleanup(uuid);
        }
        for (GlideA check : glideChecks) {
            check.cleanup(uuid);
        }
        for (NoSlowA check : noSlowChecks) {
            check.cleanup(uuid);
        }
        for (RegenA check : regenChecks) {
            check.cleanup(uuid);
        }
    }
} 