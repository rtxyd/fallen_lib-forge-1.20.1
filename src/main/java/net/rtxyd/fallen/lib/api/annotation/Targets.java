package net.rtxyd.fallen.lib.api.annotation;

public @interface Targets {
    /**
     * This will target all given classes.
     */
    Class<?>[] exact() default {};
    /**
     * This will target all subclasses of given classes and their nest members.
     */
    Class<?>[] subclass() default {};
}
