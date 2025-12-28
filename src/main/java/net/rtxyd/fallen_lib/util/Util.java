package net.rtxyd.fallen_lib.util;

import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class Util {
    public static ClassNode cloneClassNode(ClassNode original) {
        ClassWriter cw = new ClassWriter(0);
        original.accept(cw);
        byte[] bytes = cw.toByteArray();

        ClassReader cr = new ClassReader(bytes);
        ClassNode copy = new ClassNode();
        cr.accept(copy, 0);
        return copy;
    }

    public static boolean isCleanRecordCtor(MethodNode mn, ClassNode cn) {
        boolean isRecord = cn.recordComponents != null && !cn.recordComponents.isEmpty();
        if (!isRecord) return false;
        int counter = 0;
        if (mn.name.startsWith("<init>")) {
            int filedNumber = cn.recordComponents.size();
            InsnList insns = mn.instructions;
            if (insns == null) return false;
            // to debug this method, use a predefined insns list here when debugging
            try {
                for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                    // use a list here when debugging
                    if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                        AbstractInsnNode insnWalker = insn;
                        while (counter <= filedNumber) {
                            insnWalker = insnWalker.getNext();
                            if (counter == filedNumber) {
                                if (insnWalker.getOpcode() != Opcodes.RETURN) {
                                    counter ++;
                                    break;
                                }
                                break;
                            }
                            if (insnWalker instanceof VarInsnNode && insnWalker.getNext() instanceof VarInsnNode) {
                                insnWalker = insnWalker.getNext().getNext();
                                if (insnWalker instanceof FieldInsnNode) {
                                    counter ++;
                                    continue;
                                } else break;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}
            return counter == filedNumber;
        }
        return true;
    }
    // not implemented
    private static void mapPatchPrefix() {
        throw new NotImplementedException();
    }
    private static void mapPatchPostfix() {
        throw new NotImplementedException();
    }
}
