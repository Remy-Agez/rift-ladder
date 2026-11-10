package com.riftladder.model;

/**
 * Low damage but scales with its own health, and starts every duel with a shield
 * (like Malphite's Granite Shield).
 */
public class Tank extends Champion {

    static final double HEALTH_SCALING = 0.05;
    static final double SHIELD_RATIO = 0.10;

    private double shield;

    public Tank(String name, double maxHealth, double attackDamage, double abilityPower,
                double armor, double magicResist) {
        super(name, maxHealth, attackDamage, abilityPower, armor, magicResist);
        this.shield = maxHealth * SHIELD_RATIO;
    }

    @Override
    public double computeDamage(Champion target) {
        return getAttackDamage() + getMaxHealth() * HEALTH_SCALING;
    }

    /** The shield soaks damage after resistances, the rest goes to health. */
    @Override
    protected double absorb(double damage) {
        double absorbed = Math.min(shield, damage);
        shield -= absorbed;
        return damage - absorbed;
    }

    @Override
    public void resetForDuel() {
        super.resetForDuel();
        shield = getMaxHealth() * SHIELD_RATIO;
    }

    public double getShield() {
        return shield;
    }

    @Override
    public DamageType getDamageType() {
        return DamageType.PHYSICAL;
    }

    @Override
    public String getRole() {
        return "Tank";
    }
}
