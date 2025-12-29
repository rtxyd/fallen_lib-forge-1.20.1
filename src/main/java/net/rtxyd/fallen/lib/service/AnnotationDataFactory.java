package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.tree.AnnotationNode;

interface AnnotationDataFactory {
    AnnotationData create(AnnotationNode node);

}
