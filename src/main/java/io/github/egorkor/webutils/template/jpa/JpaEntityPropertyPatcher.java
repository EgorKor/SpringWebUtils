package io.github.egorkor.webutils.template.jpa;

import lombok.Getter;
import lombok.SneakyThrows;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * @author EgorKor
 * @version 1.0.4
 * @since 2025
 */
public class JpaEntityPropertyPatcher {
    @Getter
    private static final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    public static List<Field> getDeclaredFieldsCached(Class<?> type) {
        return fieldCache.computeIfAbsent(type, t ->
                Arrays.stream(t.getDeclaredFields())
                        .filter(f -> !shouldSkipField(f))
                        .peek(f -> f.setAccessible(true))
                        .collect(Collectors.toList())
        );
    }

    @SneakyThrows
    public static <T> void patchIncludeNulls(T source, T target) {
        Class<?> type = source.getClass();
        while (type != null && type != Object.class) {
            for (Field field : getDeclaredFieldsCached(type)) {
                if (shouldSkipField(field)) {
                    continue;
                }

                field.setAccessible(true);
                Object sourceValue = field.get(source);

                field.set(unproxy(target), unproxy(sourceValue));

            }
            type = type.getSuperclass();
        }
    }

    @SneakyThrows
    public static <T> void patchIgnoreNulls(T source, T target) {
        Class<?> type = source.getClass();
        while (type != null && type != Object.class) {
            for (Field field : getDeclaredFieldsCached(type)) {
                if (shouldSkipField(field)) {
                    continue;
                }

                field.setAccessible(true);
                Object sourceValue = field.get(source);
                Object targetValue = field.get(target);

                if (shouldCopyValue(field, sourceValue, targetValue)) {
                    field.set(unproxy(target), unproxy(sourceValue));
                }
            }
            type = type.getSuperclass();
        }
    }

    public static Object unproxy(Object entity) {
        if (entity instanceof HibernateProxy) {
            return ((HibernateProxy) entity).getHibernateLazyInitializer()
                    .getImplementation();
        }
        return entity;
    }

    public static boolean shouldSkipField(Field field) {
        if (field.isAnnotationPresent(Version.class) ||
                field.isAnnotationPresent(Id.class) ||
                field.isAnnotationPresent(jakarta.persistence.Version.class) ||
                field.isAnnotationPresent(jakarta.persistence.Id.class)) {
            return true;
        }
        return (field.getModifiers() & Modifier.FINAL) != 0;
    }

    public static boolean shouldCopyValue(Field field, Object sourceValue, Object targetValue) {
        if (sourceValue == null && !field.getType().isPrimitive()) {
            return false;
        }
        if (field.getType().isPrimitive()) {
            return !Objects.equals(sourceValue, targetValue);
        }
        return !sourceValue.equals(targetValue);
    }

}
