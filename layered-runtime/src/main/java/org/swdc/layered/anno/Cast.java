package org.swdc.layered.anno;

import org.swdc.layered.pointers.OpaquePointer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将参数或者返回值视为本类型指针计算MangledName。
 */
@Target({ ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Cast {

    Class<? extends OpaquePointer> value();

}
