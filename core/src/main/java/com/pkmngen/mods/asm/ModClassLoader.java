package com.pkmngen.mods.asm;

import java.net.URL;
import java.net.URLClassLoader;

public class ModClassLoader extends URLClassLoader {

    public ModClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

}
