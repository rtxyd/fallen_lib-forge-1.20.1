package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.config.FallenConfig;
import net.rtxyd.fallen.lib.service.FallenPatchEntry;
import net.rtxyd.fallen.lib.type.engine.IScanContext;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.util.*;

public class ScanContext implements IScanContext {
    public final ClassIndex classIndex = new ClassIndex();
    protected final List<FallenPatchEntry> internalPatchEntries = new ArrayList<>();
    protected final Map<FallenConfig, ResourceContainer> internalConfigContainers = new HashMap<>();
    protected final Map<String, byte[]> internalClassBytes = new HashMap<>();

    private List<FallenPatchEntry> sortedPatchEntries;
    private Map<FallenConfig, ResourceContainer> configContainers;
    private Map<String, byte[]> classBytesView;

    public List<FallenPatchEntry> patchEntries() {
        if (sortedPatchEntries == null) {
            internalPatchEntries.sort(Comparator.comparingInt(FallenPatchEntry::getPriority));
            sortedPatchEntries = Collections.unmodifiableList(internalPatchEntries);
        }
        return sortedPatchEntries;
    }

    public Map<FallenConfig, ResourceContainer> configContainers() {
        if (configContainers == null) {
            configContainers = Collections.unmodifiableMap(internalConfigContainers);
        }
        return configContainers;
    }

    public Map<String, byte[]> getClassBytesView() {
        if (classBytesView == null) {
            classBytesView = Collections.unmodifiableMap(internalClassBytes);
        }
        return classBytesView;
    }
}
