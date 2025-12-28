package net.rtxyd.fallen_lib.engine;

import net.rtxyd.fallen_lib.type.engine.ResourceConsumer;
import net.rtxyd.fallen_lib.type.engine.ResourceContainer;
import net.rtxyd.fallen_lib.type.engine.ResourceScanner;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarScanner implements ResourceScanner {
    private final File jarFile;

    public JarScanner(File jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    public void scan(ResourceConsumer consumer) {
        try (JarFile jar = new JarFile(jarFile)) {
            ResourceContainer container = new JarContainer(jarFile);
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                // to prevent spamming, here is no try-catch
                consumer.accept(new JarResource(container, jar, entry));
            }
        } catch (IOException e) {
            ResourceScanEngine.LOGGER.warn("Warning: can't open: [{}]", jarFile.getAbsolutePath());
        }
    }
}