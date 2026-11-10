package com.riftladder.service;

import com.riftladder.model.Champion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A 1v1 fight: the two champions hit each other in turn until one falls. No randomness. */
public class DuelService {

    /** Safety stop: two champions that cannot hurt each other would otherwise fight forever. */
    public static final int MAX_TURNS = 100;

    /** What happened in a duel. winner is empty when it ended in a draw. */
    public record DuelResult(Champion first, Champion second, Optional<Champion> winner, int turns,
                             List<String> log) {
    }

    /**
     * Parameters
     *   first  : the champion who strikes first (the challenger)
     *   second : the other champion
     * What it does
     *   Resets both champions, then alternates attacks. Each attack goes through
     *   Champion.attack, so every role uses its own formula (polymorphism) without this class
     *   knowing which role it is. Stops when one dies or after MAX_TURNS (draw).
     * Output
     *   The result with a readable log line per turn.
     */
    public DuelResult fight(Champion first, Champion second) {
        if (first == second) {
            throw new IllegalArgumentException("A champion cannot duel itself");
        }
        first.resetForDuel();
        second.resetForDuel();
        List<String> log = new ArrayList<>();
        Champion attacker = first;
        Champion defender = second;
        int turns = 0;
        while (first.isAlive() && second.isAlive() && turns < MAX_TURNS) {
            turns++;
            double dealt = attacker.attack(defender);
            log.add(String.format("Turn %2d: %s hits %s for %.0f (%.0f HP left)",
                    turns, attacker.getName(), defender.getName(), dealt, defender.getHealth()));
            Champion swap = attacker;
            attacker = defender;
            defender = swap;
        }
        Optional<Champion> winner = Optional.empty();
        if (!second.isAlive()) {
            winner = Optional.of(first);
        } else if (!first.isAlive()) {
            winner = Optional.of(second);
        }
        return new DuelResult(first, second, winner, turns, log);
    }
}
