package net.rtxyd.fallen.lib.service;

class BytecodeClassLoader extends ClassLoader {
    public BytecodeClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length);
    }
}