package net.rtxyd.fallen.lib.engine;

import java.util.Collections;
import java.util.List;

public final class ClassInfo {
    public final String superName;
    public final List<String> interfaces;
    public final List<String> nestMembers;

    public ClassInfo(String superName, List<String> interfaces, List<String> nestMembers) {
        this.superName = superName;
        this.interfaces = interfaces != null ? interfaces : Collections.emptyList();
        this.nestMembers = nestMembers != null ? nestMembers : Collections.emptyList();
    }

    public List<String> getNestMembers() {
        return Collections.unmodifiableList(nestMembers);
    }
}