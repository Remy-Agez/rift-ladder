package com.riftladder.service;

import com.riftladder.exception.PlayerNotFoundException;
import com.riftladder.model.ChampionSpec;
import com.riftladder.model.Player;
import com.riftladder.model.Rank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LadderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    /** OneShot kills anything in one hit, Pacifist deals no damage: duel results are known in advance. */
    private final ChampionCatalog catalog = new ChampionCatalog(List.of(
            new ChampionSpec("Assassin", "OneShot", 100, 100_000, 0, 0, 0),
            new ChampionSpec("Mage", "Pacifist", 100, 0, 0, 0, 0)));

    private Player killer;
    private Player goldTarget;
    private Player diamond;
    private Player otherPacifist;
    private LadderService ladder;

    @BeforeEach
    void setUp() {
        killer = new Player("Killer", "OneShot", Rank.GOLD, 50, 6, 4, TODAY);
        goldTarget = new Player("GoldTarget", "Pacifist", Rank.GOLD, 50, 1, 3, TODAY);
        diamond = new Player("Diamond", "Pacifist", Rank.DIAMOND, 10, 3, 2, TODAY.minusDays(29));
        otherPacifist = new Player("Calm", "Pacifist", Rank.SILVER, 0, 0, 0, TODAY.minusDays(60));
        ladder = new LadderService(List.of(killer, goldTarget, diamond, otherPacifist), catalog, new DuelService());
    }

    @Test
    void shouldMoveLpAfterARankedWin() {
        LadderService.MatchReport report = ladder.playRanked("Killer", "GoldTarget", TODAY);

        assertEquals(killer, report.winner().orElseThrow());
        assertEquals(75, killer.getLp());
        assertEquals(30, goldTarget.getLp());
    }

    @Test
    void shouldGiveUpsetBonusWhenBeatingAHigherTier() {
        // Gold beats Diamond: 3 tiers above -> 25 + 3 * 5
        assertEquals(40, ladder.lpForWin(killer, diamond));
    }

    @Test
    void shouldGiveNoBonusWhenWinnerIsHigher() {
        assertEquals(LadderService.WIN_LP, ladder.lpForWin(diamond, killer));
    }

    @Test
    void shouldChangeNothingOnADraw() {
        LadderService.MatchReport report = ladder.playRanked("GoldTarget", "Calm", TODAY);

        assertTrue(report.winner().isEmpty());
        assertEquals(50, goldTarget.getLp());
        assertEquals(3, goldTarget.getLosses());
    }

    @Test
    void shouldThrowForUnknownPlayer() {
        assertThrows(PlayerNotFoundException.class, () -> ladder.playRanked("Killer", "Nobody", TODAY));
    }

    @Test
    void shouldRefuseAPlayerAgainstThemselves() {
        assertThrows(IllegalArgumentException.class, () -> ladder.playRanked("Killer", "kILLER", TODAY));
    }

    @Test
    void shouldFindPlayersIgnoringCase() {
        assertEquals(diamond, ladder.findPlayer(" diamond ").orElseThrow());
        assertTrue(ladder.findPlayer("Ghost").isEmpty());
    }

    @Test
    void shouldSortLeaderboardByTierThenLpThenName() {
        List<String> names = ladder.leaderboard().stream().map(Player::getName).toList();

        // Killer and GoldTarget are both Gold 50: alphabetical order decides
        assertEquals(List.of("Diamond", "GoldTarget", "Killer", "Calm"), names);
    }

    @Test
    void shouldDecayOnlyInactiveHighTierPlayers() {
        List<Player> decayed = ladder.applyDecay(TODAY);

        // Diamond: 29 days -> decays and is demoted. Calm: 60 days but Silver -> safe.
        assertEquals(List.of(diamond), decayed);
        assertEquals(Rank.EMERALD, diamond.getRank());
        assertEquals(Player.DEMOTION_LP, diamond.getLp());
    }

    @Test
    void shouldNotDecayOnTheLastAllowedDay() {
        assertTrue(ladder.applyDecay(TODAY.minusDays(1)).isEmpty()); // 28 days exactly
    }

    @Test
    void shouldComputeWinRateByRoleFromTotals() {
        Map<String, Double> rates = ladder.winRateByRole();

        assertEquals(0.6, rates.get("Assassin"), 0.0001); // 6 / 10
        assertEquals(4.0 / 9, rates.get("Mage"), 0.0001); // (1 + 3) / (4 + 5), Calm has no game
    }
}
