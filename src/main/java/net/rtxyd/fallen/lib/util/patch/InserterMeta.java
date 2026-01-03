package net.rtxyd.fallen.lib.util.patch;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class InserterMeta {
    AbstractInsnNode last;
    public final MethodNode methodNode;
    final int localsMaxCache;
    final int[] paramsSlot;
    final int receiverSlot;
    final int returnSlot;
    boolean couldDebug;
    boolean stackHasReturn;

    InserterMeta(AbstractInsnNode last, MethodNode methodNode, int localsMaxCache, int[] paramsSlot, int receiverSlot, int returnSlot, boolean stackHasReturn) {
        this.last = last;
        this.methodNode = methodNode;
        this.localsMaxCache = localsMaxCache;
        this.paramsSlot = paramsSlot;
        this.receiverSlot = receiverSlot;
        this.returnSlot = returnSlot;
        this.couldDebug = true;
        this.stackHasReturn = stackHasReturn;
    }
}