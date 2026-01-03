package net.rtxyd.fallen.lib.api.annotation;

import net.rtxyd.fallen.lib.util.patch.InserterType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface FallenInserter {

    /**
     * Type of this inserter
     */
    InserterType type() default InserterType.STANDARD;
}