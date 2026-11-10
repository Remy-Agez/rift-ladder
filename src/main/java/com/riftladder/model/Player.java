package com.riftladder.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A ranked player: who they are, which champion they main, and where they stand on the ladder.
 * Rank and LP only change through recordWin / recordLoss / loseLp, so the 0-99 LP rule always holds.
 */
public class Player {

    /** LP needed to go up one tier. */
    public static final int LP_PER_TIER = 100;
    /** LP you land on after a demotion (like League's "demotion shield" landing at 75). */
    public static final int DEMOTION_LP = 75;

    private final String name;
    private final String mainChampion;
    private Rank rank;
    private int lp;
    private int wins;
    private int losses;
    private LocalDate lastPlayed;

    public Player(String name, String mainChampion, Rank rank, int lp, int wins, int losses,
                  LocalDate lastPlayed) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        if (mainChampion == null || mainChampion.isBlank()) {
            throw new IllegalArgumentException(name + ": main champion must not be blank");
        }
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(lastPlayed, "lastPlayed");
        if (lp < 0 || (lp >= LP_PER_TIER && !rank.isHighest())) {
            throw new IllegalArgumentException(name + ": LP must be between 0 and 99 below the highest tier");
        }
        if (wins < 0 || losses < 0) {
            throw new IllegalArgumentException(name + ": wins and losses must not be negative");
        }
        this.name = name;
        this.mainChampion = mainChampion;
        this.rank = rank;
        this.lp = lp;
        this.wins = wins;
        this.losses = losses;
        this.lastPlayed = lastPlayed;
    }

    /**
     * Parameters
     *   gainedLp : LP won this game (positive)
     *   date     : day of the game
     * What it does
     *   Counts the win, adds the LP and promotes as many tiers as the LP allows.
     *   At the highest tier LP just keeps growing (Challenger has no ceiling).
     * Output
     *   None, the player is updated.
     */
    public void recordWin(int gainedLp, LocalDate date) {
        requirePositive(gainedLp);
        wins++;
        lastPlayed = Objects.requireNonNull(date, "date");
        lp += gainedLp;
        while (lp >= LP_PER_TIER && !rank.isHighest()) {
            lp -= LP_PER_TIER;
            rank = rank.next();
        }
    }

    /** Counts the loss, then removes the LP (see loseLp). */
    public void recordLoss(int lostLp, LocalDate date) {
        losses++;
        lastPlayed = Objects.requireNonNull(date, "date");
        loseLp(lostLp);
    }

    /**
     * Parameters
     *   amount : LP to remove (positive)
     * What it does
     *   Removes LP. Going below 0 demotes one tier and lands on DEMOTION_LP,
     *   except at the lowest tier where LP simply stops at 0.
     *   Used by losses and by inactivity decay.
     * Output
     *   None, the player is updated.
     */
    public void loseLp(int amount) {
        requirePositive(amount);
        lp -= amount;
        if (lp < 0) {
            if (rank.isLowest()) {
                lp = 0;
            } else {
                rank = rank.previous();
                lp = DEMOTION_LP;
            }
        }
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("LP amount must be positive");
        }
    }

    /** Wins divided by games played, 0 when no game was played yet (avoids a division by zero). */
    public double getWinRate() {
        int games = wins + losses;
        return games == 0 ? 0 : (double) wins / games;
    }

    /** One number to sort the ladder: tier first, then LP inside the tier. */
    public int getLadderScore() {
        return rank.ordinal() * LP_PER_TIER + lp;
    }

    public String getName() {
        return name;
    }

    public String getMainChampion() {
        return mainChampion;
    }

    public Rank getRank() {
        return rank;
    }

    public int getLp() {
        return lp;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public LocalDate getLastPlayed() {
        return lastPlayed;
    }

    @Override
    public String toString() {
        return String.format("%-14s %-11s %3d LP  %3dW %3dL  (%s)", name, rank, lp, wins, losses, mainChampion);
    }
}
