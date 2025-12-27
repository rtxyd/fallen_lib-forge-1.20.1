package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.type.service.IFallenRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FallenPatchRegistry implements IFallenRegistry<FallenPatchEntry> {
    private final Map<String, List<FallenPatchEntry>> targetEntries = new ConcurrentHashMap<>();
    private final Map<String, List<FallenPatchEntry>> cache = new ConcurrentHashMap<>();
    Map<String, byte[]> classBytes = new ConcurrentHashMap<>();
    private Set<String> targets;

    public List<FallenPatchEntry> match(String className) {
        return cache.computeIfAbsent(className, this::computeMatch);
    }

    private List<FallenPatchEntry> computeMatch(String className) {
        return targetEntries.computeIfAbsent(className, e -> List.of());
    }

    void register(String targetName, FallenPatchEntry entry) {
        targetEntries
                .computeIfAbsent(targetName, e -> new ArrayList<>())
                .add(entry);
        cache.clear();
    }

    public Set<String> targets() {
        if (targets == null) {
            targets = Collections.unmodifiableSet(targetEntries.keySet());
        }
        return targets;
    }

    Optional<byte[]> getClassBytes(String className) {
        return Optional.ofNullable(classBytes.get(className));
    }
}
