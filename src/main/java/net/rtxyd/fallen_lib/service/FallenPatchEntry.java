package net.rtxyd.fallen_lib.service;

import net.rtxyd.fallen_lib.api.IFallenPatch;
import net.rtxyd.fallen_lib.config.Targeter;
import net.rtxyd.fallen_lib.engine.ClassIndex;
import net.rtxyd.fallen_lib.type.service.IFallenPatchCtorContext;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class FallenPatchEntry {

    private final String className;
    private final int targeter;
    private final int priority;
    private final Targets targets;
    private final boolean includeNestMembers = true;

    private IFallenPatch instance;
    private boolean disabled;

    public FallenPatchEntry(String className, int targeter, int priority, Targets targets) {
        this.className = className;
        this.targeter = targeter;
        this.priority = priority;
        this.targets = targets;
    }

    public String getClassName() {
        return className;
    }

    public int getPriority() {
        return priority;
    }

    /**
     *
     * @return Collected targets number.
     */
    public int getTargetsNumber() {
        return targets.targetsNumber();
    }

    /**
     * Check if there's no targets.
     * @return whether it has any target.
     */
    public boolean isEmpty() {
        return targeter == 0;
    }

    public boolean isIncludeNestMembers() {
        return includeNestMembers;
    }

    /**
     *
     * @param targetClass The potential targeted class of this entry.
     * @param classIndex Collected classes.
     * @return whether it matches the filtered conditions.
     */
    public boolean matches(String targetClass, ClassIndex classIndex) {
        if (disabled) {
            return false;
        }

        if ((targeter & Targeter.EXACT_TARGETER) != 0) {
            if (targets.exact.contains(targetClass)) {
                return true;
            }
        }

        if ((targeter & Targeter.SUBCLASS_TARGETER) != 0 && classIndex.isSubclassOf(targetClass, targets.subclass)) {
            targets.bySubNumber += 1;
            return true;
        }

        return false;
    }

    /**
     *
     * @param loader {@link BytecodeClassLoader} to define the patch class.
     * @param classBytes The byte array cached from scanner engine.
     * @param ctx Patch constructor context
     * @return A {@link IFallenPatch} instance or {@code null}
     */
    IFallenPatch getOrCreateInstance(BytecodeClassLoader loader, byte[] classBytes, DefaultPatchCtorContext ctx) {
        if (disabled || classBytes == null) return null;
        if (instance != null) return instance;
        try {
            Class<?> c = loader.defineClass(className, classBytes);

            try {
                Constructor<?> ctor = c.getDeclaredConstructor(IFallenPatchCtorContext.class);
                // make sure it gives a basic info before constructing.
                ctx.setCurrent(new DefaultPatchDescriptor(className, getTargetsNumber()));
                instance = (IFallenPatch) ctor.newInstance(ctx);
            } catch (NoSuchMethodException e) {
                // fallback
                instance = (IFallenPatch) c.getDeclaredConstructor().newInstance();
            }

            return instance;
        } catch (Throwable t) {
            disabled = true;
            FallenBootstrap.LOGGER.error("Failed to load fallen patch: {} : {}", className, t);
            return null;
        }
    }

    /**
     * Early way to create instance, not used.
     * @param loader {@link ClassLoader} to load the class.
     * @param ctx Patch constructor context
     * @return A {@link IFallenPatch} instance or {@code null}
     */
    IFallenPatch getOrCreateInstance(ClassLoader loader, IFallenPatchCtorContext ctx) {
        if (disabled) return null;
        if (instance != null) return instance;
        try {
            Class<?> c = Class.forName(className, false, loader);

            try {
                Constructor<?> ctor = c.getDeclaredConstructor(IFallenPatchCtorContext.class);
                instance = (IFallenPatch) ctor.newInstance(ctx);
            } catch (NoSuchMethodException e) {
                // fallback
                instance = (IFallenPatch) c.getDeclaredConstructor().newInstance();
            }

            return instance;
        } catch (Throwable t) {
            disabled = true;
            FallenBootstrap.LOGGER.error("Failed to load transformer {}", className, t);
            return null;
        }
    }

    public void disable() {
        disabled = true;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public static class Targets {
        Set<String> exact = Set.of();
        Set<String> subclass = Set.of();
        int byExaNumber = 0;
        int bySubNumber = 0;

        public Targets from(List<String> exact, List<String> subclass) {
            this.exact = exact.stream().map(s->s.replace(".", "/")).collect(Collectors.toUnmodifiableSet());
            this.byExaNumber = this.exact.size();
            this.subclass = subclass.stream().map(s->s.replace(".", "/")).collect(Collectors.toUnmodifiableSet());
            return this;
        }

        public int computeTargeter() {
            int targeter = 0;
            if (!this.exact.isEmpty()) {
                targeter |= Targeter.EXACT_TARGETER;
            }
            if (!this.subclass.isEmpty()) {
                targeter |= Targeter.SUBCLASS_TARGETER;
            }
            return targeter;
        }

        public int targetsNumber() {
            return byExaNumber + bySubNumber;
        }
    }
}
