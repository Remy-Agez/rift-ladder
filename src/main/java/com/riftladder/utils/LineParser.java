package com.riftladder.utils;

import com.riftladder.exception.InvalidDataException;

/**
 * Turns the columns of one CSV line into an object of type T.
 * An interface so CsvReader can read any file (champions, players...) without knowing what is inside.
 */
@FunctionalInterface
public interface LineParser<T> {

    T parse(String[] fields) throws InvalidDataException;
}
