package com.pkmngen.api.factories;

import com.pkmngen.api.IPokemon;

public interface PokemonFactory {

    void onSetup(IPokemon pokemon);
    void onInitialisation(IPokemon pokemon);
    void onStatChange(IPokemon pokemon);

}
