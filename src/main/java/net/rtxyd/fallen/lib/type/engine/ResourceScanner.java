package net.rtxyd.fallen.lib.type.engine;

import java.io.IOException;

public interface ResourceScanner {
    void scan(ResourceConsumer consumer) throws IOException;
}
