package com.pkmngen.api;

import com.pkmngen.api.factories.PokemonFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokeWilds {

    private static final List<PokemonFactory> FACTORIES = new ArrayList<>();

    public static void addPokemonFactory(PokemonFactory factory) {
        FACTORIES.add(factory);
    }

    public static List<PokemonFactory> getFactories() {
        return Collections.unmodifiableList(FACTORIES);
    }

    private PokeWilds() {}

}
