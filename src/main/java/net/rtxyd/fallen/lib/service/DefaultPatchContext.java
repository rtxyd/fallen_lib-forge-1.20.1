package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.type.service.IFallenPatchContext;

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
    public Set<String> currentClassAppliedPatches() {
        return appliedByClassView;
    }

    public void endClass() {
        appliedByClass.clear();
    }
}
