package net.rtxyd.fallen.lib.util.patch;

public record InserterKey(String ownerQualifiedName, String methodName, InserterType type) {
    public static InserterKey of(String ownerQualifiedName, String methodName, InserterType type) {
        return new InserterKey(ownerQualifiedName, methodName, type);
    }
    public String combine() {
        return ownerQualifiedName + "." + methodName + "." + type.ordinal();
    }
}