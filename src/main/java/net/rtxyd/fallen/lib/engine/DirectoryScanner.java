package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.ResourceConsumer;
import net.rtxyd.fallen.lib.type.engine.ResourceScanner;

import java.io.File;
import java.io.IOException;

public class DirectoryScanner implements ResourceScanner {
    private final File root;

    public DirectoryScanner(File root) {
        this.root = root;
    }

    @Override
    public void scan(ResourceConsumer consumer) {
        try {
            walk(root, "", consumer);
        } catch (Exception e) {
            // if it goes here, the check the code inside consumer.
            ResourceScanEngine.LOGGER.error("Unexpected: Failed scanning directory: {}", root.getAbsolutePath());
        }
    }

    private void walk(File dir, String path, ResourceConsumer consumer) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            ResourceScanEngine.LOGGER.error("Directory is invalid: {}", dir.getAbsolutePath());
            return;
        }

        for (File f : files) {
            String p = path + f.getName();
            if (f.isDirectory()) {
                walk(f, p + "/", consumer);
            } else {
                consumer.accept(new FileResource(dir, f, p));
            }
        }
    }
}
