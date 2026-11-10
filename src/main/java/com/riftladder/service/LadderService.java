package com.riftladder.service;

import com.riftladder.exception.PlayerNotFoundException;
import com.riftladder.model.ChampionSpec;
import com.riftladder.model.Player;
import com.riftladder.model.Rank;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** The ranked ladder: plays ranked games, moves LP, sorts the players and gives statistics. */
public class LadderService {

    public static final int WIN_LP = 25;
    public static final int LOSS_LP = 20;
    /** Extra LP for each tier the loser is above the winner. */
    public static final int UPSET_BONUS_LP = 5;
    /** Inactivity decay: from this tier, after this many days without a game, lose DECAY_LP. */
    public static final Rank DECAY_FROM = Rank.DIAMOND;
    public static final int DECAY_DAYS = 28;
    public static final int DECAY_LP = 50;

    private final List<Player> players;
    private final ChampionCatalog catalog;
    private final DuelService duelService;

    /** The result of a ranked game. winner and loser are empty after a draw (no LP moves). */
    public record MatchReport(DuelService.DuelResult duel, Optional<Player> winner, Optional<Player> loser,
                              int lpGained, int lpLost) {
    }

    public LadderService(List<Player> players, ChampionCatalog catalog, DuelService duelService) {
        this.players = new ArrayList<>(players);
        this.catalog = catalog;
        this.duelService = duelService;
    }

    /** Case does not matter. */
    public Optional<Player> findPlayer(String name) {
        return players.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    private Player getPlayer(String name) {
        return findPlayer(name).orElseThrow(() -> new PlayerNotFoundException(name));
    }

    /**
     * Parameters
     *   challengerName : player who starts the duel (their champion strikes first)
     *   opponentName   : the other player
     *   date           : day of the game, stored as lastPlayed
     * What it does
     *   Each player fights with a fresh copy of their main champion, then the winner gains LP
     *   (with the upset bonus) and the loser loses LOSS_LP. A draw changes nothing.
     * Output
     *   The match report. PlayerNotFoundException for an unknown name,
     *   IllegalArgumentException if both names are the same player.
     */
    public MatchReport playRanked(String challengerName, String opponentName, LocalDate date) {
        Player challenger = getPlayer(challengerName);
        Player opponent = getPlayer(opponentName);
        if (challenger == opponent) {
            throw new IllegalArgumentException("A player cannot play against themselves");
        }
        DuelService.DuelResult duel = duelService.fight(
                catalog.create(challenger.getMainChampion()), catalog.create(opponent.getMainChampion()));
        if (duel.winner().isEmpty()) {
            return new MatchReport(duel, Optional.empty(), Optional.empty(), 0, 0);
        }
        boolean challengerWon = duel.winner().get() == duel.first();
        Player winner = challengerWon ? challenger : opponent;
        Player loser = challengerWon ? opponent : challenger;
        int gained = lpForWin(winner, loser);
        winner.recordWin(gained, date);
        loser.recordLoss(LOSS_LP, date);
        return new MatchReport(duel, Optional.of(winner), Optional.of(loser), gained, LOSS_LP);
    }

    /** WIN_LP, plus UPSET_BONUS_LP for each tier the loser is above the winner. */
    public int lpForWin(Player winner, Player loser) {
        int tierGap = loser.getRank().ordinal() - winner.getRank().ordinal();
        return WIN_LP + UPSET_BONUS_LP * Math.max(0, tierGap);
    }

    /** Best first: highest tier, then most LP, then name for a stable order. */
    public List<Player> leaderboard() {
        return players.stream()
                .sorted(Comparator.comparingInt(Player::getLadderScore).reversed()
                        .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Parameters
     *   none
     * What it does
     *   Groups the players by the role of their main champion and computes the global
     *   win rate of each role (total wins / total games, not an average of averages).
     * Output
     *   Role -> win rate between 0 and 1, sorted by role name. Roles with no game are left out.
     */
    public Map<String, Double> winRateByRole() {
        Map<String, List<Player>> byRole = players.stream()
                .collect(Collectors.groupingBy(this::roleOf, TreeMap::new, Collectors.toList()));
        Map<String, Double> rates = new TreeMap<>();
        byRole.forEach((role, list) -> {
            int wins = list.stream().mapToInt(Player::getWins).sum();
            int games = wins + list.stream().mapToInt(Player::getLosses).sum();
            if (games > 0) {
                rates.put(role, (double) wins / games);
            }
        });
        return rates;
    }

    private String roleOf(Player player) {
        return catalog.find(player.getMainChampion()).map(ChampionSpec::role).orElse("Unknown");
    }

    /**
     * Parameters
     *   today : the date to compare lastPlayed with
     * What it does
     *   Like League's high-elo decay: a player at DECAY_FROM or above who has not played
     *   for more than DECAY_DAYS loses DECAY_LP (and may be demoted).
     * Output
     *   The players who decayed (empty list if nobody did).
     */
    public List<Player> applyDecay(LocalDate today) {
        List<Player> decayed = players.stream()
                .filter(p -> p.getRank().compareTo(DECAY_FROM) >= 0)
                .filter(p -> ChronoUnit.DAYS.between(p.getLastPlayed(), today) > DECAY_DAYS)
                .toList();
        decayed.forEach(p -> p.loseLp(DECAY_LP));
        return decayed;
    }

    public int size() {
        return players.size();
    }
}
