package com.riftladder.ui;

import com.riftladder.exception.PlayerNotFoundException;
import com.riftladder.model.ChampionSpec;
import com.riftladder.model.Player;
import com.riftladder.service.ChampionCatalog;
import com.riftladder.service.LadderService;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Everything the user sees and types. No game rule lives here: it only calls the services. */
public class ConsoleMenu {

    private final LadderService ladder;
    private final ChampionCatalog catalog;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleMenu(LadderService ladder, ChampionCatalog catalog, Scanner in, PrintStream out) {
        this.ladder = ladder;
        this.catalog = catalog;
        this.in = in;
        this.out = out;
    }

    /** Shows the menu until the user picks 0 or the input ends. */
    public void run() {
        while (true) {
            out.println();
            out.println("=== RIFT LADDER ===");
            out.println("1. Leaderboard");
            out.println("2. Champion list");
            out.println("3. Player profile");
            out.println("4. Play a ranked duel");
            out.println("5. Win rate by role");
            out.println("6. Simulate one day of inactivity decay");
            out.println("0. Quit");
            String choice = ask("Your choice: ");
            if (choice == null || choice.equals("0")) {
                out.println("GG, see you on the Rift.");
                return;
            }
            try {
                handle(choice);
            } catch (PlayerNotFoundException | IllegalArgumentException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handle(String choice) {
        switch (choice) {
            case "1" -> showLeaderboard();
            case "2" -> showChampions();
            case "3" -> showProfile();
            case "4" -> playDuel();
            case "5" -> showRoleStats();
            case "6" -> runDecay();
            default -> out.println("Unknown choice, type a number from the menu.");
        }
    }

    /** Prints the question and returns the trimmed answer, or null if there is no more input. */
    private String ask(String question) {
        out.print(question);
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private String askRequired(String question) {
        String answer = ask(question);
        if (answer == null || answer.isEmpty()) {
            throw new IllegalArgumentException("An answer is required");
        }
        return answer;
    }

    private void showLeaderboard() {
        List<Player> board = ladder.leaderboard();
        for (int i = 0; i < board.size(); i++) {
            out.printf("%2d. %s%n", i + 1, board.get(i));
        }
    }

    private void showChampions() {
        for (ChampionSpec c : catalog.all()) {
            out.printf("%-10s %-9s HP %5.0f  AD %4.0f  AP %4.0f  Armor %4.0f  MR %4.0f%n",
                    c.name(), c.role(), c.maxHealth(), c.attackDamage(), c.abilityPower(), c.armor(),
                    c.magicResist());
        }
    }

    private void showProfile() {
        String name = askRequired("Player name: ");
        Player p = ladder.findPlayer(name).orElseThrow(() -> new PlayerNotFoundException(name));
        out.println(p);
        out.printf("Win rate: %.1f %%  |  Last game: %s%n", p.getWinRate() * 100, p.getLastPlayed());
    }

    private void playDuel() {
        String challenger = askRequired("Challenger (strikes first): ");
        String opponent = askRequired("Opponent: ");
        LadderService.MatchReport report = ladder.playRanked(challenger, opponent, LocalDate.now());
        report.duel().log().forEach(out::println);
        if (report.winner().isEmpty()) {
            out.println("Draw after " + report.duel().turns() + " turns: no LP change.");
            return;
        }
        Player winner = report.winner().get();
        Player loser = report.loser().orElseThrow();
        out.printf("%s wins! +%d LP -> %s %d LP%n", winner.getName(), report.lpGained(), winner.getRank(),
                winner.getLp());
        out.printf("%s loses. -%d LP -> %s %d LP%n", loser.getName(), report.lpLost(), loser.getRank(),
                loser.getLp());
    }

    private void showRoleStats() {
        Map<String, Double> rates = ladder.winRateByRole();
        rates.forEach((role, rate) -> out.printf("%-9s %5.1f %%%n", role, rate * 100));
    }

    private void runDecay() {
        List<Player> decayed = ladder.applyDecay(LocalDate.now());
        if (decayed.isEmpty()) {
            out.println("Nobody decayed.");
        }
        decayed.forEach(p -> out.printf("%s lost %d LP for inactivity -> %s %d LP%n", p.getName(),
                LadderService.DECAY_LP, p.getRank(), p.getLp()));
    }
}
