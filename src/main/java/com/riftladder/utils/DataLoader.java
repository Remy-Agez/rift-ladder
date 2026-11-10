package com.riftladder.utils;

import com.riftladder.exception.InvalidDataException;
import com.riftladder.model.ChampionSpec;
import com.riftladder.model.Player;
import com.riftladder.model.Rank;
import com.riftladder.service.ChampionCatalog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Knows the two data files: where they are and what each column means. */
public final class DataLoader {

    public static final String CHAMPIONS_FILE = "champions.csv";
    public static final String PLAYERS_FILE = "players.csv";
    private static final int COLUMNS = 7;

    private DataLoader() {
    }

    /**
     * Parameters
     *   fileName : CHAMPIONS_FILE or PLAYERS_FILE
     *   dataDir  : folder given on the command line, or null to use the files shipped in the jar
     * What it does
     *   Opens the file as UTF-8 text.
     * Output
     *   A reader to close after use. FileNotFoundException / IOException if the file is missing.
     */
    public static BufferedReader open(String fileName, Path dataDir) throws IOException {
        if (dataDir != null) {
            return Files.newBufferedReader(dataDir.resolve(fileName), StandardCharsets.UTF_8);
        }
        InputStream in = DataLoader.class.getResourceAsStream("/data/" + fileName);
        if (in == null) {
            throw new FileNotFoundException("Missing resource data/" + fileName);
        }
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Reads champions.csv: role,name,maxHealth,attackDamage,abilityPower,armor,magicResist */
    public static CsvReader.Result<ChampionSpec> readChampions(BufferedReader reader) throws IOException {
        return CsvReader.read(reader, COLUMNS, DataLoader::parseChampion);
    }

    /** Reads players.csv: name,mainChampion,rank,lp,wins,losses,lastPlayed (yyyy-MM-dd) */
    public static CsvReader.Result<Player> readPlayers(BufferedReader reader, ChampionCatalog catalog)
            throws IOException {
        return CsvReader.read(reader, COLUMNS, fields -> parsePlayer(fields, catalog));
    }

    static ChampionSpec parseChampion(String[] f) throws InvalidDataException {
        ChampionSpec spec = new ChampionSpec(f[0], f[1],
                CsvReader.toDouble(f[2], "maxHealth"), CsvReader.toDouble(f[3], "attackDamage"),
                CsvReader.toDouble(f[4], "abilityPower"), CsvReader.toDouble(f[5], "armor"),
                CsvReader.toDouble(f[6], "magicResist"));
        try {
            spec.create(); // the constructors already check role and stats: reuse them
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(e.getMessage(), e);
        }
        return spec;
    }

    static Player parsePlayer(String[] f, ChampionCatalog catalog) throws InvalidDataException {
        String champion = catalog.find(f[1])
                .map(ChampionSpec::name)
                .orElseThrow(() -> new InvalidDataException("unknown champion \"" + f[1] + "\""));
        Rank rank;
        LocalDate lastPlayed;
        try {
            rank = Rank.valueOf(f[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("unknown rank \"" + f[2] + "\"", e);
        }
        try {
            lastPlayed = LocalDate.parse(f[6]);
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("lastPlayed is not a yyyy-MM-dd date: \"" + f[6] + "\"", e);
        }
        try {
            return new Player(f[0], champion, rank, CsvReader.toInt(f[3], "lp"),
                    CsvReader.toInt(f[4], "wins"), CsvReader.toInt(f[5], "losses"), lastPlayed);
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(e.getMessage(), e);
        }
    }
}
