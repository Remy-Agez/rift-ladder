package com.riftladder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTest {

    @Test
    void shouldGoUpOneTier() {
        assertEquals(Rank.BRONZE, Rank.IRON.next());
        assertEquals(Rank.GRANDMASTER, Rank.MASTER.next());
    }

    @Test
    void shouldStayAtHighestTier() {
        assertEquals(Rank.CHALLENGER, Rank.CHALLENGER.next());
        assertTrue(Rank.CHALLENGER.isHighest());
    }

    @Test
    void shouldGoDownOneTier() {
        assertEquals(Rank.SILVER, Rank.GOLD.previous());
    }

    @Test
    void shouldNotGoBelowLowestTier() {
        assertEquals(Rank.IRON, Rank.IRON.previous());
        assertTrue(Rank.IRON.isLowest());
    }
}
