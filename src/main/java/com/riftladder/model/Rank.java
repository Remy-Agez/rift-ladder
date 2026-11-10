package com.riftladder.model;

/**
 * Ranked tiers, ordered from lowest to highest.
 * The declaration order matters: next() and previous() rely on it.
 */
public enum Rank {
    IRON,
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    EMERALD,
    DIAMOND,
    MASTER,
    GRANDMASTER,
    CHALLENGER;

    /** The tier above this one, or the same tier if it is already the highest. */
    public Rank next() {
        Rank[] ranks = values();
        return ordinal() == ranks.length - 1 ? this : ranks[ordinal() + 1];
    }

    /** The tier below this one, or the same tier if it is already the lowest. */
    public Rank previous() {
        return ordinal() == 0 ? this : values()[ordinal() - 1];
    }

    public boolean isHighest() {
        return this == values()[values().length - 1];
    }

    public boolean isLowest() {
        return this == values()[0];
    }
}
