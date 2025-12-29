package net.rtxyd.fallen.lib.engine;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.List;

public final class ClassView extends ClassVisitor {
    public static int SKIP_ANNOTATION = 0x1;
    int viewTag;
    public String superName;
    public List<String> interfaces;
    public List<String> nestMembers;
    public List<AnnotationNode> visibleAnnotations;


    public ClassView(int viewTag) {
        super(Opcodes.ASM9);
        this.viewTag = viewTag;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.superName = superName;
        if (interfaces != null && interfaces.length > 0) {
            this.interfaces = List.of(interfaces);
        }
    }

    @Override
    public void visitNestMember(String nestMember) {
        if (nestMembers == null) {
            nestMembers = new ArrayList<>();
        }
        nestMembers.add(nestMember);
    }

    @Override
    public void visitSource(final String file, final String debug) {
        // do nothing.
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
        return null;
    }

    @Override
    public void visitNestHost(final String nestHost) {
        // do nothing.
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        // do nothing.
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if ((viewTag & SKIP_ANNOTATION) != 0) {
            return null;
        }

        AnnotationNode an = new AnnotationNode(descriptor);
        if (visible) {
            if (visibleAnnotations == null) {
                visibleAnnotations = new ArrayList<>();
            }
            visibleAnnotations.add(an);
        }
        return an;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        // do nothing.
        return null;
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        // do nothing.
    }

    @Override
    public void visitPermittedSubclass(final String permittedSubclass) {
        // do nothing.
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        // do nothing.
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(final String name, final String descriptor, final String signature) {
        // do nothing
        return null;
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        // do nothing.
        return null;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        // do nothing.
        return null;
    }

    @Override
    public void visitEnd() {
        // do nothing.
    }
}
