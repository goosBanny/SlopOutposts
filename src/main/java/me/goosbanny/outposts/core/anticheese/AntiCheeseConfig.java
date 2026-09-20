package me.goosbanny.outposts.core.anticheese;

/**
 * Configuration parameters for anti-cheese capture validation.
 */
public class AntiCheeseConfig {

    private final boolean lineOfSight;
    private final CombatDamagePolicy combatDamagePolicy;
    private final int combatPauseSeconds;
    private final boolean disallowGodmode;
    private final boolean disallowFlying;
    private final boolean disallowElytra;
    private final boolean disallowVanished;
    private final boolean disallowAlliedStall;

    public AntiCheeseConfig(
            boolean lineOfSight,
            CombatDamagePolicy combatDamagePolicy,
            int combatPauseSeconds,
            boolean disallowGodmode,
            boolean disallowFlying,
            boolean disallowElytra,
            boolean disallowVanished,
            boolean disallowAlliedStall
    ) {
        this.lineOfSight = lineOfSight;
        this.combatDamagePolicy = combatDamagePolicy != null ? combatDamagePolicy : CombatDamagePolicy.PAUSE_CAPTURE;
        this.combatPauseSeconds = Math.max(0, combatPauseSeconds);
        this.disallowGodmode = disallowGodmode;
        this.disallowFlying = disallowFlying;
        this.disallowElytra = disallowElytra;
        this.disallowVanished = disallowVanished;
        this.disallowAlliedStall = disallowAlliedStall;
    }

    public static AntiCheeseConfig createDefault() {
        return new AntiCheeseConfig(
                true,
                CombatDamagePolicy.PAUSE_CAPTURE,
                5,
                true,
                true,
                true,
                true,
                true
        );
    }

    public boolean isLineOfSight() { return lineOfSight; }
    public CombatDamagePolicy getCombatDamagePolicy() { return combatDamagePolicy; }
    public int getCombatPauseSeconds() { return combatPauseSeconds; }
    public boolean isDisallowGodmode() { return disallowGodmode; }
    public boolean isDisallowFlying() { return disallowFlying; }
    public boolean isDisallowElytra() { return disallowElytra; }
    public boolean isDisallowVanished() { return disallowVanished; }
    public boolean isDisallowAlliedStall() { return disallowAlliedStall; }
}
