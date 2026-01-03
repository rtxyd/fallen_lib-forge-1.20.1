package net.rtxyd.fallen.lib.util.patch;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class AbstractInserterHelper {
    protected final MethodNode methodNode;
    protected final MethodInsnNode exactTarget;
    protected final MethodInsnNode hookMethod;

    protected AbstractInserterHelper(MethodNode methodNode, MethodInsnNode exactTarget, MethodInsnNode hookMethod) {
        this.methodNode = methodNode;
        this.exactTarget = exactTarget;
        this.hookMethod = hookMethod;
    }

    abstract AbstractInsnNode runAndNavigate();

    abstract AbstractInsnNode runAndNavigateWithCache(InserterMeta meta, boolean insertBottom);
}
