package io.github.egorkor.webutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamCountLimit {
    int UNLIMITED = -1;

    int value() default UNLIMITED;
}
