package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.ResourceProcessor;
import net.rtxyd.fallen.lib.type.engine.ResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceScanEngine {

    protected static final Logger LOGGER = LoggerFactory.getLogger("fallen.engine");

    private final Map<Class<?>, ResourceProcessor> processors = new LinkedHashMap<>();

    public void register(ResourceProcessor processor) {
        processors.put(processor.getClass(), processor);
    }

    private void defaultInitialize() {
        register(new FallenConfigProcessor());
        register(new ClassResourceProcessor());
    }

    public ScanContext scan(List<ResourceScanner> scanners, boolean isDefault) throws IOException {
        if (isDefault) {
            defaultInitialize();
        }

        ScanContext ctx = new ScanContext();
        if (scanners == null) {
            return ctx;
        }
        for (ResourceScanner scanner : scanners) {
            scanner.scan(resource -> {
                for (ResourceProcessor p : processors.values()) {
                    if (p.supports(resource)) {
                        p.process(resource, ctx);
                    }
                }
            });
        }

        return ctx;
    }


}
