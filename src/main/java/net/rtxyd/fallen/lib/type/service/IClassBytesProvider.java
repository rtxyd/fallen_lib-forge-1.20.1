package net.rtxyd.fallen.lib.type.service;

import java.io.File;

@FunctionalInterface
public interface IClassBytesProvider {
    byte[] getClassBytes(File container, String path);
}