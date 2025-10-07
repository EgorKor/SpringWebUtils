package io.github.egorkor.webutils.annotations;

import io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedOperations {
    FilterOperation[] value() default {FilterOperation.EQUALS};
}
