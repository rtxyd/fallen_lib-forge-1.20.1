package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.type.service.IFallenPatchContext;
import net.rtxyd.fallen.lib.util.patch.InserterKey;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.*;

public class DefaultPatchContext implements IFallenPatchContext {
    private final Set<String> appliedByClass = new LinkedHashSet<>();
    private final Set<String> appliedByClassView = Collections.unmodifiableSet(appliedByClass);
    private boolean isClassEnd;
    private Map<String, MethodInsnNode> currentEntryInserters;

    public void beginClass() {
        isClassEnd = false;
        appliedByClass.clear();
    }

    void recordPatchEffect(String patchId) {
        appliedByClass.add(patchId);
    }

    @Override
    public Set<String> currentClassPatchesApplied() {
        return appliedByClassView;
    }

    public void endClass() {
        isClassEnd = true;
        appliedByClass.clear();
    }

    @Override
    public MethodInsnNode getFallenInserter(InserterKey inserterKey) {
        return currentEntryInserters.get(inserterKey.combine());
    }

    void setEntryInserters(Map<String, MethodInsnNode> inserter) {
        currentEntryInserters = Collections.unmodifiableMap(inserter);
    }

    public boolean isClassEnd() {
        return isClassEnd;
    }
}
