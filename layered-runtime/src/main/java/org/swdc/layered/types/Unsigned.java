package org.swdc.layered.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 带有此参数的注解，对应的Mangled符号中应标记为unsigned无符号类型，
 * 除了常规的位置，本注解可以标注在泛型参数的具体类型上，例如：PtrPointer<@Unsigned Integer>
 */
@Target({ElementType.PARAMETER, ElementType.TYPE_USE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Unsigned {
}
