package com.riftladder.model;

/**
 * One line of champions.csv: the recipe of a champion, not a live champion.
 * A duel needs fresh champions (full health, full shield), so we keep the recipe
 * and build a new object each time with create().
 */
public record ChampionSpec(String role, String name, double maxHealth, double attackDamage,
                           double abilityPower, double armor, double magicResist) {

    /**
     * Parameters
     *   none
     * What it does
     *   Factory: picks the subclass from the role read in the file.
     *   This is the only place that knows the list of roles.
     * Output
     *   A new champion at full health. IllegalArgumentException for an unknown role or invalid stats.
     */
    public Champion create() {
        return switch (role.toLowerCase()) {
            case "assassin" -> new Assassin(name, maxHealth, attackDamage, abilityPower, armor, magicResist);
            case "mage" -> new Mage(name, maxHealth, attackDamage, abilityPower, armor, magicResist);
            case "tank" -> new Tank(name, maxHealth, attackDamage, abilityPower, armor, magicResist);
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }
}
