package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.Resource;
import net.rtxyd.fallen.lib.type.engine.ResourceProcessor;
import org.objectweb.asm.ClassReader;

import java.io.InputStream;

class ClassResourceProcessor implements ResourceProcessor {
    // flag to provide spamming in log if exception.
    private boolean loggerFlag = true;

    @Override
    public boolean supports(Resource r) {
        return r.path().endsWith(".class");
    }

    @Override
    public void process(Resource r, ScanContext ctx) {
        try (InputStream is = r.open()) {
            ClassView cv = new ClassView(ClassView.SKIP_ANNOTATION);
            new ClassReader(is).accept(cv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            ctx.classIndex.add(r.path().replace(".class", ""), new ClassInfo(cv.superName, cv.interfaces, cv.nestMembers));
        } catch (Exception e) {
            if (loggerFlag) {
                ResourceScanEngine.LOGGER.warn("Errors occurred during Fallen patch scanning, see trace for details");
            }
            loggerFlag = false;
            ResourceScanEngine.LOGGER.trace("Class {} failed to scan", r.path(), e);
        }
    }
}