package com.pkmngen.samplemod;

import com.pkmngen.api.IPokemon;
import com.pkmngen.api.factories.PokemonFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IVPokemonFactory implements PokemonFactory {

    @Override
    public void onSetup(IPokemon pokemon) {
        if (!pokemon.getExtraData().containsKey("ivs")) {
            int[] ivs = new int[]{
                    ThreadLocalRandom.current().nextInt(0, 16), ThreadLocalRandom.current().nextInt(0, 16),
                    ThreadLocalRandom.current().nextInt(0, 16), ThreadLocalRandom.current().nextInt(0, 16),
                    };
            ArrayList<String> ivList = new ArrayList<>();
            for (int i : ivs) {
                ivList.add(Integer.toString(i));
            }

            pokemon.getExtraData().put("ivs", ivList);
        }
    }

    @Override
    public void onInitialisation(IPokemon pokemon) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStatChange(IPokemon pokemon) {
        List<String> ivStr = (List<String>) pokemon.getExtraData().get("ivs");
        int[] ivs = new int[5];
        for (int i = 0; i < ivStr.size(); i++) {
            ivs[i + 1] = Integer.parseInt(ivStr.get(i));
        }
        for (int i = 0; i < 4; i++) {
            ivs[0] = ivs[0] | ((ivs[4 - i] & 1) << i);
        }

        pokemon.getMaxStats().put("hp", calculateHealth(
                pokemon.getBaseStats().get("hp"),
                ivs[0], 0, pokemon.getLevel()
        ));
        pokemon.getMaxStats().put("attack", calculateStat(
                pokemon.getBaseStats().get("attack"),
                ivs[1], 0, pokemon.getLevel()
        ));
        pokemon.getMaxStats().put("defense", calculateStat(
                pokemon.getBaseStats().get("defense"),
                ivs[2], 0, pokemon.getLevel()
        ));
        pokemon.getMaxStats().put("specialAtk", calculateStat(
                pokemon.getBaseStats().get("specialAtk"),
                ivs[3], 0, pokemon.getLevel()
        ));
        pokemon.getMaxStats().put("specialDef", calculateStat(
                pokemon.getBaseStats().get("specialDef"),
                ivs[3], 0, pokemon.getLevel()
        ));
        pokemon.getMaxStats().put("speed", calculateStat(
                pokemon.getBaseStats().get("speed"),
                ivs[4], 0, pokemon.getLevel()
        ));
    }

    private static int calculateHealth(int base, int iv, int ev, int level) {
        int a = ((base + iv) * 2 + Math.floorDiv((int)Math.ceil(Math.sqrt(ev)), 4)) * level;
        return Math.floorDiv(a, 100) + level + 10;
    }

    private static int calculateStat(int base, int iv, int ev, int level) {
        int a = ((base + iv) * 2 + Math.floorDiv((int)Math.ceil(Math.sqrt(ev)), 4)) * level;
        return Math.floorDiv(a, 100) + 5;
    }
}
