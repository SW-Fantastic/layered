package org.swdc.layered.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 带有此注解的参数，类型，或者函数，应使用本注解的value作为它的类型名称，
 * 而不是类型或者函数本身的name。
 *
 * 例如，如果一个函数名为"add"，但是你想在Mangled符号中使用"plus"作为它的名称，
 * 就可以标注此注解在函数上方。
 *
 * 如果函数的参数标有本注解的同时，参数类型也标有本注解，则以参数标注的本注解为最优先，
 * 其次是参数的类型所标注的本注解，如果本注解标注在函数上，则函数的名称使用本注解标注的value。
 */
@Target({ElementType.TYPE, ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Symbol {

    String value();

}
