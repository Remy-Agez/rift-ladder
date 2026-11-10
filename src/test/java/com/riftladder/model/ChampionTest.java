package com.riftladder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Behaviour shared by every champion, tested through concrete roles since Champion is abstract. */
class ChampionTest {

    private static final double DELTA = 0.001;

    @Test
    void shouldReduceDamageWithArmor() {
        Champion katarina = new Assassin("Katarina", 600, 200, 0, 0, 0);
        Champion lux = new Mage("Lux", 1000, 0, 0, 100, 0);

        double lost = katarina.attack(lux);

        // 200 * 100 / (100 + 100) = 100
        assertEquals(100, lost, DELTA);
        assertEquals(900, lux.getHealth(), DELTA);
    }

    @Test
    void shouldNeverDropHealthBelowZero() {
        Champion lux = new Mage("Lux", 50, 0, 0, 0, 0);

        double lost = lux.takeDamage(200, DamageType.PHYSICAL);

        assertEquals(50, lost, DELTA);
        assertEquals(0, lux.getHealth(), DELTA);
        assertFalse(lux.isAlive());
    }

    @Test
    void shouldRestoreFullHealthForNewDuel() {
        Champion lux = new Mage("Lux", 500, 0, 0, 0, 0);
        lux.takeDamage(300, DamageType.MAGIC);

        lux.resetForDuel();

        assertEquals(500, lux.getHealth(), DELTA);
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Mage(" ", 500, 0, 0, 0, 0));
    }

    @Test
    void shouldRejectNegativeStats() {
        assertThrows(IllegalArgumentException.class,
                () -> new Assassin("Zed", 600, -10, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Tank("Malphite", 0, 50, 0, 0, 0));
    }

    @Test
    void shouldRejectNegativeDamage() {
        Champion lux = new Mage("Lux", 500, 0, 0, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> lux.takeDamage(-1, DamageType.PHYSICAL));
    }

    @Test
    void shouldNotAttackItself() {
        Champion katarina = new Assassin("Katarina", 600, 100, 0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> katarina.attack(katarina));
    }
}
