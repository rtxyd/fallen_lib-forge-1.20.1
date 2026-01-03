package net.rtxyd.fallen.lib.util.patch;

public enum InserterType {

    STANDARD("(Lnet/rtxyd/fallen/lib/type/util/patch/IInserterContext;[Ljava/lang/Object;)Ljava/lang/Object;"),
    STANDARD_VOID("(Lnet/rtxyd/fallen/lib/type/util/patch/IInserterContext;[Ljava/lang/Object;)V");
//    BEFORE("(Lnet/rtxyd/fallen/lib/type/util/patch/IInserterContext;[Ljava/lang/Object;)Z"),
//    AFTER("(Lnet/rtxyd/fallen/lib/type/util/patch/IInserterContext;)Ljava/lang/Object;");

    private static final String STANDARD_STARTER = "(Lnet/rtxyd/fallen/lib/type/util/patch/IInserterContext;[Ljava/lang/Object;)";

    private final String desc;

    InserterType(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }
    public static String standardStarter() {
        return STANDARD_STARTER;
    }
}