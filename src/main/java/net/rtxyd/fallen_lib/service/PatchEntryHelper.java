package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.config.FallenConfig;
import net.rtxyd.fallen_lib.type.engine.ResourceContainer;
import net.rtxyd.fallen_lib.type.service.IClassBytesProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
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
            buildInner(outEntries, outStoredBytes, cfg, cont, (f, zn) -> {
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

        buildInner(outEntries, outStoredBytes, cfg, cont, (jar, zn) -> {
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

    private void buildInner(List<FallenPatchEntry> entries, Map<String, byte[]> restoredBytes, FallenConfig cfg, File cont, IClassBytesProvider bytesFunction) {
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

                if (cn.visibleAnnotations != null) {
                    AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(cn.visibleAnnotations);
                    Optional<AnnotationData> opt = factory.getAnnotation("Lnet/rtxyd/fallen_lib/api/annotation/FallenPatch;");
                    if (opt.isEmpty()
                            || !securityHelper.isPatchClassSafe(cn)) {
                        continue;
                    }
                    FallenPatchEntry pEntry = this.parseAndBuild(className, opt.get());
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
    public FallenPatchEntry parseAndBuild(String path, AnnotationData data) {
        Integer pr = (Integer) data.get("priority");
        String className = path.replace("/", ".").replaceAll("\\.class$", "");
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
        return buildEntry(className, pr, targets);
    }

    private FallenPatchEntry buildEntry(String className, int priority, FallenPatchEntry.Targets targets) {
        return new FallenPatchEntry(
                className,
                targets.computeTargeter(),
                priority,
                targets);
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
