package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.type.service.IFallenPatchCtorContext;
import net.rtxyd.fallen_lib.type.service.IPatchDescriptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DefaultPatchCtorContext implements IFallenPatchCtorContext {
    private IPatchDescriptor current;
    private final Set<String> patchSet;

    public DefaultPatchCtorContext(Set<String> patches) {
        patchSet = Collections.unmodifiableSet(patches);
    }

    public Set<String> getPatchList() {
        return patchSet;
    }

    @Override
    public IPatchDescriptor currentPatch() {
        if (current == null) {
            throw new IllegalStateException("currentPatch() called outside of patch construction");
        }
        return current;
    }

    void setCurrent(IPatchDescriptor desc) {
        current = desc;
    }
}
