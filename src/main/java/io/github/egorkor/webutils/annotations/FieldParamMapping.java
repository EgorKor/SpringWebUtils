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
 * <ul>
 * <li>{@link #sqlMapping()} параметр задаёт, то в какое имя поля будет
 * преобразован входной параметр</li>
 * <li>{@link #requestParamMapping()} параметр задаёт, то какое имя входного
 * параметра принимается с клиента</li>
 * </ul>
 *
 * <pre>
 *     {@code
 *        public class UserFilter{
 *            private String username;
 *            @FieldParamMapping(requestParamMapping="order_name", sqlMapping="orders.name")
 *            private String ordername;
 *            @FieldParamMapping(requestParamMapping="orders_count", sqlMapping="orders.length")
 *            private Integer ordersCount;
 *        }
 *     }
 * </pre>
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldParamMapping {
    String requestParamMapping() default NO_MAPPING;

    String sqlMapping() default NO_MAPPING;


    String NO_MAPPING = "NO_MAPPING";
}
