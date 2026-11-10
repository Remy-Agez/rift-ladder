package com.riftladder.model;

/** Magic damage scaling on ability power, so armor does nothing against it (only magic resist does). */
public class Mage extends Champion {

    public Mage(String name, double maxHealth, double attackDamage, double abilityPower,
                double armor, double magicResist) {
        super(name, maxHealth, attackDamage, abilityPower, armor, magicResist);
    }

    @Override
    public double computeDamage(Champion target) {
        return getAbilityPower();
    }

    @Override
    public DamageType getDamageType() {
        return DamageType.MAGIC;
    }

    @Override
    public String getRole() {
        return "Mage";
    }
}
