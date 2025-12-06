package net.rtxyd.fallen_dev_helper;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import net.rtxyd.fallen_dev_helper.service.FallenTransformer;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import cpw.mods.modlauncher.api.ITransformer.Target;
import org.slf4j.LoggerFactory;

public class FallenTransformerService implements ITransformationService {
    private final Map<String, ClassInfo> classHierarchy = new HashMap<>();
    private final Map<String, ClassInfo> classesToCheck = new HashMap<>();
    private final Set<Target> classTargets = new HashSet<>();
    private final Set<String> mainClasses = new HashSet<>();
    private final Set<File> jarFiles = new HashSet<>();
    private Path fallenPath;
    public static final Logger LOGGER = LoggerFactory.getLogger("fallen");
    public static final String BLACKLIST_NAME = "fallen_ref_blacklist.txt";
    public static final String TARGETS_NAME = "fallen_ref_targets.txt";
    public static final String MAINS_NAME = "fallen_ref_mains.txt";

    @Override
    public String name() {
        return "fallen";
    }

    @Override
    public void initialize(IEnvironment environment) {
        // check environment for scanning.
        final String superClass = "dev/shadowsoffire/apotheosis/adventure/socket/gem/bonus/GemBonus";

        Optional<Path> gamePath = environment.getProperty(IEnvironment.Keys.GAMEDIR.get());
        // create files, clear references
        gamePath.ifPresentOrElse(path -> {
                    fallenPath = path.resolve("fallen");
                    if (!Files.exists(fallenPath)) {
                        try {
                            Files.createDirectory(fallenPath);
                            Files.writeString(fallenPath.resolve("fallen_ref_blacklist.txt"), "");
                            Files.writeString(fallenPath.resolve("fallen_ref_targets.txt"), "");
                            Files.writeString(fallenPath.resolve("fallen_ref_mains.txt"), "");
                        } catch (IOException e) {
                            LOGGER.error("Failed to create fallen dir", e);
                        }
                    }
                }, () -> LOGGER.error("Could not find game path"));
        // find target classes
        if (classHierarchy.isEmpty()) {
            LOGGER.info("Scanning mods");
            scanModsDirFilteredBySuperClass(classHierarchy, jarFiles, superClass);
            if (isIDEATestEnvironment(environment)) {
                LOGGER.info("IDEA environment detected, scanning mods from classpath, this takes longer.");
                scanClasspathFilteredBySuperClass(classHierarchy, jarFiles, mainClasses, superClass);
            }
        }

        for (File file : jarFiles) {
            scanJar(file, classesToCheck);
        }

        for (String className : classesToCheck.keySet()) {
            if (isSubclassOf(className, superClass)) {
                mainClasses.add(className);
            }
        }
        for (String className : mainClasses) {
            classTargets.add(ITransformer.Target.targetClass(className));
            ClassInfo info = classHierarchy.get(className);
            if (info != null) {
                classTargets.addAll(info.innerClasses.stream().map(i -> Target.targetClass(i.name)).collect(Collectors.toSet()));
            }
        }
        LOGGER.info("Found {} classes", classTargets.size());
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {
    }

    @Override
    public @NotNull List<ITransformer> transformers() {
        LOGGER.info("Creating transformer.");
        return List.of(new FallenTransformer(classTargets, fallenPath));
    }

    private void scanClasspathFilteredBySuperClass(Map<String, ClassInfo> outClassInfo, Set<File> outJars, Set<String> outClasses, String superClass) {
        String classpath = System.getProperty("java.class.path");
        String projectPath = new File(System.getProperty("user.dir")).getParent();
        for (String path : classpath.split(File.pathSeparator)) {
            if (path.startsWith(projectPath) || path.contains("forge_gradle")) {
                File f = new File(path);
                if (f.isDirectory()) {
                    scanDirFilteredBySuperClass(f, "", outClassInfo, outClasses, superClass);
                } else if (f.getName().endsWith(".jar")) {
                    scanJarOutJarsContainSubClass(f, outClassInfo, outJars, superClass);
                }
            }
        }
    }

    private void scanModsDirFilteredBySuperClass(Map<String, ClassInfo> outClassInfo, Set<File> outJars, String superClass) {
        File modsDir = new File(System.getProperty("user.dir"), "mods");
        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (files == null) return;

            for (File jarFile : files) {
                scanJarOutJarsContainSubClass(jarFile, outClassInfo, outJars, superClass);
            }
        }
    }

    private void scanDirFilteredBySuperClass(File dir, String pkg, Map<String, ClassInfo> out, Set<String> outClasses, String superClass) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                scanDirFilteredBySuperClass(file, pkg + file.getName() + ".", out, outClasses, superClass);
            } else if (file.getName().endsWith(".class")) {
                String className = (pkg + file.getName()).replace(".class", "").replace('.', '/');
                try (InputStream is = file.toURI().toURL().openStream()) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(is).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    out.put(className, new ClassInfo(cn.superName, cn.interfaces, cn.innerClasses));
                    if (isSubclassOf(className, superClass)) {
                        outClasses.add(className);
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    private void scanJarOutJarsContainSubClass(File jarFile, Map<String, ClassInfo> outCIMap, Set<File> outJars, String superClass) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            boolean shouldAddJar = false;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;

                String className = entry.getName().replace(".class", "");
                try (InputStream is = jar.getInputStream(entry)) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(is).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    outCIMap.put(className, new ClassInfo(cn.superName, cn.interfaces, cn.innerClasses));
                    if (!shouldAddJar && isSubclassOf(className, superClass)) {
                        outJars.add(jarFile);
                    }
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private void scanDir(File dir, String pkg, Map<String, ClassInfo> out) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                scanDir(file, pkg + file.getName() + ".", out);
            } else if (file.getName().endsWith(".class")) {
                String className = (pkg + file.getName()).replace(".class", "").replace('.', '/');
                try (InputStream is = file.toURI().toURL().openStream()) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(is).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    out.put(className, new ClassInfo(cn.superName, cn.interfaces, cn.innerClasses));
                } catch (IOException ignored) {}
            }
        }
    }

    private void scanJar(File jarFile, Map<String, ClassInfo> out) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;

                String className = entry.getName().replace(".class", "");
                try (InputStream is = jar.getInputStream(entry)) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(is).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    out.put(className, new ClassInfo(cn.superName, cn.interfaces, cn.innerClasses));
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    // inheritance check
    private boolean isSubclassOf(String className, String targetSuper) {
        String current = className;
        Set<String> visited = new HashSet<>();
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            ClassInfo info = classHierarchy.get(current);
            if (info == null) break;
            if (info.superName != null && info.superName.equals(targetSuper)) return true;
            for (String iface : info.interfaces) {
                if (iface.equals(targetSuper)) return true;
            }
            // keep bottom-up sourcing
            current = info.superName;
        }
        return false;
    }

    private static class ClassInfo {
        final String superName;
        final List<String> interfaces;
        final List<InnerClassNode> innerClasses;

        ClassInfo(String superName, List<String> interfaces, List<InnerClassNode> innerClasses) {
            this.superName = superName;
            this.interfaces = interfaces != null ? interfaces : Collections.emptyList();
            this.innerClasses = innerClasses != null ? innerClasses : Collections.emptyList();
        }
    }

    private boolean isIDEATestEnvironment(IEnvironment environment) {
        String cp = System.getProperty("java.class.path");
        Optional<String> targetOp = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get());
        String target = "";
        if (targetOp.isPresent()) {
            target = targetOp.get();
        }
        return (target.equals("forgeclientuserdev") || target.equals("forgeserveruserdev")) && cp.contains("build/classes/java".replace("/", File.separator));
    }
}
