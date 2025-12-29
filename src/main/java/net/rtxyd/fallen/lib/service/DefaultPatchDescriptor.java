package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.type.service.IPatchDescriptor;

public record DefaultPatchDescriptor(String className, int targetCount) implements IPatchDescriptor {
}
