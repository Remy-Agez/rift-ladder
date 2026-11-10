package com.riftladder.exception;

/**
 * A line of a data file cannot be turned into an object (wrong number of columns,
 * text where a number is expected, unknown role...).
 * Checked on purpose: whoever reads a file must decide what to do with a bad line.
 */
public class InvalidDataException extends Exception {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
