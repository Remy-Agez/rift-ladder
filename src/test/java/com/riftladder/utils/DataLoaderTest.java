package com.riftladder.utils;

import com.riftladder.model.ChampionSpec;
import com.riftladder.model.Player;
import com.riftladder.service.ChampionCatalog;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataLoaderTest {

    private static final String CHAMPION_HEADER = "role,name,maxHealth,attackDamage,abilityPower,armor,magicResist\n";
    private static final String PLAYER_HEADER = "name,mainChampion,rank,lp,wins,losses,lastPlayed\n";

    private static BufferedReader text(String content) {
        return new BufferedReader(new StringReader(content));
    }

    private static CsvReader.Result<ChampionSpec> champions(String lines) throws IOException {
        return DataLoader.readChampions(text(CHAMPION_HEADER + lines));
    }

    private static CsvReader.Result<Player> players(String lines) throws IOException {
        ChampionCatalog catalog = new ChampionCatalog(List.of(new ChampionSpec("Mage", "Lux", 1800, 50, 250, 60, 45)));
        return DataLoader.readPlayers(text(PLAYER_HEADER + lines), catalog);
    }

    @Test
    void shouldLoadValidChampionsAndSkipBlankAndCommentLines() throws IOException {
        CsvReader.Result<ChampionSpec> result = champions("""
                Assassin,Katarina,2050,215,0,70,50

                # a comment
                Tank , Malphite , 2900 , 115 , 0 , 160 , 55
                """);

        assertTrue(result.errors().isEmpty());
        assertEquals(2, result.items().size());
        assertEquals("Malphite", result.items().get(1).name()); // spaces trimmed
    }

    @Test
    void shouldReportWrongColumnCountWithLineNumber() throws IOException {
        CsvReader.Result<ChampionSpec> result = champions("Assassin,Katarina,2050\n");

        assertTrue(result.items().isEmpty());
        assertEquals("line 2: expected 7 columns, found 3", result.errors().get(0));
    }

    @Test
    void shouldReportTextWhereANumberIsExpected() throws IOException {
        CsvReader.Result<ChampionSpec> result = champions("Mage,Lux,lots,50,250,60,45\n");

        assertTrue(result.errors().get(0).contains("maxHealth is not a number"));
    }

    @Test
    void shouldReportUnknownRoleAndInvalidStats() throws IOException {
        CsvReader.Result<ChampionSpec> result = champions("""
                Support,Yuumi,1500,40,100,30,30
                Mage,Lux,1800,50,250,-5,45
                Mage,Ahri,1900,60,240,65,50
                """);

        assertEquals(1, result.items().size()); // the bad lines do not stop the good one
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().get(0).contains("Unknown role"));
        assertTrue(result.errors().get(1).contains("must not be negative"));
    }

    @Test
    void shouldLoadAValidPlayer() throws IOException {
        CsvReader.Result<Player> result = players("Light,lux,GOLD,40,10,8,2026-11-01\n");

        assertTrue(result.errors().isEmpty());
        assertEquals("Lux", result.items().get(0).getMainChampion()); // name taken from the catalog
    }

    @Test
    void shouldReportInvalidPlayerLines() throws IOException {
        CsvReader.Result<Player> result = players("""
                A,Teemo,GOLD,40,10,8,2026-11-01
                B,Lux,WOOD,40,10,8,2026-11-01
                C,Lux,GOLD,40,10,8,01/11/2026
                D,Lux,GOLD,150,10,8,2026-11-01
                E,Lux,GOLD,forty,10,8,2026-11-01
                """);

        assertTrue(result.items().isEmpty());
        assertEquals(5, result.errors().size());
        assertTrue(result.errors().get(0).contains("unknown champion"));
        assertTrue(result.errors().get(1).contains("unknown rank"));
        assertTrue(result.errors().get(2).contains("yyyy-MM-dd"));
        assertTrue(result.errors().get(3).contains("LP must be between"));
        assertTrue(result.errors().get(4).contains("lp is not a whole number"));
    }

    @Test
    void shouldRejectTheSameChampionTwice() {
        List<ChampionSpec> twice = List.of(
                new ChampionSpec("Mage", "Lux", 1800, 50, 250, 60, 45),
                new ChampionSpec("Mage", "LUX", 1800, 50, 250, 60, 45));

        assertThrows(IllegalArgumentException.class, () -> new ChampionCatalog(twice));
    }

    @Test
    void shouldLoadTheShippedDataFilesWithoutErrors() throws IOException {
        CsvReader.Result<ChampionSpec> champions;
        try (BufferedReader reader = DataLoader.open(DataLoader.CHAMPIONS_FILE, null)) {
            champions = DataLoader.readChampions(reader);
        }
        ChampionCatalog catalog = new ChampionCatalog(champions.items());
        CsvReader.Result<Player> players;
        try (BufferedReader reader = DataLoader.open(DataLoader.PLAYERS_FILE, null)) {
            players = DataLoader.readPlayers(reader, catalog);
        }

        assertTrue(champions.errors().isEmpty());
        assertTrue(players.errors().isEmpty());
        assertEquals(12, catalog.size());
        assertEquals(12, players.items().size());
    }
}
