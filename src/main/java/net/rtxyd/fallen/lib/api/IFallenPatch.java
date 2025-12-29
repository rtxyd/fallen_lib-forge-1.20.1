package net.rtxyd.fallen.lib.api;

import net.rtxyd.fallen.lib.api.annotation.FallenPatch;
import net.rtxyd.fallen.lib.type.service.IFallenPatchContext;
import org.objectweb.asm.tree.ClassNode;

/**
 * Implementation must be annotated with {@link FallenPatch}.
 * Do only ASM operations in your implementations.
 * Do not define any static fields and use static block.
 * Do not reference in constructor with any {@link net.minecraft}, {@link net.minecraftforge}
 * Do not reference any target class.
 * Do not trigger any active class loading.
 * Do not create extra thread in your implementations.
 */
@FallenPatch
public interface IFallenPatch {
    static FallenPatch defaultAnnotation() {
        return IFallenPatch.class.getAnnotation(FallenPatch.class);
    }
    void apply(ClassNode node, IFallenPatchContext ctx);
}
