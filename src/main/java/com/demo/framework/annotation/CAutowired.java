package com.demo.framework.annotation;

import java.lang.annotation.*;

/**
 * Program Name: spring-design
 * <p>
 * Description:
 * <p>
 *
 * @author zhangjianwei
 * @version 1.0
 * @date 2019/4/24 11:48 AM
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CAutowired {
    String value() default "";
}
