package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.type.service.IPatchDescriptor;

public record DefaultPatchDescriptor(String className, int targetCount) implements IPatchDescriptor {
}
