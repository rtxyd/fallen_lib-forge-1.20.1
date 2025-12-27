package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.type.service.IFallenPatchContext;

import java.util.*;

public class DefaultPatchContext implements IFallenPatchContext {
    private final Set<String> appliedByClass = new LinkedHashSet<>();
    private final Set<String> appliedByClassView = Collections.unmodifiableSet(appliedByClass);

    public void beginClass() {
        appliedByClass.clear();
    }

    void recordPatchEffect(String patchId) {
        appliedByClass.add(patchId);
    }

    @Override
    public Set<String> getAppliedPatches() {
        return appliedByClassView;
    }

    public void endClass() {
        appliedByClass.clear();
    }
}
