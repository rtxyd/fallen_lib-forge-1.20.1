package net.rtxyd.fallen.lib.util.patch;

import net.rtxyd.fallen.lib.service.DefaultPatchContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

public class StablePatchUtilV2 {
    private static final Map<AbstractInsnNode, InserterMeta> inserterMetaCache = new HashMap<>();
    // method version 1.1
    private static boolean validate(MethodInsnNode exactTarget, MethodInsnNode hookMethod) {
        if (!hookMethod.desc.startsWith(InserterType.standardStarter())) {
            return false;
        }
        if (hookMethod.getOpcode() != Opcodes.INVOKESTATIC) {
            return false;
        }
        if (exactTarget.name.equals(hookMethod.name) && exactTarget.owner.equals(hookMethod.owner) && exactTarget.desc.equals(hookMethod.desc)) {
            return false;
        }
        return true;
    }

    // method version 1.1
    public static AbstractInsnNode insertMethodHookStandard(MethodNode methodNode,
                                                            MethodInsnNode exactTarget,
                                                            MethodInsnNode hookMethod,
                                                            boolean replaceReturn,
                                                            boolean debugMode) {

        if (!validate(exactTarget, hookMethod)) {
            return exactTarget;
        }
        int maxLocalsCache = methodNode.maxLocals;

        StandardInserterHelper ih = new StandardInserterHelper(methodNode, exactTarget, hookMethod, replaceReturn, debugMode);
        InserterMeta meta = inserterMetaCache.get(exactTarget);
        if (meta != null) {
            return ih.runAndNavigateWithCache(meta, true);
        }

        AbstractInsnNode last = ih.runAndNavigate();
        InserterMeta meta1 = new InserterMeta(last, methodNode, maxLocalsCache, ih.getParamSlots(), ih.getReceiverSlot(), ih.getReturnSlot(), ih.stackHasReturn());
        if (debugMode) {
            meta1.couldDebug = false;
        }
        inserterMetaCache.put(exactTarget, meta1);
        return last;
    }

    public static void clearCache(DefaultPatchContext patchContext) throws IllegalAccessException {
        if (patchContext.isClassEnd()) {
            inserterMetaCache.clear();
        } else {
            throw new IllegalAccessException("Unexpected: current class is not end!");
        }
    }
}
