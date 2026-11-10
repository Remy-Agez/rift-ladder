package com.riftladder.service;

import com.riftladder.model.Champion;
import com.riftladder.model.ChampionSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Every champion that exists in the game, found by name (case does not matter). */
public class ChampionCatalog {

    private final Map<String, ChampionSpec> specs = new LinkedHashMap<>();

    public ChampionCatalog(List<ChampionSpec> champions) {
        champions.forEach(this::add);
    }

    private void add(ChampionSpec spec) {
        if (specs.putIfAbsent(key(spec.name()), spec) != null) {
            throw new IllegalArgumentException("Champion listed twice: " + spec.name());
        }
    }

    private static String key(String name) {
        return name.trim().toLowerCase();
    }

    public Optional<ChampionSpec> find(String name) {
        return Optional.ofNullable(specs.get(key(name)));
    }

    /** A fresh champion ready for a duel. IllegalArgumentException if the name is unknown. */
    public Champion create(String name) {
        return find(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown champion: " + name))
                .create();
    }

    /** In file order. */
    public List<ChampionSpec> all() {
        return new ArrayList<>(specs.values());
    }

    public int size() {
        return specs.size();
    }
}
