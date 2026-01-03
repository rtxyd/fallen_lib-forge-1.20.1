package net.rtxyd.fallen.lib.type.service;

import net.rtxyd.fallen.lib.util.patch.InserterKey;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Set;

public interface IFallenPatchContext {

    Set<String> currentClassPatchesApplied();

    MethodInsnNode getFallenInserter(InserterKey inserterKey);
}