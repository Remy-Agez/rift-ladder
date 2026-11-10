package com.riftladder.utils;

import com.riftladder.exception.InvalidDataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Generic CSV reading: one header line, then one object per line. */
public final class CsvReader {

    private CsvReader() {
    }

    /** Objects read without problem, plus one message per rejected line. */
    public record Result<T>(List<T> items, List<String> errors) {
    }

    /**
     * Parameters
     *   reader          : the open file
     *   expectedColumns : number of columns every line must have
     *   parser          : turns the columns into an object
     * What it does
     *   Skips the header, blank lines and lines starting with '#'.
     *   A bad line does not stop the loading: it is skipped and reported with its line number.
     * Output
     *   The valid objects and the error messages. IOException if the file itself cannot be read.
     */
    public static <T> Result<T> read(BufferedReader reader, int expectedColumns, LineParser<T> parser)
            throws IOException {
        List<T> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        reader.readLine(); // header
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != expectedColumns) {
                errors.add("line " + lineNumber + ": expected " + expectedColumns + " columns, found " + fields.length);
                continue;
            }
            for (int i = 0; i < fields.length; i++) {
                fields[i] = fields[i].trim();
            }
            try {
                items.add(parser.parse(fields));
            } catch (InvalidDataException e) {
                errors.add("line " + lineNumber + ": " + e.getMessage());
            }
        }
        return new Result<>(items, errors);
    }

    /** Reads a decimal number, with the column name in the error message. */
    public static double toDouble(String value, String column) throws InvalidDataException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(column + " is not a number: \"" + value + "\"", e);
        }
    }

    /** Reads a whole number, with the column name in the error message. */
    public static int toInt(String value, String column) throws InvalidDataException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(column + " is not a whole number: \"" + value + "\"", e);
        }
    }
}
