package com.anticheat.guardian.checks.player;

import com.anticheat.guardian.Guardian;
import com.anticheat.guardian.checks.Check;
import com.anticheat.guardian.checks.CheckCategory;
import com.anticheat.guardian.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.RegainReason;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegenA extends Check {

    private static final long REGEN_THRESHOLD_TIME = 1000L; // 1 second threshold for regen
    private final Map<UUID, Long> lastRegenTime = new HashMap<>();

    public RegenA(Guardian plugin) {
        super(plugin, "Regen", "A", CheckCategory.PLAYER, 5.0, false);
    }

    public void handle(Player player, PlayerData data, EntityRegainHealthEvent event) {
        if (player.hasPermission("guardian.bypass")) return;

        if (event.getRegainReason() != RegainReason.SATIATED && event.getRegainReason() != RegainReason.REGEN) {
            return; // Only check natural regeneration and beacon/potion regen
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastRegenTime.containsKey(uuid)) {
            long timeDiff = now - lastRegenTime.get(uuid);
            if (timeDiff < REGEN_THRESHOLD_TIME) {
                int vl = data.getCombatVL(); // Or player VL, depending on how you categorize regen
                fail(player, data,
                        String.format("Regenerating too fast: %dms (threshold: %dms)", timeDiff, REGEN_THRESHOLD_TIME),
                        Math.min(vl + 1, maxVL) // Increment VL, up to maxVL
                );
                event.setCancelled(true); // Optionally cancel the regen
            }
        }
        lastRegenTime.put(uuid, now);
    }

    public void cleanup(UUID uuid) {
        lastRegenTime.remove(uuid);
    }
} 