package com.riftladder.model;

/**
 * Common base of every champion: stats, health and how incoming damage is reduced.
 * Abstract because a "plain" champion does not exist: each role decides how it deals damage.
 */
public abstract class Champion {

    private final String name;
    private double maxHealth;
    private double health;
    private double attackDamage;
    private double abilityPower;
    private double armor;
    private double magicResist;

    protected Champion(String name, double maxHealth, double attackDamage, double abilityPower,
                       double armor, double magicResist) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Champion name must not be blank");
        }
        if (maxHealth <= 0) {
            throw new IllegalArgumentException(name + ": max health must be positive");
        }
        if (attackDamage < 0 || abilityPower < 0 || armor < 0 || magicResist < 0) {
            throw new IllegalArgumentException(name + ": stats must not be negative");
        }
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.attackDamage = attackDamage;
        this.abilityPower = abilityPower;
        this.armor = armor;
        this.magicResist = magicResist;
    }

    /**
     * Parameters
     *   target : the champion being attacked
     * What it does
     *   Computes the raw damage of one hit, before the target's resistances.
     *   Each role has its own formula.
     * Output
     *   Raw damage (never negative).
     */
    public abstract double computeDamage(Champion target);

    /** The kind of damage this role deals, used to pick armor or magic resist. */
    public abstract DamageType getDamageType();

    /** Role name shown in the console and read from the CSV file. */
    public abstract String getRole();

    /**
     * Parameters
     *   target : the champion being attacked
     * What it does
     *   One full hit: this champion's formula, then the target's defences.
     *   Final so every role goes through the same steps.
     * Output
     *   Health actually removed from the target.
     */
    public final double attack(Champion target) {
        if (target == this) {
            throw new IllegalArgumentException(name + " cannot attack itself");
        }
        return target.takeDamage(computeDamage(target), getDamageType());
    }

    /**
     * Parameters
     *   rawDamage : damage before resistances
     *   type      : physical or magic
     * What it does
     *   Applies League's resistance formula: damage * 100 / (100 + resistance),
     *   lets a defensive passive absorb part of it, then removes the rest from health
     *   (health never goes below 0).
     * Output
     *   Health actually removed.
     */
    public final double takeDamage(double rawDamage, DamageType type) {
        if (rawDamage < 0) {
            throw new IllegalArgumentException("Damage must not be negative");
        }
        double resistance = type == DamageType.PHYSICAL ? armor : magicResist;
        double mitigated = rawDamage * 100 / (100 + resistance);
        return loseHealth(absorb(mitigated));
    }

    /**
     * Hook for defensive passives (shields). No passive by default: nothing is absorbed.
     * Returns the damage that still goes through to health.
     */
    protected double absorb(double damage) {
        return damage;
    }

    /** Removes health and returns how much was really lost (a dead champion cannot lose more). */
    private double loseHealth(double amount) {
        double lost = Math.min(amount, health);
        health -= lost;
        return lost;
    }

    /** Full health again, before a new duel. Subclasses reset their passives here. */
    public void resetForDuel() {
        health = maxHealth;
    }

    public boolean isAlive() {
        return health > 0;
    }

    /** Between 0 and 1: 0.3 means 30 % of max health left. */
    public double getHealthRatio() {
        return health / maxHealth;
    }

    public String getName() {
        return name;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getHealth() {
        return health;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public double getAbilityPower() {
        return abilityPower;
    }

    public double getArmor() {
        return armor;
    }

    public double getMagicResist() {
        return magicResist;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %.0f/%.0f HP", name, getRole(), health, maxHealth);
    }
}
