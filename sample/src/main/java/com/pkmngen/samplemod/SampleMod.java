package com.pkmngen.samplemod;

import com.pkmngen.api.Mod;
import com.pkmngen.api.PokeWilds;
import com.pkmngen.api.events.PostloadEvent;
import com.pkmngen.api.events.Subscribe;

@Mod(name="Sample Mod", author="NinjaPhase", version={1, 0, 0})
@SuppressWarnings("unused")
public class SampleMod {

    @Subscribe
    public void onLoad(PostloadEvent e) {
        PokeWilds.addPokemonFactory(new IVPokemonFactory());
    }

}
