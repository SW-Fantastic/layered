package org.swdc.layered.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Closure {

    /**
     * 本Closure所引用的Method名称。
     * @return method name
     */
    String value();

}
