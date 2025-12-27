package net.rtxyd.fallen_lib.service;

import cpw.mods.modlauncher.api.IEnvironment;
import net.rtxyd.fallen_lib.engine.*;
import net.rtxyd.fallen_lib.type.engine.ResourceScanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InitializeHelper {

    private final IEnvironment environment;
    private final ResourceScanEngine engine;
    private final List<ResourceScanner> scanners = new ArrayList<>();
    private ScanContext ctx;

    private Path gamePath;
    private File modsDir;

    public InitializeHelper(IEnvironment environment) {
        this.environment = environment;
        this.engine = new ResourceScanEngine();

        checkEnvironment();
        resolveGamePath();
        resolveModsDir();
    }

    private void checkEnvironment() {
        if (environment == null) {
            throw new IllegalStateException("Not a game bootstrap!");
        }
    }

    private void resolveGamePath() {
        gamePath = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(null);
        if (gamePath == null) {
            throw new IllegalStateException("Game path not found!");
        }
    }

    private void resolveModsDir() {
        modsDir = gamePath.resolve("mods").toFile();
    }

    File getModsDir() {
        return modsDir;
    }

    void collectScanners() {
        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] files = modsDir.listFiles((f, n) -> n.endsWith(".jar"));
            if (files != null) {
                for (File jar : files) {
                    scanners.add(new JarScanner(jar));
                }
            } else {
                FallenBootstrap.LOGGER.error("Mods directory is invalid: {}", modsDir.getAbsolutePath());
            }
        }

        if (isIDEATestEnvironment(environment)) {
            for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
                File f = new File(path);
                if (f.isDirectory()) scanners.add(new DirectoryScanner(f));
                else if (f.getName().endsWith(".jar")) scanners.add(new JarScanner(f));
            }
        }
    }

    void scanResources() throws IOException {
        FallenBootstrap.LOGGER.info("Prepare to scan resources.");
        ctx = engine.scan(scanners, true);
        FallenBootstrap.LOGGER.info("End scanning.");
    }

    void registerPatches(FallenPatchRegistry registry) {
        FallenBootstrap.LOGGER.info("Prepare to sort fallen patches.");
        List<FallenPatchEntry> entries = new ArrayList<>();
        FallenBootstrap.LOGGER.info("Sorted {} patch entries.", entries.size());

        ClassIndex index = ctx.classIndex;
        Set<String> allClasses = index.getAllClasses();
        ctx.configContainers().forEach(((config, container) -> {
            new PatchEntryHelper().buildPatchEntries(config, container, entries, registry.classBytes);
        }));

        for (FallenPatchEntry e : entries) {
            if (e.isEmpty()) continue;
            for (String className : allClasses) {
                if (e.matches(className, index)) {
                    registry.register(className, e);
                }
            }
        }

        FallenBootstrap.LOGGER.info("End registering fallen patch entries.");
    }

    ScanContext getScanContext() {
        return ctx;
    }

    List<ResourceScanner> getScanners() {
        return scanners;
    }

    ResourceScanEngine getEngine() {
        return engine;
    }

    private boolean isIDEATestEnvironment(IEnvironment environment) {
        String cp = System.getProperty("java.class.path");
//        String args = System.getProperty("jvmArgs");
        String target = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("");
        return (target.equals("forgeclientuserdev") || target.equals("forgeserveruserdev")) && cp.contains("build/classes/java".replace("/", File.separator));
    }
}