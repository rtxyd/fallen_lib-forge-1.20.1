package net.rtxyd.fallen.lib.type.engine;

import java.io.File;
import java.util.Optional;

public interface ResourceContainer {
    String id();

    Optional<File> asFile();

    String path();
}
