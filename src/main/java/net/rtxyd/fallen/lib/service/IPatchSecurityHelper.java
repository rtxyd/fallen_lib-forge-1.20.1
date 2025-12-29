package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.tree.ClassNode;

interface IPatchSecurityHelper {
    boolean isPatchClassSafe(ClassNode cn);
}
