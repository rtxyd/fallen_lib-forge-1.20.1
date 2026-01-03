package net.rtxyd.fallen.lib.util.patch;

import net.rtxyd.fallen.lib.type.util.patch.IInserterContext;

public record InserterContext(Object receiver, Object ret, int flags) implements IInserterContext<Object, Object> {
    public static final int F_INSTANCE        = 1 << 0;
    public static final int F_INVOKESTATIC    = 1 << 1;

    public static final int F_RET_VOID        = 1 << 2;
    public static final int F_RET_PRIMITIVE   = 1 << 3;
    public static final int F_REPLACE_RET     = 1 << 4;

    public static final int F_INVOKEINTERFACE = 1 << 5;
    public static final int F_INVOKESPECIAL   = 1 << 6;
    public static final int F_INVOKEDYNAMIC   = 1 << 7;

    @Override
    public boolean isInstanceCall() {
        return (flags & F_INSTANCE) != 0;
    }
    @Override
    public boolean isStaticCall() {
        return (flags & F_INVOKESTATIC) != 0;
    }
    @Override
    public boolean isInvokeDynamic() {
        return (flags & F_INVOKEDYNAMIC) != 0;
    }
    @Override
    public boolean isVoidReturn() {
        return (flags & F_RET_VOID) != 0;
    }
    @Override
    public boolean isPrimitiveReturn() {
        return (flags & F_RET_PRIMITIVE) != 0;
    }
    @Override
    public boolean willReplaceReturn() {
        return (flags & F_REPLACE_RET) != 0;
    }
}
