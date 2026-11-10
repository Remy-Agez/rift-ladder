package com.riftladder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Each role's own damage formula and passive. */
class RolesTest {

    private static final double DELTA = 0.001;

    /** A target with no resistances, so raw damage equals health lost. */
    private static Champion dummy(double maxHealth) {
        return new Mage("Target Dummy", maxHealth, 0, 0, 0, 0);
    }

    @Test
    void assassinShouldDealNormalDamageToHealthyTarget() {
        Champion katarina = new Assassin("Katarina", 600, 100, 0, 0, 0);

        assertEquals(100, katarina.computeDamage(dummy(1000)), DELTA);
    }

    @Test
    void assassinShouldDealBonusDamageToLowTarget() {
        Champion katarina = new Assassin("Katarina", 600, 100, 0, 0, 0);
        Champion target = dummy(1000);
        target.takeDamage(800, DamageType.PHYSICAL); // 20 % health left

        assertEquals(150, katarina.computeDamage(target), DELTA);
    }

    @Test
    void assassinShouldNotGetBonusAtExactlyThirtyPercent() {
        Champion katarina = new Assassin("Katarina", 600, 100, 0, 0, 0);
        Champion target = dummy(1000);
        target.takeDamage(700, DamageType.PHYSICAL); // exactly 30 %

        assertEquals(100, katarina.computeDamage(target), DELTA);
    }

    @Test
    void mageShouldIgnoreArmorAndUseMagicResist() {
        Champion lux = new Mage("Lux", 500, 0, 120, 0, 0);
        Champion malphite = new Tank("Malphite", 1000, 0, 0, 300, 20);

        lux.attack(malphite);

        // 120 * 100 / (100 + 20) = 100, then the 100 shield absorbs all of it
        assertEquals(1000, malphite.getHealth(), DELTA);
        assertEquals(0, ((Tank) malphite).getShield(), DELTA);
    }

    @Test
    void tankDamageShouldScaleWithMaxHealth() {
        Champion malphite = new Tank("Malphite", 1000, 50, 0, 0, 0);

        // 50 + 5 % of 1000
        assertEquals(100, malphite.computeDamage(dummy(500)), DELTA);
    }

    @Test
    void tankShieldShouldAbsorbFirstDamageOnly() {
        Tank malphite = new Tank("Malphite", 1000, 50, 0, 0, 0);

        double firstHit = malphite.takeDamage(150, DamageType.PHYSICAL);
        double secondHit = malphite.takeDamage(150, DamageType.PHYSICAL);

        assertEquals(50, firstHit, DELTA);   // 100 absorbed by the shield
        assertEquals(150, secondHit, DELTA); // shield is gone
        assertEquals(800, malphite.getHealth(), DELTA);
    }

    @Test
    void tankShieldShouldComeBackForNewDuel() {
        Tank malphite = new Tank("Malphite", 1000, 50, 0, 0, 0);
        malphite.takeDamage(500, DamageType.PHYSICAL);

        malphite.resetForDuel();

        assertEquals(100, malphite.getShield(), DELTA);
        assertEquals(1000, malphite.getHealth(), DELTA);
    }
}
