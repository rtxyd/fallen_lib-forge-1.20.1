package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

class PatchSecurityHelperV2 implements IPatchSecurityHelper {
    @Override
    public boolean isPatchClassSafe(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<clinit>") || mn.name.equals("<init>")) {
                if (!isMethodSafe(mn)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean isMethodSafe(MethodNode mn) {
        for (AbstractInsnNode insn : mn.instructions) {
            int op = insn.getOpcode();

            if (op == Opcodes.PUTSTATIC) {
                return false;
            }

            if (insn instanceof MethodInsnNode mi) {
                if (isForbiddenCall(mi)) {
                    return false;
                }
            }

            if (insn instanceof TypeInsnNode ti) {
                if (ti.getOpcode() == Opcodes.NEW &&
                        ti.desc.equals("java/lang/Thread")) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean isForbiddenCall(MethodInsnNode mi) {
        String owner = mi.owner;

        if (owner.equals("java/lang/Class") && mi.name.equals("forName")) {
            return true;
        }

        if (owner.equals("java/lang/Thread") && mi.name.equals("start")) {
            return true;
        }

        if (owner.equals("java/lang/ClassLoader")) {
            return true;
        }

        if (owner.startsWith("net/minecraft/")
                || owner.startsWith("net/minecraftforge/")) {
            return true;
        }

        return false;
    }
}
