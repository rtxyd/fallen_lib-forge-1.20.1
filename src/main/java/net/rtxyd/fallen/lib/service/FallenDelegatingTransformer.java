package net.rtxyd.fallen.lib.service;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.rtxyd.fallen.lib.api.IFallenPatch;
import net.rtxyd.fallen.lib.util.PatchUtil;
import net.rtxyd.fallen.lib.util.patch.StablePatchUtilV2;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;

public final class FallenDelegatingTransformer implements ITransformer<ClassNode> {
    private final FallenPatchRegistry registry;
    private final DefaultPatchCtorContext ctorContext;
    private final DefaultPatchContext patchContext;
    private BytecodeClassLoader classLoader;


    FallenDelegatingTransformer(FallenPatchRegistry registry, DefaultPatchCtorContext ctorContext) {
        this.registry = registry;
        this.ctorContext = ctorContext;
        this.patchContext = new DefaultPatchContext();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public @NotNull ClassNode transform(ClassNode cn, ITransformerVotingContext context) {
        if (classLoader == null) {
            // must ensure the classloader's parent is the same with other transformers.
            // so it's controllable to handle not allowed class loading.
            classLoader = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader());
        }
        // modlauncher will protect us from ReentrantException, but better do not trigger any class loading.
        patchContext.beginClass();
        String className = cn.name;
        for (FallenPatchEntry e : registry.match(className)) {
            Optional<byte[]> cbOpt = registry.getClassBytes(e.getClassName());
            // pass the context to ctor.
            IFallenPatch t = e.getOrCreateInstance(classLoader, cbOpt.orElse(null), ctorContext);
            if (t == null) continue;
            // must make sure there is a fallback if transformer failed.
            // but we can't actually restrain its behavior, this is a safeguard.
            ClassNode fallback = PatchUtil.cloneClassNode(cn);
            try {
                patchContext.setEntryInserters(e.getInserter());
                t.apply(cn, patchContext);
                if (cn == null) {
                    throw new NullPointerException("ClassNode is set to null!");
                }
                patchContext.recordPatchEffect(e.getClassName());
                FallenBootstrap.LOGGER.info("Fallen patch [{}] successfully applied on [{}]", e.getClassName(), className);
            } catch (Throwable ex) {
                FallenBootstrap.LOGGER.error("Fallen patch [{}] failed on [{}].\n" +
                        "If it's *Reentrant Exception*, please check if your fallen patch references any target class.", e.getClassName(), className, ex);
                e.disable();
                cn = fallback;
            }
        }
        patchContext.endClass();
        try {
            StablePatchUtilV2.clearCache(patchContext);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // Note: cpw transformer will compute frames, so we don't need to worry about it.
        // just modify and return.
        return cn;
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public @NotNull Set<Target> targets() {
        return registry.targets().stream().map(Target::targetClass).collect(Collectors.toSet());
    }
}
