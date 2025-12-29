package net.rtxyd.fallen.lib.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FallenPatch {
    /**
     * Determine the sequence when applying.
     */
    int priority() default 1000;

    /**
     * Determine the target classes.
     */
    Targets targets() default @Targets;
}
