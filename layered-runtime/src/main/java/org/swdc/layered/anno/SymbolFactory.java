package org.swdc.layered.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SymbolFactory {

    String value();
    boolean creator() default false;
    boolean deleter() default false;

}
