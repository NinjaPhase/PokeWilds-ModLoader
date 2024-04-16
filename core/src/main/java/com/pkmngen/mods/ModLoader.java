package com.pkmngen.mods;

import com.pkmngen.api.IPokemon;
import com.pkmngen.api.Mod;
import com.pkmngen.api.PokeWilds;
import com.pkmngen.api.events.*;
import com.pkmngen.api.factories.PokemonFactory;
import com.pkmngen.mods.asm.JarPatcher;
import com.pkmngen.mods.asm.ModClassLoader;
import com.pkmngen.mods.asm.PatchClassLoader;
import com.pkmngen.mods.events.ModEvent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModLoader {
    private static ModLoader INSTANCE;

    private final JarPatcher patcher;
    private final List<Object> mods;
    private final Map<ModEvent, List<EventHandler>> eventHandler;

    public ModLoader() {
        this.patcher = new JarPatcher();
        this.mods = new ArrayList<>();
        this.eventHandler = new HashMap<>();
    }

    public void start() throws Exception {
        patcher.patch();
        PatchClassLoader patchClassLoader = patcher.getClassLoader();

        List<URL> urlList = new ArrayList<>();
        List<String> clsNames = new ArrayList<>();
        for (File f : Objects.requireNonNull(Constants.MOD_PATH.listFiles(filter -> filter.getName().endsWith(".jar")))) {
            try (JarFile jf = new JarFile(f)) {
                for (Enumeration<JarEntry> entries = jf.entries(); entries.hasMoreElements();) {
                    JarEntry e = entries.nextElement();
                    if (e.getName().endsWith(".class")) {
                        clsNames.add(e.getName().substring(0, e.getName().length() - 6).replaceAll("/", "."));
                    }
                }
            } catch (Exception e) {
                System.out.println("error loading mod: " + e.getMessage());
            }
            urlList.add(f.toURI().toURL());
        }
        URL[] urls = urlList.toArray(new URL[0]);
        ModClassLoader modClassLoader = new ModClassLoader(urls, patchClassLoader);
        for (String str : clsNames) {
            Class<?> cls = modClassLoader.loadClass(str);
            if (!cls.isAnnotationPresent(Mod.class)) {
                continue;
            }
            Mod mod = cls.getAnnotation(Mod.class);
            loadMod(cls);
            System.out.println("Loaded mod " + mod.name() + " made by " + mod.author());
        }

        patcher.start();
    }

    @SuppressWarnings("unused")
    public void handleEvent(ModEvent event, Object[] args) {
        if (event == ModEvent.POKEMON_CREATE) {
            for (PokemonFactory factory : PokeWilds.getFactories()) {
                factory.onInitialisation((IPokemon) args[0]);
            }
            return;
        } else if (event == ModEvent.POKEMON_CREATING) {
            for (PokemonFactory factory : PokeWilds.getFactories()) {
                factory.onSetup((IPokemon) args[0]);
            }
            return;
        } else if (event == ModEvent.POKEMON_STAT_UPDATE) {
            for (PokemonFactory factory : PokeWilds.getFactories()) {
                factory.onStatChange((IPokemon) args[0]);
            }
        }

        if (this.eventHandler.containsKey(event)) {
            List<EventHandler> eventHandlers = this.eventHandler.get(event);
            for (EventHandler handler : eventHandlers) {
                try {
                    Object[] params = new Object[handler.m.getParameterCount()];
                    for (int i = 0; i < handler.m.getParameterCount(); i++) {
                        Class<?> t = handler.m.getParameterTypes()[i];
                        if (event == ModEvent.CREATE_FINISHED && t == PostloadEvent.class) {
                            params[i] = new PostloadEvent();
                        }
                    }

                    System.out.println(Arrays.toString(args));
                    System.out.println(Arrays.toString(params));
                    handler.m.invoke(handler.o, params);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadMod(Class<?> p) throws Exception {
        Object o = p.getDeclaredConstructor().newInstance();

        for (Method m : p.getDeclaredMethods()) {
            if (m.getAnnotation(Subscribe.class) == null) {
                continue;
            }
            for (Class<?> param : m.getParameterTypes()) {
                if (param == PostloadEvent.class) {
                    if (!this.eventHandler.containsKey(ModEvent.CREATE_FINISHED)) {
                        this.eventHandler.put(ModEvent.CREATE_FINISHED, new ArrayList<>());
                    }
                    this.eventHandler.get(ModEvent.CREATE_FINISHED).add(new EventHandler(o, m));
                }
            }
        }

        this.mods.add(o);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            (ModLoader.getInstance()).start();
        } catch (Exception e) {
            showErrorModal(e);
        }
    }

    public static void showErrorModal(Exception e) {
        JFrame err = new JFrame("Unexpected error");
        err.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        StringWriter stackWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stackWriter));
        JTextArea errText = new JTextArea(stackWriter.toString());
        errText.setBorder(BorderFactory.createLoweredBevelBorder());
        errText.setEditable(false);
        JPanel inner = new JPanel(new BorderLayout(10, 10));
        inner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inner.add(errText);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btn = new JButton("Close");
        btn.addActionListener((ev) -> {
            err.dispose();
        });
        actions.add(btn);
        inner.add(actions, BorderLayout.SOUTH);
        err.setContentPane(inner);
        err.setSize(640, 480);
        err.setLocationRelativeTo(null);
        err.setVisible(true);
    }

    public static ModLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModLoader();
        }
        return INSTANCE;
    }

    private static class EventHandler {
        Object o;
        Method m;

        EventHandler(Object o, Method m) {
            this.o = o;
            this.m = m;
        }
    }
}
