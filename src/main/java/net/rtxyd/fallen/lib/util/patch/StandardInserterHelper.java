package net.rtxyd.fallen.lib.util.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static net.rtxyd.fallen.lib.util.PatchUtil.*;

public final class StandardInserterHelper extends AbstractInserterHelper {

    private boolean replaceReturn;
    private boolean debugMode;

    private Type[] callArgs;
    private Type callReturnType;
    private Type hookReturnType;
    private int callReturnTypeSort;
    private int hookReturnTypeSort;

    private boolean hookReturnVoid;
    private boolean callReturnVoid;
    private boolean callReturnPrimitive;
    private boolean ignoreReceiver;

    private LabelNode startLabel;
    private LabelNode endLabel;

    private int localsMax;
    private int[] paramSlots;
    private int receiverSlot;
    private int returnSlot;
    private int contextFlags;

    public StandardInserterHelper(MethodNode methodNode, MethodInsnNode exactTarget, MethodInsnNode hookMethod, boolean replaceReturn, boolean debugMode) {
        super(methodNode, exactTarget, hookMethod);
        this.replaceReturn = replaceReturn;
        this.debugMode = debugMode;
        if (debugMode) {
            this.startLabel = new LabelNode();
            this.endLabel = new LabelNode();
        }
        init();
    }

    @Override
    AbstractInsnNode runAndNavigate() {
        start();
        computeParamSlots();
        InsnList before = computeBeforeInsns();
        methodNode.instructions.insertBefore(exactTarget, before);
        InsnList after = computeAfterInsns();
        // get here to avoid insert clearing list.
        AbstractInsnNode last = after.getLast();
        methodNode.instructions.insert(exactTarget, after);
        end();
        return last == null ? exactTarget : last;
    }

    @Override
    AbstractInsnNode runAndNavigateWithCache(InserterMeta meta, boolean insertBottom) {
        localsMax = meta.localsMaxCache;
        paramSlots = meta.paramsSlot;
        receiverSlot = meta.receiverSlot;
        returnSlot = meta.returnSlot;
        debugMode = meta.couldDebug && debugMode;

        InsnList after = new InsnList();
        if (meta.stackHasReturn) {
            after.add(new VarInsnNode(getStoreOpcode(callReturnType), returnSlot));
        }

        after.add(loadHookParamsAndInvoke());
        after.add(parseHookReturn());
        // get here to avoid insert clearing list.
        AbstractInsnNode last = after.getLast();
        if (insertBottom) {
            // update newest node
            methodNode.instructions.insert(meta.last, after);
            meta.last = last;
            return last;
        }
        methodNode.instructions.insert(exactTarget, after);
        return meta.last;
    }

    private void init() {
        callArgs = Type.getArgumentTypes(exactTarget.desc);
        callReturnType = Type.getReturnType(exactTarget.desc);
        hookReturnType = Type.getReturnType(hookMethod.desc);
        contextFlags = 0;

        callReturnTypeSort = callReturnType.getSort();
        hookReturnTypeSort = hookReturnType.getSort();

        hookReturnVoid = hookReturnTypeSort == Type.VOID;
        callReturnVoid = callReturnTypeSort == Type.VOID;
        callReturnPrimitive = callReturnTypeSort != Type.OBJECT && callReturnTypeSort != Type.ARRAY;
        int targetOp = exactTarget.getOpcode();

        ignoreReceiver = true;
        switch (targetOp) {
            case Opcodes.INVOKEINTERFACE -> {
                contextFlags |= InserterContext.F_INVOKEINTERFACE;
                ignoreReceiver = false;
            }
            case Opcodes.INVOKESTATIC -> {
                contextFlags |= InserterContext.F_INVOKESTATIC;
            }
            case Opcodes.INVOKEDYNAMIC -> {
                contextFlags |= InserterContext.F_INVOKEDYNAMIC;
            }
            case Opcodes.INVOKESPECIAL -> {
                contextFlags |= InserterContext.F_INVOKESPECIAL;
            }
        }

        if (callReturnPrimitive) {
            contextFlags |= InserterContext.F_RET_PRIMITIVE;
        }
        if (callReturnVoid) {
            contextFlags |= InserterContext.F_RET_VOID;
        }

        if (ignoreReceiver) {
            contextFlags |= InserterContext.F_INSTANCE;
        }

        // these two are related to hook
        if (hookReturnVoid || hookReturnTypeSort != callReturnTypeSort) {
            replaceReturn = false;
        }
        if (replaceReturn) {
            contextFlags |= InserterContext.F_REPLACE_RET;
        }
    }

    private void start() {
        localsMax = methodNode.maxLocals;
        paramSlots = new int[callArgs.length];
    }

    private void computeParamSlots() {
        // local slots and name if debug
        for (int i = 0; i < callArgs.length; i++) {
            // calc the stack slot of params
            paramSlots[i] = localsMax;
            if (debugMode) {
                methodNode.localVariables.add(new LocalVariableNode(
                        "fallen$hookP$" + i,
                        callArgs[i].getDescriptor(),
                        null,
                        startLabel,
                        endLabel,
                        localsMax
                ));
            }
            localsMax += callArgs[i].getSize();
        }
    }

    private InsnList computeBeforeInsns() {
        InsnList argInsns = new InsnList();
        if (debugMode) {
            argInsns.add(startLabel);
        }
        for (int i = callArgs.length - 1; i >= 0; i--) {
            argInsns.add(new VarInsnNode(getStoreOpcode(callArgs[i]), paramSlots[i]));
        }
        receiverSlot = localsMax;
        if (!ignoreReceiver) {
            // store object ref (this)
            argInsns.add(new VarInsnNode(Opcodes.ASTORE, receiverSlot));
            if (debugMode) {
                methodNode.localVariables.add(new LocalVariableNode(
                        "fallen$hookThs",
                        "Ljava/lang/util/Object;",
                        null,
                        startLabel,
                        endLabel,
                        localsMax
                ));
            }
            argInsns.add(new VarInsnNode(Opcodes.ALOAD, receiverSlot));
            localsMax += 1;
        }
        for (int i = 0; i < callArgs.length; i++) {
            argInsns.add(new VarInsnNode(getLoadOpcode(callArgs[i]), paramSlots[i]));
        }
        return argInsns;
    }

    private InsnList newCtx() {
        InsnList newInsns = new InsnList();
        newInsns.add(new TypeInsnNode(Opcodes.NEW, "net/rtxyd/fallen/lib/util/patch/InserterContext"));
        newInsns.add(new InsnNode(Opcodes.DUP));
        if (ignoreReceiver) {
            newInsns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            newInsns.add(new VarInsnNode(Opcodes.ALOAD, receiverSlot));
        }
        if (!callReturnVoid) {
            if (callReturnPrimitive) {
                newInsns.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
                newInsns.add(boxType(callReturnType));
            } else {
                // object
                newInsns.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
            }
        } else {
            newInsns.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        newInsns.add(pushInt(contextFlags));
        newInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/rtxyd/fallen/lib/util/patch/InserterContext", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;I)V"));
        // here is 1 stack remain
        return newInsns;
    }

    private InsnList loadHookParamsAndInvoke() {
        InsnList invokeInsns = new InsnList();
        invokeInsns.add(newCtx());
        invokeInsns.add(pushInt(callArgs.length));
        invokeInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        // load params to stack and invoke hook
        for (int i = 0; i < callArgs.length; i++) {
            Type callT = callArgs[i];
            int paramSlot = paramSlots[i];
            if (callT.getSort() != Type.OBJECT && callT.getSort() != Type.ARRAY) {
                invokeInsns.add(new InsnNode(Opcodes.DUP));
                invokeInsns.add(pushInt(i));
                invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
                invokeInsns.add(boxType(callT));
                invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            } else {
                // object
                invokeInsns.add(new InsnNode(Opcodes.DUP));
                invokeInsns.add(pushInt(i));
                invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
                invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        /*
         *  invoke hook method
         *  ctx, arg1, arg2, arg3,..., argN
         */
        invokeInsns.add(hookMethod.clone(null));
        return invokeInsns;
    }

    private InsnList computeAfterInsns() {
        // createInserterContext
        InsnList hookInsns = new InsnList();
        returnSlot = localsMax;
        if (!callReturnVoid) {
            hookInsns.add(new VarInsnNode(getStoreOpcode(callReturnType), returnSlot));
            if (debugMode) {
                methodNode.localVariables.add(new LocalVariableNode(
                        "fallen$hookRet",
                        callReturnType.getDescriptor(),
                        null,
                        startLabel,
                        endLabel,
                        localsMax
                ));
            }
            localsMax += callReturnType.getSize();
        }

        hookInsns.add(loadHookParamsAndInvoke());
        hookInsns.add(parseHookReturn());
        return hookInsns;
    }

    private InsnList parseHookReturn() {
        InsnList retList = new InsnList();
        if (callReturnPrimitive && replaceReturn) {
            retList.add(unboxType(callReturnType));
        }
        // test here with hook returns x,
        // and target returns x,
        // and relaceReturn is false
        if (!replaceReturn) {
            if (!hookReturnVoid) {
                retList.add(new InsnNode(Opcodes.POP));
            }
            if (!callReturnVoid) {
                retList.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
            }
        }
        if (debugMode) {
            retList.add(endLabel);
        }
        return retList;
    }

    private void end() {
        methodNode.maxLocals = localsMax;
    }

    int[] getParamSlots() {
        return paramSlots;
    }

    int getReceiverSlot() {
        return receiverSlot;
    }

    int getReturnSlot() {
        return returnSlot;
    }

    boolean stackHasReturn() {
        return !callReturnVoid;
    }
}
