package com.riftladder.exception;

/**
 * The user typed a player name that is not on the ladder.
 * Unchecked: it is a wrong input, the console catches it and asks again.
 */
public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String name) {
        super("No player named \"" + name + "\" on the ladder");
    }
}
