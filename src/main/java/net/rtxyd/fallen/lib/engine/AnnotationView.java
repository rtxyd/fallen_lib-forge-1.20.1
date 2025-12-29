package net.rtxyd.fallen.lib.engine;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

class AnnotationView extends AnnotationVisitor {

    public final String desc;
    public final Map<String, Object> values = new HashMap<>();

    public AnnotationView(String desc) {
        super(Opcodes.ASM9);
        this.desc = desc;
    }

    @Override
    public void visit(String name, Object value) {
        values.put(name, value);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        values.put(name, value);
    }
}