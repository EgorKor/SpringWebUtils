package io.github.egorkor.webutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Аннотация для пометки полей наследников класса
 * {@link io.github.egorkor.webutils.queryparam.Filter}
 * с целью задать псевдоним для поля, который
 * впоследствии применяется при получении SQL или JPA фильтра
 * и проверке допустимых полей в фильтре
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FilterFieldAllies {
    String value() default "";
}
