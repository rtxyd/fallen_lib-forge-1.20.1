package net.rtxyd.fallen.lib.type.util.patch;

public interface IInserterContext<T, R> {
    T receiver();
    R ret();

    boolean isInstanceCall();

    boolean isStaticCall();

    boolean isInvokeDynamic();

    boolean isVoidReturn();

    boolean isPrimitiveReturn();

    boolean willReplaceReturn();
}
