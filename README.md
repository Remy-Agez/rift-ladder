# Rift Ladder

A console ranked ladder inspired by League of Legends. Players each main a champion; a ranked game is a turn-based 1v1 duel between their champions, and the result moves LP, promotes and demotes players through the tiers (Iron to Challenger), and applies high-elo inactivity decay.

## How to run

Requirements: Java 17 or newer. Maven is not needed, the project ships the Maven wrapper.

```bash
# Windows
mvnw.cmd package
# Linux / macOS
./mvnw package

java -jar target/rift-ladder.jar
```

By default the data files bundled in `src/main/resources/data/` are used. To load other files, pass a folder that contains `champions.csv` and `players.csv`:

```bash
java -jar target/rift-ladder.jar path/to/folder
```

## How to run the tests

```bash
mvnw.cmd test        # Windows
./mvnw test          # Linux / macOS
```

54 JUnit 5 tests cover normal cases, edge cases (exact thresholds, lowest and highest tier, draws, no games played) and invalid input (bad CSV lines, unknown players, impossible stats).

## Features

1. **Leaderboard**: players sorted by tier, then LP, then name.
2. **Champion list**: the champions loaded from `champions.csv`.
3. **Player profile**: rank, LP, record, win rate and last game date.
4. **Ranked duel**: two players fight with their main champion. The challenger strikes first, then the champions alternate attacks until one falls (or 100 turns, which is a draw). The winner gains 25 LP, plus 5 LP for each tier the loser is above them; the loser loses 20 LP.
5. **Win rate by role**: total wins / total games for Assassins, Mages and Tanks.
6. **Inactivity decay**: Diamond players and above who have not played for more than 28 days lose 50 LP. Each run simulates one day.

### Game rules

| Rule | Value |
|---|---|
| Damage after resistances | `damage * 100 / (100 + resistance)` (armor for physical, magic resist for magic) |
| Assassin | physical, attack damage, x1.5 on a target below 30 % health |
| Mage | magic, ability power, so armor is useless against it |
| Tank | physical, attack damage + 5 % of its max health, starts each duel with a shield of 10 % max health |
| Promotion | 100 LP: next tier, the extra LP is kept |
| Demotion | below 0 LP: previous tier at 75 LP (Iron stays at 0) |
| Challenger | no LP ceiling |

## Project structure

```
src/main/java/com/riftladder/
  Application.java          entry point: loads the data, builds the services, starts the menu
  model/                    the game objects and their rules
    Champion.java           abstract base class of all champions
    Assassin, Mage, Tank    the three roles
    ChampionSpec.java       one line of champions.csv, builds new champions (factory)
    Player.java             a ranked player: LP, rank, record
    Rank.java, DamageType.java   enums
  service/
    ChampionCatalog.java    all champions, found by name
    DuelService.java        runs a duel
    LadderService.java      ranked games, LP, leaderboard, statistics, decay
  exception/
    InvalidDataException.java     a data line cannot be read
    PlayerNotFoundException.java  a name that is not on the ladder
  utils/
    LineParser.java         interface: one CSV line -> one object
    CsvReader.java          generic CSV reading with error collection
    DataLoader.java         the two data files and their columns
  ui/
    ConsoleMenu.java        console input and output only
src/main/resources/data/    champions.csv, players.csv
```

## Main classes

- `Champion`: holds the stats and health, and the shared attack sequence `attack()`: the role's own formula (`computeDamage`), then the target's resistances and passive (`takeDamage`, `absorb`).
- `Player`: the only place where LP and rank change, so the "0 to 99 LP below Challenger" rule always holds.
- `DuelService`: alternates attacks between two champions and returns a `DuelResult` with a turn-by-turn log.
- `LadderService`: business rules of the ladder (LP gain with upset bonus, leaderboard, win rate by role, decay).
- `CsvReader` / `DataLoader`: read the files at startup and turn every bad line into a clear message.

## Object-oriented design

### Inheritance and abstract class

`Champion` is **abstract**: a champion without a role does not exist in the game, and every role must provide its damage formula. It declares `computeDamage`, `getDamageType` and `getRole` as abstract methods, while the common logic (stats validation, resistances, health) is written once in the base class. `Assassin`, `Mage` and `Tank` extend it.

An abstract class was chosen rather than an interface because the roles share **state** (name, health, stats) and **code** (`takeDamage`, `resetForDuel`), which an interface cannot hold.

### Polymorphism

`DuelService.fight` only knows `Champion`. When it calls `attacker.attack(defender)`, Java runs the right `computeDamage` for the real role at runtime. The tank's shield works the same way: `absorb` does nothing in `Champion` and is overridden in `Tank`. Adding a new role (for example a Marksman) means writing one new class and one line in `ChampionSpec.create()`; the duel and ladder code does not change.

`attack` is declared `final` (template method): every role goes through the same steps, only the formula changes.

### Interface

`LineParser<T>` is a generic functional interface: "turn the columns of one CSV line into a T". `CsvReader.read` uses it to read any file without knowing what is inside; the champion and player parsers are passed as a method reference and a lambda.

## Custom exceptions and error handling

- `InvalidDataException` (**checked**): a line of a data file is wrong (missing columns, text instead of a number, unknown role, rank or champion, negative stats, invalid date). It is checked so that the reading code is forced to decide what to do: here the line is skipped and reported with its line number, and the rest of the file is still loaded.
- `PlayerNotFoundException` (**unchecked**): the user typed a name that does not exist. The menu catches it, shows the message and comes back to the menu.
- Standard exceptions: `IllegalArgumentException` for invalid values (negative stats, a champion attacking itself, a player against themselves), `NumberFormatException` and `DateTimeParseException` caught and converted into `InvalidDataException`, `IOException` when a file is missing (the program stops with a clear message).

## Data loading

At startup `Application` reads `champions.csv`, then `players.csv` (each player's champion must exist in the catalog). Files are UTF-8, comma-separated, with one header line; blank lines and lines starting with `#` are ignored.

```
role,name,maxHealth,attackDamage,abilityPower,armor,magicResist
Assassin,Katarina,2050,215,0,70,50

name,mainChampion,rank,lp,wins,losses,lastPlayed
RiftWalker,Katarina,MASTER,64,212,180,2026-11-08
```

## Technical decisions

1. **No randomness in duels.** The result only depends on the stats and who strikes first, so duels are reproducible and can be tested with exact values.
2. **Champion recipes instead of shared champions.** The catalog stores `ChampionSpec` records and creates a fresh `Champion` for every duel. Two players who main the same champion never share one object, and a duel never starts with leftover damage or a used shield.
3. **A bad data line does not stop the program.** It is reported and skipped; only a missing file stops the startup. A real ladder should not go down because of one corrupted line.
4. **LP changes only through `Player` methods.** No setter for rank or LP, so promotions and demotions cannot be skipped by mistake.
5. **Win rate by role from totals.** It is total wins / total games, not an average of the players' win rates, so a player with 3 games does not weigh as much as one with 300.

## SOLID

- **Single responsibility**: reading files (`utils`), game rules (`model`, `service`) and the console (`ui`) are separate. `Application` only wires them together.
- **Open/closed**: a new role extends `Champion` without changing `DuelService` or `LadderService`.
- **Liskov substitution**: any `Champion` subclass can be used wherever a `Champion` is expected; the duel works the same way.
- **Interface segregation**: `LineParser` has a single method.
- **Dependency inversion**: `LadderService` receives its `ChampionCatalog` and `DuelService` in its constructor, and `ConsoleMenu` receives its `Scanner` and `PrintStream`, so they can be replaced in tests.

## Java features used

Collections (`List`, `Map`, `LinkedHashMap`, `TreeMap`), streams (`sorted`, `groupingBy`, `filter`, `mapToInt`), `Optional`, enums with methods, records, generics, `LocalDate` and `ChronoUnit`, switch expressions, text blocks in tests, try-with-resources.
