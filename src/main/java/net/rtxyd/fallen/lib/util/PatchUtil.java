package net.rtxyd.fallen.lib.util;

import net.rtxyd.fallen.lib.service.FallenBootstrap;
import net.rtxyd.fallen.lib.util.patch.InserterType;
import net.rtxyd.fallen.lib.util.patch.StablePatchUtilV2;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public class PatchUtil {
    static final Logger LOGGER = LoggerFactory.getLogger("fallen.util");
    public static final String STANDARD_METHOD = """
                public static Object hook(IInserterContext<Object, Object> ctx, Object... args) {
                    return ctx.ret();
                }
            """;

    public static void bootstrap() {}

    public static ClassNode cloneClassNode(ClassNode original) {
        ClassWriter cw = new ClassWriter(0);
        original.accept(cw);
        byte[] bytes = cw.toByteArray();

        ClassReader cr = new ClassReader(bytes);
        ClassNode copy = new ClassNode();
        cr.accept(copy, 0);
        return copy;
    }

    public static boolean isCtor(MethodNode mn) {
        return mn.name.equals("<init>");
    }

    public static boolean isCleanRecordCtor(MethodNode mn, ClassNode cn) {
        if (!isRecord(cn) || !mn.name.equals("<init>")) {
            return false;
        }
        InsnList insns = mn.instructions;

        int fieldCount = cn.recordComponents.size();
        int counter = 0;
        boolean failed = false;

        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            int op = insn.getOpcode();
            // skip non-code opcode
            if (op < 0) continue;

            if (op == Opcodes.PUTFIELD) {
                if (counter >= fieldCount) {
                    failed = true;
                    break;
                }
                FieldInsnNode fi = (FieldInsnNode) insn;
                RecordComponentNode rc = cn.recordComponents.get(counter);
                if (!fi.name.equals(rc.name)) {
                    failed = true;
                    break;
                }
                counter++;
                continue;
            }
            if (op == Opcodes.RETURN) {
                break;
            }
        }

        return counter == fieldCount && !failed;
    }

    public static boolean isRecord(ClassNode cn) {
        return cn.recordComponents != null && !cn.recordComponents.isEmpty();
    }

    public static boolean isNotInnerClass(ClassNode cn) {
        return cn.nestHostClass == null;
    }

    public static MethodInsnNode methodToInsn(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        String name = method.getName();
        String desc = Type.getMethodDescriptor(method);
        boolean isInterface = method.getDeclaringClass().isInterface();

        int opcode;
        if (Modifier.isStatic(method.getModifiers())) {
            opcode = Opcodes.INVOKESTATIC;
        } else if (isInterface) {
            opcode = Opcodes.INVOKEINTERFACE;
        } else if (method.getName().equals("<init>") || Modifier.isPrivate(method.getModifiers())) {
            opcode = Opcodes.INVOKESPECIAL;
        } else {
            opcode = Opcodes.INVOKEVIRTUAL;
        }

        return new MethodInsnNode(opcode, owner, name, desc, isInterface);
    }

    public static int getLoadOpcode(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: return Opcodes.ILOAD;
            case Type.LONG: return Opcodes.LLOAD;
            case Type.FLOAT: return Opcodes.FLOAD;
            case Type.DOUBLE: return Opcodes.DLOAD;
            case Type.ARRAY:
            case Type.OBJECT: return Opcodes.ALOAD;
            default: throw new IllegalArgumentException("Unknown type: " + t);
        }
    }

    public static int getStoreOpcode(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: return Opcodes.ISTORE;
            case Type.LONG: return Opcodes.LSTORE;
            case Type.FLOAT: return Opcodes.FSTORE;
            case Type.DOUBLE: return Opcodes.DSTORE;
            case Type.ARRAY:
            case Type.OBJECT: return Opcodes.ASTORE;
            default:
                throw new IllegalArgumentException("Unknown type: " + t);
        }
    }

    public static AbstractInsnNode boxType(Type t) {
        String owner, desc;
        switch (t.getSort()) {
            case Type.BOOLEAN:
                owner = "java/lang/Boolean";
                desc = "(Z)Ljava/lang/Boolean;";
                break;
            case Type.BYTE:
                owner = "java/lang/Byte";
                desc = "(B)Ljava/lang/Byte;";
                break;
            case Type.CHAR:
                owner = "java/lang/Character";
                desc = "(C)Ljava/lang/Character;";
                break;
            case Type.SHORT:
                owner = "java/lang/Short";
                desc = "(S)Ljava/lang/Short;";
                break;
            case Type.INT:
                owner = "java/lang/Integer";
                desc = "(I)Ljava/lang/Integer;";
                break;
            case Type.LONG:
                owner = "java/lang/Long";
                desc = "(J)Ljava/lang/Long;";
                break;
            case Type.FLOAT:
                owner = "java/lang/Float";
                desc = "(F)Ljava/lang/Float;";
                break;
            case Type.DOUBLE:
                owner = "java/lang/Double";
                desc = "(D)Ljava/lang/Double;";
                break;
            default:
                throw new IllegalArgumentException("Cannot box type: " + t);
        }
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "valueOf", desc, false);
    }

    public static AbstractInsnNode unboxType(Type t) {
        String owner, name, desc;
        switch (t.getSort()) {
            case Type.BOOLEAN:
                owner = "java/lang/Boolean";
                name = "booleanValue";
                desc = "()Z";
                break;
            case Type.BYTE:
                owner = "java/lang/Byte";
                name = "byteValue";
                desc = "()B";
                break;
            case Type.CHAR:
                owner = "java/lang/Character";
                name = "charValue";
                desc = "()C";
                break;
            case Type.SHORT:
                owner = "java/lang/Short";
                name = "shortValue";
                desc = "()S";
                break;
            case Type.INT:
                owner = "java/lang/Integer";
                name = "intValue";
                desc = "()I";
                break;
            case Type.LONG:
                owner = "java/lang/Long";
                name = "longValue";
                desc = "()J";
                break;
            case Type.FLOAT:
                owner = "java/lang/Float";
                name = "floatValue";
                desc = "()F";
                break;
            case Type.DOUBLE:
                owner = "java/lang/Double";
                name = "doubleValue";
                desc = "()D";
                break;
            default:
                throw new IllegalArgumentException("Cannot unbox type: " + t);
        }
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc, false);
    }

    public static AbstractInsnNode getDefaultValueInsn(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: return new InsnNode(Opcodes.ICONST_0);
            case Type.LONG: return new InsnNode(Opcodes.LCONST_0);
            case Type.FLOAT: return new InsnNode(Opcodes.FCONST_0);
            case Type.DOUBLE: return new InsnNode(Opcodes.DCONST_0);
            case Type.ARRAY:
            case Type.OBJECT: return new InsnNode(Opcodes.ACONST_NULL);
            default: throw new IllegalArgumentException("Unknown type: " + t);
        }
    }

    public static MethodInsnNode buildMethodInsnNodeWith(InserterType type, String fullClassName, String methodName) {
        if (type == InserterType.STANDARD) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC,
                    fullClassName.replace(".", "/"),
                    methodName,
                    InserterType.STANDARD.desc());
        } else if (type == InserterType.STANDARD_VOID) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC,
                    fullClassName.replace(".", "/"),
                    methodName,
                    InserterType.STANDARD_VOID.desc());
        }
        return null;
    }

    public static void insertMethodHook(ClassNode classNode,
                                        MethodNode methodNode,
                                        Predicate<? super AbstractInsnNode> filter,
                                        InserterType type,
                                        MethodInsnNode hookMethod,
                                        boolean replaceReturn,
                                        boolean debugMode) {
        switch (type) {
            case STANDARD, STANDARD_VOID -> insertStandardMethodHook(classNode, methodNode, filter::test, hookMethod, replaceReturn, debugMode);
        }
    }

    public static void insertStandardMethodHook(ClassNode classNode,
                                        MethodNode methodNode,
                                        Predicate<MethodInsnNode> filter,
                                        MethodInsnNode hookMethod,
                                        boolean replaceReturn,
                                        boolean debugMode) {
        InsnList insns = methodNode.instructions;
        // to check if init, or if is after super ctor
        boolean shouldCheck = methodNode.name.equals("<init>");
        if (!hookMethod.desc.startsWith(InserterType.standardStarter())) {
            LOGGER.debug("Inserter {} is not standard form! Expect: (descriptor)\n {}", hookMethod.owner + "." + hookMethod.name, STANDARD_METHOD);
            return;
        }
        if (hookMethod.getOpcode() != Opcodes.INVOKESTATIC) {
            LOGGER.debug("Inserter {} is not standard form ! Expect: (static)\n {}", hookMethod.owner + "." + hookMethod.name, STANDARD_METHOD);
            return;
        }
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call)) continue;
            if (!filter.test(call)) continue;
            if (shouldCheck && call.owner.equals(classNode.superName)) {
                shouldCheck = false;
                continue;
            }
            insn = StablePatchUtilV2.insertMethodHookStandard(methodNode, call, hookMethod, replaceReturn, debugMode);
        }
    }

    public static AbstractInsnNode pushInt(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, v);
        } else {
            return new LdcInsnNode(v);
        }
    }

    public static AbstractInsnNode pushBoolean(boolean bool) {
        if (bool) {
            return new InsnNode(Opcodes.ICONST_1);
        } else {
            return new InsnNode(Opcodes.ICONST_0);
        }
    }

    public static boolean isStatic(MethodNode mn) {
        return (mn.access & Opcodes.ACC_STATIC) != 0;
    }

    public static List<AbstractInsnNode> findInsnInMethod(MethodNode mn, Predicate<AbstractInsnNode> ainP) {
        InsnList insns = mn.instructions;
        List<AbstractInsnNode> insnNodeList = new ArrayList<>();
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (ainP.test(insn)) {
                insnNodeList.add(insn);
                return insnNodeList;
            }
        }
        return insnNodeList;
    }

    public static List<MethodInsnNode> findMethodInsnInMethod(MethodNode mn, Predicate<MethodInsnNode> ainP) {
        InsnList insns = mn.instructions;
        List<MethodInsnNode> insnNodeList = new ArrayList<>();
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && ainP.test(min)) {
                insnNodeList.add(min);
                return insnNodeList;
            }
        }
        return insnNodeList;
    }

    public static InsnList toInsnList(List<AbstractInsnNode> list) {
        InsnList cloneList = new InsnList();
        for (AbstractInsnNode insn : list) {
            // clone
            cloneList.add(insn.clone(null));
        }
        return cloneList;
    }
}
