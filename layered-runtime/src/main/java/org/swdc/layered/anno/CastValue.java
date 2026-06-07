package org.swdc.layered.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 如果参数或者返回值的类型是平台相关的，那么应该使用本注解
 * 标注它的具体类型，目前常见的有“size_t"。
 */
@Target({ ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CastValue {

    PlatformType value();

}
