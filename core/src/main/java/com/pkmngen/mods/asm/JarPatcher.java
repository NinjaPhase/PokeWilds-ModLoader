package com.pkmngen.mods.asm;

import com.pkmngen.mods.Constants;
import com.pkmngen.mods.ModLoader;
import com.pkmngen.mods.Options;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class JarPatcher extends JFrame {

    private final JProgressBar progressBar;
    private final JLabel label;
    private final String jarPath, patchPath;
    private PatchClassLoader classLoader;

    public JarPatcher() {
        super("PokeWilds - ModLoader");
        this.jarPath = ".\\app\\pokewilds.jar";
        this.patchPath = ".\\app\\pokewilds-patched.jar";

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.progressBar = new JProgressBar();
        this.label = new JLabel("Loading...");
        panel.add(progressBar);
        panel.add(label, BorderLayout.SOUTH);
        this.add(panel);

        this.pack();
        this.setLocationRelativeTo(null);
    }

    public void patch() throws ParserConfigurationException, IOException, SAXException {
        Document patchFile = getPatchFile();
        File jarPathFile = new File(jarPath);
        File patchPathFile = new File(patchPath);

        String originalDigest = digest(jarPathFile);
        if (!Constants.UNPATCHED_SHA256.equals(originalDigest)) {
            throw new RuntimeException("Version of PokéWilds is invalid, expected version " + Constants.VERSION);
        }

        String patchDigest = digest(patchPathFile);
        if (!Constants.PATCHED_SHA256.equals(patchDigest)) {
            this.setVisible(!Options.headless);

            try (JarFile jf = new JarFile(jarPath)) {
                int count = 0;
                for (Enumeration<JarEntry> entries = jf.entries(); entries.hasMoreElements(); entries.nextElement()) {
                    count++;
                }
                progressBar.setMaximum(count);
            }

            try (JarInputStream jis = new JarInputStream(new FileInputStream(jarPath))) {
                try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(patchPath), jis.getManifest())) {
                    JarEntry je;
                    while ((je = jis.getNextJarEntry()) != null) {
                        if (je.getName().equals("META-INF/MANIFEST.MF")) {
                            continue;
                        }
                        if (!je.getName().endsWith(".class")) {
                            this.label.setText("Copying file: " + je.getName());
                            jos.putNextEntry(je);
                            byte[] buffer = new byte[1024];
                            int read;
                            while((read = jis.read(buffer)) != -1) {
                                jos.write(buffer, 0, read);
                            }
                            progressBar.setValue(progressBar.getValue() + 1);
                            continue;
                        }
                        applyClassPatch(patchFile.getDocumentElement(), je, jis, jos);
                    }
                    jos.finish();
                }
            }

            this.setVisible(false);
        }
        this.classLoader = new PatchClassLoader(patchPath);

        this.dispose();
    }

    public void start() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, MalformedURLException {
        Method m = this.classLoader.loadClass("com.pkmngen.game.desktop.DesktopLauncher").getDeclaredMethod("main", String[].class);
        m.invoke(null, new Object[]{new String[]{}});
    }

    private void applyClassPatch(Element root, JarEntry je, JarInputStream jis, JarOutputStream jos) throws IOException {
        String className = je.getName().substring(0, je.getName().length() - 6).replaceAll("/", ".");
        NodeList children = root.getChildNodes();
        Node patchList = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!child.getNodeName().equals("file") && !child.hasAttributes()) {
                continue;
            }
            if (!child.getAttributes().getNamedItem("class").getTextContent().equals(className)) {
                continue;
            }
            patchList = children.item(i);
            break;
        }
        if (patchList == null) {
            this.label.setText("Copying file: " + je.getName());
            jos.putNextEntry(je);
            byte[] buffer = new byte[1024];
            int read;
            while((read = jis.read(buffer)) != -1) {
                jos.write(buffer, 0, read);
            }
            return;
        }
        this.label.setText("Applying patch to: " + je.getName());
        byte[] buffer = new byte[1024];
        int read;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((read = jis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassReader cr = new ClassReader(bos.toByteArray());
        cr.accept(new ModClassPatcher(patchList, cw), ClassReader.EXPAND_FRAMES);
        byte[] data = cw.toByteArray();

        JarEntry newEntry = new JarEntry(je.getName());
        jos.putNextEntry(newEntry);

        jos.write(data, 0, data.length);
        jos.flush();
        progressBar.setValue(progressBar.getValue() + 1);

        new File("patched/" + je.getName()).getParentFile().mkdirs();
        Files.write(new File("patched/" + je.getName()).toPath(), data, StandardOpenOption.CREATE);
    }

    public Document getPatchFile() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(ModLoader.class.getResourceAsStream("/patch.xml"));
    }

    public PatchClassLoader getClassLoader() {
        return this.classLoader;
    }

    private static String digest(File f) {
        if (!f.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] in = new byte[4096 * 1024];
            int i;
            while ((i = fis.read(in, 0, in.length)) != -1) {
                digest.update(in, 0, i);
            }
            return bytesToHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
