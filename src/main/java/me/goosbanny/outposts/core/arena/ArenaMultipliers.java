package me.goosbanny.outposts.core.arena;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the multiplier values associated with holding an outpost.
 */
public class ArenaMultipliers {

    public static final String SPAWNER_RATE = "spawner_rate";
    public static final String MOB_DROP_RATE = "mob_drop_rate";
    public static final String EXP_DROP_RATE = "exp_drop_rate";
    public static final String DAMAGE_RATE = "damage_rate";
    public static final String SHOPGUI_SELL_RATE = "shopgui_sell_rate";

    private final Map<String, Double> multipliers;

    public ArenaMultipliers(@NotNull Map<String, Double> multipliers) {
        this.multipliers = Collections.unmodifiableMap(new HashMap<>(multipliers));
    }

    public static ArenaMultipliers createDefault() {
        Map<String, Double> map = new HashMap<>();
        map.put(SPAWNER_RATE, 1.5);
        map.put(MOB_DROP_RATE, 1.75);
        map.put(EXP_DROP_RATE, 2.0);
        map.put(DAMAGE_RATE, 1.10);
        map.put(SHOPGUI_SELL_RATE, 1.35);
        return new ArenaMultipliers(map);
    }

    public double getMultiplier(@NotNull String key) {
        return multipliers.getOrDefault(key.toLowerCase(), 1.0);
    }

    public Map<String, Double> asMap() {
        return multipliers;
    }
}
