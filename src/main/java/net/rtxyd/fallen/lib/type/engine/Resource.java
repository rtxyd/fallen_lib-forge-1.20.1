package net.rtxyd.fallen.lib.type.engine;

import java.io.IOException;
import java.io.InputStream;

public interface Resource {
    String path();
    InputStream open() throws IOException;
    ResourceContainer container();
    default boolean isRoot() {
        return !path().contains("/");
    }
}
