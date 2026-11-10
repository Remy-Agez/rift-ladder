package com.riftladder;

import com.riftladder.model.Player;
import com.riftladder.service.ChampionCatalog;
import com.riftladder.service.DuelService;
import com.riftladder.service.LadderService;
import com.riftladder.ui.ConsoleMenu;
import com.riftladder.utils.CsvReader;
import com.riftladder.utils.DataLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point: loads the data, builds the services and starts the menu. Nothing else.
 * Usage: java -jar rift-ladder.jar [dataFolder]
 * Without a folder, the CSV files shipped in src/main/resources/data are used.
 */
public class Application {

    public static void main(String[] args) {
        Path dataDir = args.length > 0 ? Path.of(args[0]) : null;
        try {
            ChampionCatalog catalog;
            try (BufferedReader reader = DataLoader.open(DataLoader.CHAMPIONS_FILE, dataDir)) {
                catalog = new ChampionCatalog(keepValid(DataLoader.CHAMPIONS_FILE, DataLoader.readChampions(reader)));
            }
            List<Player> players;
            try (BufferedReader reader = DataLoader.open(DataLoader.PLAYERS_FILE, dataDir)) {
                players = keepValid(DataLoader.PLAYERS_FILE, DataLoader.readPlayers(reader, catalog));
            }
            LadderService ladder = new LadderService(players, catalog, new DuelService());
            System.out.printf("Loaded %d champions and %d players.%n", catalog.size(), ladder.size());
            new ConsoleMenu(ladder, catalog, new Scanner(System.in), System.out).run();
        } catch (IOException e) {
            System.err.println("Cannot start, data file not readable: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Cannot start, invalid data: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Prints one warning per rejected line and returns the valid objects. */
    private static <T> List<T> keepValid(String file, CsvReader.Result<T> result) {
        result.errors().forEach(error -> System.out.println("Warning, " + file + " " + error + " (skipped)"));
        return result.items();
    }
}
