package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.api.IFallenPatch;
import net.rtxyd.fallen.lib.config.FallenConfig;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;
import net.rtxyd.fallen.lib.type.service.IClassBytesProvider;
import net.rtxyd.fallen.lib.util.patch.InserterType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class PatchEntryHelper {

    private final IPatchSecurityHelper securityHelper = new PatchSecurityHelperV2();

    // out parameters must not be null
    public void buildPatchEntries(FallenConfig cfg, ResourceContainer rc, List<FallenPatchEntry> outEntries, Map<String, byte[]> outStoredBytes) {
        Optional<File> contOpt = rc.asFile();
        if (contOpt.isEmpty()) {
             return;
        }
        File cont = contOpt.get();
        // in development environment.
        if (cont.isDirectory()) {
            buildEntriesInner(outEntries, outStoredBytes, cfg, cont, (f, zn) -> {
                try (InputStream isA = getClass().getClassLoader().getResourceAsStream(zn)) {
                    if (isA != null) {
                        return isA.readAllBytes();
                    }
                    try (InputStream isB = ClassLoader.getSystemClassLoader().getResourceAsStream(zn)) {
                        if (isB != null) {
                            return isB.readAllBytes();
                        }
                    }
                    return null;
                } catch (IOException e) {
                    FallenBootstrap.LOGGER.debug("Failed parsing [{}] in folder [{}]", zn, f.getName(), e);
                    return null;
                }
            });
            return;
        }

        buildEntriesInner(outEntries, outStoredBytes, cfg, cont, (jar, zn) -> {
            try (JarFile jarFile = new JarFile(jar)) {
                JarEntry jarEntry = jarFile.getJarEntry(zn);
                if (jarEntry == null) {
                    FallenBootstrap.LOGGER.warn("Warning: class file [{}] not found in [{}]", zn, jar.getName());
                    return null;
                }
                try (InputStream is = jarFile.getInputStream(jarEntry)) {
                    return is.readAllBytes();
                }
            } catch (IOException e) {
                FallenBootstrap.LOGGER.debug("Failed parsing [{}] in jarFile [{}]",zn, jar.getName(), e);
                return null;
            }
        });
    }

    private void buildEntriesInner(List<FallenPatchEntry> entries, Map<String, byte[]> restoredBytes, FallenConfig cfg, File cont, IClassBytesProvider bytesFunction) {
        int counter = 0;
        for (String className : cfg.buildClassNames()) {
            String zn = className.replace(".", "/") + ".class";
            try {
                byte[] inputBytes = bytesFunction.getClassBytes(cont, zn);
                if (inputBytes == null) {
                    continue;
                }
                ClassNode cn = new ClassNode();
                new ClassReader(inputBytes).accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                if (cn.invisibleAnnotations != null) {
                    AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(cn.invisibleAnnotations);
                    Optional<AnnotationData> opt = factory.getAnnotation("Lnet/rtxyd/fallen/lib/api/annotation/FallenPatch;");
                    if (opt.isEmpty()
                            || !securityHelper.isPatchClassSafe(cn)) {
                        continue;
                    }
                    FallenPatchEntry pEntry = this.parseAndBuild(className, opt.get(), cont);
                    entries.add(pEntry);
                    restoredBytes.put(className, inputBytes);
                    counter += 1;
                }
            } catch (Exception e) {
                FallenBootstrap.LOGGER.debug("Failed parsing fallen patch class {}", zn, e);
            }
        }
        if (counter == 0) {
            FallenBootstrap.LOGGER.warn("Empty entries: package: {}", cfg.getPackage());
        }
    }

//    @SuppressWarnings("unchecked")
    public FallenPatchEntry parseAndBuild(String className, AnnotationData data, File cont) {
        Integer pr = (Integer) data.get("priority");
        List<String> ic = data.getWithDefaut("inserters", List.of());
        Map<String, MethodInsnNode> inserterMap = new HashMap<>();
        buildInserter(inserterMap, ic, cont);
        if (inserterMap.isEmpty()) {
            FallenBootstrap.LOGGER.debug("Warning: inserters of {} is empty. May be unstandard patch.", className);
        }
        if (pr == null) {
            pr = 1000;
        }
        FallenPatchEntry.Targets targets;
        AnnotationData tarData = (AnnotationData) data.get("targets");
        if (tarData == null) {
            targets = new FallenPatchEntry.Targets();
        } else {
            targets = new FallenPatchEntry.Targets();
            List<String> exact = tarData.getWithDefaut("exact", List.of());
            List<String> subclass = tarData.getWithDefaut("subclass", List.of());
            if (containsForbidden(exact) || containsForbidden(subclass)) {
                FallenBootstrap.LOGGER.warn("Warning: {} targets mc or forge class, " +
                        "it is not supported for now. May support in the future.", className);
            } else {
                targets = new FallenPatchEntry.Targets().from(exact, subclass);
            }
        }
        return buildEntry(className, pr, targets, inserterMap);
    }

    private void buildInserter(Map<String, MethodInsnNode> inserterMap, List<String> classNames, File cont) {
        for (String className : classNames) {
            parseAndBuildInserter(inserterMap, cont, className);
        }
    }

    private void parseAndBuildInserter(Map<String, MethodInsnNode> inserterMap, File cont, String className) {
        String internal = className.replace(".", "/");
        String zn = internal + ".class";
        // in development environment.
        if (cont.isDirectory()) {
            ClassNode cn = null;
            try (InputStream isA = getClass().getClassLoader().getResourceAsStream(zn)) {
                if (isA != null) {
                    cn = new ClassNode();
                    new ClassReader(isA).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                try (InputStream isB = ClassLoader.getSystemClassLoader().getResourceAsStream(zn)) {
                    if (isB != null) {
                        cn = new ClassNode();
                        new ClassReader(isB).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    }
                }
                if (cn == null) {
                    FallenBootstrap.LOGGER.debug("Failed parsing [{}] in folder [{}]", zn, cont.getName());
                    return;
                }
                buildInserterInner(inserterMap, cn, internal, className);
            } catch (IOException e) {
                FallenBootstrap.LOGGER.debug("Failed parsing [{}] in folder [{}]", zn, cont.getName(), e);
            }
            return;
        }
        try (JarFile jarFile = new JarFile(cont)) {
            JarEntry jarEntry = jarFile.getJarEntry(zn);
            if (jarEntry == null) {
                FallenBootstrap.LOGGER.warn("Warning: inserter class file [{}] not found in [{}]", zn, cont.getName());
                return;
            }
            try (InputStream is = jarFile.getInputStream(jarEntry)) {
                ClassNode cn = new ClassNode();
                new ClassReader(is).accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
                buildInserterInner(inserterMap, cn, internal, className);
            }
        } catch (IOException e) {
            FallenBootstrap.LOGGER.debug("Failed parsing [{}] in jarFile [{}]",zn, cont.getName(), e);
        }
    }

    private void buildInserterInner(Map<String, MethodInsnNode> inserterMap, ClassNode cn, String internalName, String qualifiedName) {
        String sStandard = InserterType.standardStarter();
        String fInserter = IFallenPatch.fallenInserterInternalName();
        for (MethodNode mn : cn.methods) {
            if (mn.invisibleAnnotations != null) {
                AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(mn.invisibleAnnotations);
                Optional<AnnotationData> opt = factory.getAnnotation(fInserter);
                if (opt.isEmpty()) {
                    continue;
                }
                AnnotationData data = opt.get();
                String type = data.getWithDefaut("type", InserterType.STANDARD.name());
                if ((mn.access & Opcodes.ACC_PUBLIC) != 0
                        && (mn.access & Opcodes.ACC_STATIC) != 0) {
                    if (mn.desc.startsWith(sStandard)) {
                        if (type.equals(InserterType.STANDARD.name()) && mn.desc.endsWith("Ljava/lang/Object;")) {
                            inserterMap.put(qualifiedName + "." + mn.name + "." + InserterType.STANDARD.ordinal(),
                                    new MethodInsnNode(Opcodes.INVOKESTATIC, internalName, mn.name, InserterType.STANDARD.desc()));
                        } else if (type.equals(InserterType.STANDARD_VOID.name()) && mn.desc.endsWith("V")) {
                            inserterMap.put(qualifiedName + "." + mn.name + "." + InserterType.STANDARD_VOID.ordinal(),
                                    new MethodInsnNode(Opcodes.INVOKESTATIC, internalName, mn.name, InserterType.STANDARD_VOID.desc()));
                        }
                    }
                }
            }
        }
    }

    private FallenPatchEntry buildEntry(String className, int priority, FallenPatchEntry.Targets targets, Map<String, MethodInsnNode> inserters) {
        return new FallenPatchEntry(
                className,
                targets.computeTargeter(),
                priority,
                targets,
                inserters);
    }

    public boolean containsForbidden(List<String> targets) {
        for (String s : targets) {
            if (s.startsWith("net.minecraft.")
                    || s.startsWith("net.minecraftforge.")) {
                return true;
            }
        }
        return false;
    }
}
