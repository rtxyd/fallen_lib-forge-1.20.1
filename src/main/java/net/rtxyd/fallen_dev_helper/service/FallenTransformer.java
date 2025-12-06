package net.rtxyd.fallen_dev_helper.service;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;

import net.rtxyd.fallen_dev_helper.FallenTransformerService;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.rtxyd.fallen_dev_helper.FallenTransformerService.LOGGER;

public class FallenTransformer implements ITransformer<ClassNode> {

    private final Set<Target> classTargets;
    private final Path fallenPath;
    private final Path blacklist;
    private final Path mains;
    private final Path targets;
    private static final String SEP = System.lineSeparator();

    public FallenTransformer(Set<Target> classTargets, Path fallenPath) {
        this.classTargets = classTargets;
        this.fallenPath = fallenPath;
        this.blacklist = fallenPath.resolve(FallenTransformerService.BLACKLIST_NAME);
        this.mains = fallenPath.resolve(FallenTransformerService.MAINS_NAME);
        this.targets = fallenPath.resolve(FallenTransformerService.TARGETS_NAME);
    }

    @Override
    public @NotNull ClassNode transform(ClassNode cn, ITransformerVotingContext context) {
        LOGGER.info("Starting patch operation for {}.", cn);
        boolean isRecord = cn.recordComponents != null && !cn.recordComponents.isEmpty();
        boolean isMain = cn.nestHostClass.equals("null");
        writeRef(cn.name, targets.toFile());
        if (isMain) {
            // outputs the main classes
            writeRef(cn.name, mains.toFile());
        }

        for (MethodNode method : cn.methods) {
            InsnList insns = method.instructions;
            // we must ensure the record class is clean, or it could be risky when we create instance.
            if (method.name.startsWith("<init>") && isRecord) {
                int counter = 0;
                int filedNumber = cn.recordComponents.size();
                // we can use a list here when we debug
                try {
                    for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                        // list here when we debug
                        if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                            AbstractInsnNode insnWalker = insn;
                            while (counter < filedNumber) {
                                insnWalker = insnWalker.getNext();
                                if (insnWalker instanceof VarInsnNode && insnWalker.getNext() instanceof VarInsnNode) {
                                    insnWalker = insnWalker.getNext().getNext();
                                    if (insnWalker instanceof FieldInsnNode) {
                                        counter ++;
                                    } else break;
                                } else break;
                                if (insnWalker.getOpcode() == Opcodes.RETURN) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {}
                if (counter != filedNumber) {
                    // outputs the risky classes
                    writeRef(cn.name, blacklist.toFile());
                }
            }
            if (insns == null) continue;

            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                    MethodInsnNode m = (MethodInsnNode) insn;
                    // only patches like Map.get(key), or Map.getOrDefault(key),
                    // and there are other cases, but will be strictly filtered.
                    if (m.owner.endsWith("Map") && m.name.startsWith("get") && m.desc.startsWith("(Ljava/lang/Object;")) {
                        // param count
                        Type[] args = Type.getArgumentTypes(m.desc);
                        int paramCount = args.length;

                        AbstractInsnNode insnPre = insn.getPrevious();
                        InsnList toInsertPre = new InsnList();
                        // insert map key check method
                        switch (paramCount) {
                            // normal get (needs only 1 parameter)
                            case 1: {
                                toInsertPre.add(new InsnNode(Opcodes.DUP));
                                toInsertPre.add(new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                        "keyCheck",
                                        "(Ljava/lang/Object;)V",
                                        false
                                ));
                                break;
                            }
                            // when uses "Map.getOrDefault()" (needs 2 parameters)
                            case 2: {
                                // check how many stack slots the second parameter takes
                                Type arg2 = args[1];
                                switch (arg2.getSort()) {
                                    // takes 1 stack slot
                                    case Type.INT, Type.FLOAT, Type.SHORT, Type.BYTE, Type.BOOLEAN, Type.CHAR: {
                                        toInsertPre.add(new InsnNode(Opcodes.DUP2));
                                        toInsertPre.add(new InsnNode(Opcodes.POP));
                                        toInsertPre.add(new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                                "keyCheck",
                                                "(Ljava/lang/Object;)V",
                                                false
                                        ));
                                        break;
                                    }
                                    // takes 2 stack slot
                                    case Type.DOUBLE, Type.LONG: {
                                        toInsertPre.add(new InsnNode(Opcodes.DUP2_X2));
                                        toInsertPre.add(new InsnNode(Opcodes.POP2));
                                        toInsertPre.add(new InsnNode(Opcodes.DUP_X2));
                                        toInsertPre.add(new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                                "keyCheck",
                                                "(Ljava/lang/Object;)V",
                                                false
                                        ));
                                        break;
                                    }
                                    default: {
                                        LOGGER.warn("There's a method with unsupported parameter, skip.");
                                        return cn;
                                    }
                                }
                            }
                            // don't do anything for unstandard cases, it should have patched most cases,
                            // but if there's another case we want to patch, it could be added then.
                            default: {
                                LOGGER.warn("There's a method more than 2 parameter, skip.");
                                return cn;
                            }
                        }
                        insns.insert(insnPre, toInsertPre);
                        // after get logic, check return type.
                        if (m.desc.endsWith(")Ljava/lang/Object;")) {
                            InsnList toInsert = new InsnList();
                            toInsert.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                    "modifyLPre",
                                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                                    false
                            ));
                            // most time there's an Opcodes.CHECKCAST after get value, but not always
                            insns.insert(m, toInsert);
                        } else {
                            InsnList toInsertA = new InsnList();
                            switch (m.desc.substring(m.desc.length() - 2)) {
                                // int
                                case ")I":
                                    toInsertA.add(new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                            "modifyI",
                                            "(I)",
                                            false
                                    ));
                                    break;
                                // float
                                case ")F":
                                    toInsertA.add(new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                            "modifyF",
                                            "(F)",
                                            false
                                    ));
                                    break;
                                // double
                                case ")D":
                                    toInsertA.add(new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                            "modifyD",
                                            "(D)",
                                            false
                                    ));
                                    break;
                                // long
                                case ")J":
                                    toInsertA.add(new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                            "modifyJ",
                                            "(J)",
                                            false
                                    ));
                                    break;
                                // boolean
                                // char ignore
                                // byte
                                case ")B":
                                    toInsertA.add(new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                            "modifyB",
                                            "(B)",
                                            false
                                    ));
                                    break;
                                // short
                                case ")S":
                                    toInsertA.add(new MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            "net/kayn/fallen_gems_affixes/augment/GemBonusModifier",
                                            "modifyS",
                                            "(S)",
                                            false
                                    ));
                                    break;
                                default: {
                                    LOGGER.warn("There's a method has unsupported return type, skip.");
                                    return cn;
                                }
                            }
                            insns.insert(m, toInsertA);
                        }
                    }
                }
            }
        }

        return cn;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public @NotNull Set<Target> targets() {
        return classTargets;
    }

    public void writeRef(String className, File file) {
        try {
            Files.createDirectories(fallenPath);
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, true)) {
                writer.append(className).append(SEP);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to create fallen data", e);
        }
    }

}
