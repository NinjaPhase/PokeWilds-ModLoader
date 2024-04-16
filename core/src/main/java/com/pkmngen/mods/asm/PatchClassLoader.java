package com.pkmngen.mods.asm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class PatchClassLoader extends URLClassLoader {

    public PatchClassLoader(String jarPath) throws MalformedURLException {
        super(new URL[]{new File(jarPath).toURI().toURL()});
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            return ClassLoader.getSystemClassLoader().loadClass(name);
        }
    }
}
