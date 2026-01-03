package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

class PatchSecurityHelperV2 implements IPatchSecurityHelper {
    @Override
    public boolean isPatchClassSafe(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<clinit>") || mn.name.equals("<init>")) {
                if (!isMethodSafe(mn)) {
                    FallenBootstrap.LOGGER.debug("Warning: `{}.{}` contains unsafe instruction, skip.", cn.name, mn.name);
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
        boolean result = false;

        if (owner.equals("java/lang/Class") && mi.name.equals("forName")) {
            result = true;
        }

        if (owner.equals("java/lang/Thread") && mi.name.equals("start")) {

            result = true;
        }

        if (owner.equals("java/lang/ClassLoader")) {
            result = true;
        }

        if (owner.startsWith("net/minecraft/")
                || owner.startsWith("net/minecraftforge/")) {
            result = true;
        }

        if (result) {
            FallenBootstrap.LOGGER.debug("Warning: `{}.{} is not allowed in constructor, skip", owner, mi.name);
        }

        return result;
    }
}
