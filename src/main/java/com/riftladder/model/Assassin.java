package com.riftladder.model;

/** Physical burst. Deals bonus damage to a target that is already low (think Katarina's resets). */
public class Assassin extends Champion {

    /** Below this share of max health the target counts as "low". */
    static final double EXECUTE_THRESHOLD = 0.3;
    static final double EXECUTE_MULTIPLIER = 1.5;

    public Assassin(String name, double maxHealth, double attackDamage, double abilityPower,
                    double armor, double magicResist) {
        super(name, maxHealth, attackDamage, abilityPower, armor, magicResist);
    }

    @Override
    public double computeDamage(Champion target) {
        double damage = getAttackDamage();
        if (target.getHealthRatio() < EXECUTE_THRESHOLD) {
            damage *= EXECUTE_MULTIPLIER;
        }
        return damage;
    }

    @Override
    public DamageType getDamageType() {
        return DamageType.PHYSICAL;
    }

    @Override
    public String getRole() {
        return "Assassin";
    }
}
