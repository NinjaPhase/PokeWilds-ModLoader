package com.pkmngen.api;

import java.util.Map;

public interface IPokemon {
    Map<String, Object> getExtraData();
    Map<String, Integer> getMaxStats();
    Map<String, Integer> getBaseStats();
    void setShiny(boolean shiny);
    int getLevel();
}
