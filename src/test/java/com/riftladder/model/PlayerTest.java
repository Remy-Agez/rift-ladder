package com.riftladder.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** LP, promotions and demotions. */
class PlayerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private static Player player(Rank rank, int lp) {
        return new Player("Tester", "Katarina", rank, lp, 10, 10, DAY.minusDays(1));
    }

    @Test
    void shouldAddLpAndCountTheWin() {
        Player p = player(Rank.GOLD, 10);

        p.recordWin(25, DAY);

        assertEquals(Rank.GOLD, p.getRank());
        assertEquals(35, p.getLp());
        assertEquals(11, p.getWins());
        assertEquals(DAY, p.getLastPlayed());
    }

    @Test
    void shouldPromoteWhenReachingHundredLp() {
        Player p = player(Rank.GOLD, 90);

        p.recordWin(25, DAY);

        assertEquals(Rank.PLATINUM, p.getRank());
        assertEquals(15, p.getLp());
    }

    @Test
    void shouldDemoteAndLandOnDemotionLp() {
        Player p = player(Rank.GOLD, 10);

        p.recordLoss(20, DAY);

        assertEquals(Rank.SILVER, p.getRank());
        assertEquals(Player.DEMOTION_LP, p.getLp());
        assertEquals(11, p.getLosses());
    }

    @Test
    void shouldStopAtZeroLpInLowestTier() {
        Player p = player(Rank.IRON, 10);

        p.recordLoss(20, DAY);

        assertEquals(Rank.IRON, p.getRank());
        assertEquals(0, p.getLp());
    }

    @Test
    void shouldKeepGrowingLpAtHighestTier() {
        Player p = player(Rank.CHALLENGER, 90);

        p.recordWin(25, DAY);

        assertEquals(Rank.CHALLENGER, p.getRank());
        assertEquals(115, p.getLp());
    }

    @Test
    void shouldHaveZeroWinRateWithoutGames() {
        Player p = new Player("Fresh", "Lux", Rank.IRON, 0, 0, 0, DAY);

        assertEquals(0, p.getWinRate());
    }

    @Test
    void shouldComputeWinRate() {
        Player p = new Player("Half", "Lux", Rank.IRON, 0, 3, 1, DAY);

        assertEquals(0.75, p.getWinRate(), 0.0001);
    }

    @Test
    void shouldRankHigherTierAboveMoreLp() {
        assertTrue(player(Rank.PLATINUM, 0).getLadderScore() > player(Rank.GOLD, 99).getLadderScore());
    }

    @Test
    void shouldRejectHundredLpBelowHighestTier() {
        assertThrows(IllegalArgumentException.class, () -> player(Rank.GOLD, 100));
    }

    @Test
    void shouldRejectNegativeLpAndGames() {
        assertThrows(IllegalArgumentException.class, () -> player(Rank.GOLD, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Player("Bad", "Lux", Rank.GOLD, 0, -1, 0, DAY));
    }

    @Test
    void shouldRejectBlankNameOrChampion() {
        assertThrows(IllegalArgumentException.class, () -> new Player(" ", "Lux", Rank.GOLD, 0, 0, 0, DAY));
        assertThrows(IllegalArgumentException.class, () -> new Player("Bob", "", Rank.GOLD, 0, 0, 0, DAY));
    }

    @Test
    void shouldRejectNonPositiveLpChange() {
        Player p = player(Rank.GOLD, 50);

        assertThrows(IllegalArgumentException.class, () -> p.recordWin(0, DAY));
        assertThrows(IllegalArgumentException.class, () -> p.loseLp(-5));
    }
}
