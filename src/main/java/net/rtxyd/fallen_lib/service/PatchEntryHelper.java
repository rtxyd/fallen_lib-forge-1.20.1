package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.config.FallenConfig;
import net.rtxyd.fallen_lib.type.engine.ResourceContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class PatchEntryHelper {

    private final IPatchSecurityHelper securityHelper = new PatchSecurityHelperV2();

    // out parameters must not be null
    public void buildPatchEntries(FallenConfig cfg, ResourceContainer rc, List<FallenPatchEntry> outEntries, Map<String, byte[]> outRestoredBytes) {
        Optional<File> contOpt = rc.asFile();
        if (contOpt.isEmpty()) {
             return;
        }
        File cont = contOpt.get();
        if (cont.isDirectory()) {
            buildInner(outEntries, outRestoredBytes, cfg, cont, (f, zn) -> {
                InputStream is = getClass().getClassLoader().getResourceAsStream(zn);
                if (is == null) {
                    is = ClassLoader.getSystemClassLoader().getResourceAsStream(zn);
                }
                return is;
            });
            return;
        }

        buildInner(outEntries, outRestoredBytes, cfg, cont, (jar, zn) -> {
            try (JarFile jarFile = new JarFile(jar)){
                JarEntry jarEntry = jarFile.getJarEntry(zn);
                return jarFile.getInputStream(jarEntry);
            } catch (IOException e) {
                FallenBootstrap.LOGGER.error("Failed parsing jarFile {}: {}", jar.getName(), e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void buildInner(List<FallenPatchEntry> entries, Map<String, byte[]> restoredBytes, FallenConfig cfg, File cont, BiFunction<File, String, InputStream> isFunction) {
        int counter = 0;
        String zn = "";
        for (String className : cfg.buildClassNames()) {
            zn = className.replace(".", "/") + ".class";
            try (InputStream is = isFunction.apply(cont, zn)) {
                byte[] inputBytes = is.readAllBytes();
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
            } catch (IOException e) {
                FallenBootstrap.LOGGER.error("Failed parsing fallen patch class {} : {}", zn, e.getMessage());
            }
        }
        if (counter == 0) {
            FallenBootstrap.LOGGER.warn("Empty entries: package: {}, class: {}", cfg.getPackage(), zn);
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
                FallenBootstrap.LOGGER.warn("Warning: {} targets mc or forge class, it is not supported for now. May support in the future.", className);
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
