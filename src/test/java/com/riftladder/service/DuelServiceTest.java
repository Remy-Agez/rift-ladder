package com.riftladder.service;

import com.riftladder.model.Assassin;
import com.riftladder.model.Champion;
import com.riftladder.model.DamageType;
import com.riftladder.model.Mage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuelServiceTest {

    private final DuelService duels = new DuelService();

    @Test
    void shouldLetTheStrongerChampionWin() {
        Champion strong = new Mage("Syndra", 1000, 0, 300, 0, 0);
        Champion weak = new Mage("Dummy", 1000, 0, 50, 0, 0);

        DuelService.DuelResult result = duels.fight(weak, strong);

        assertSame(strong, result.winner().orElseThrow());
        assertEquals(result.turns(), result.log().size());
    }

    @Test
    void shouldGiveFirstStrikeToTheChallenger() {
        Champion a = new Assassin("Zed", 100, 100, 0, 0, 0);
        Champion b = new Assassin("Talon", 100, 100, 0, 0, 0);

        DuelService.DuelResult result = duels.fight(a, b);

        assertSame(a, result.winner().orElseThrow());
        assertEquals(1, result.turns());
    }

    @Test
    void shouldEndInDrawWhenNobodyCanDealDamage() {
        Champion a = new Mage("Pacifist A", 100, 0, 0, 0, 0);
        Champion b = new Mage("Pacifist B", 100, 0, 0, 0, 0);

        DuelService.DuelResult result = duels.fight(a, b);

        assertTrue(result.winner().isEmpty());
        assertEquals(DuelService.MAX_TURNS, result.turns());
    }

    @Test
    void shouldStartFromFullHealth() {
        Champion hurt = new Assassin("Zed", 200, 100, 0, 0, 0);
        Champion other = new Assassin("Talon", 100, 100, 0, 0, 0);
        hurt.takeDamage(150, DamageType.PHYSICAL); // 50 HP left before the duel

        DuelService.DuelResult result = duels.fight(other, hurt);

        // Healed to 200, it survives the first 100 hit and wins on its turn
        assertSame(hurt, result.winner().orElseThrow());
        assertEquals(100, hurt.getHealth());
    }

    @Test
    void shouldRejectDuelAgainstItself() {
        Champion a = new Assassin("Zed", 100, 100, 0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> duels.fight(a, a));
    }
}
