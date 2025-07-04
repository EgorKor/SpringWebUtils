package io.github.egorkor.webutils.queryparam.utils;

import lombok.SneakyThrows;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FieldTypeUtils {
    private static final ConcurrentMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Safely gets the type of a field (including nested fields) with proper error handling
     *
     * @param targetType the starting class to inspect
     * @param fieldPath  the field name or path (e.g. "person.address.street")
     * @return the Class<?> type of the field
     * @throws IllegalArgumentException if arguments are invalid
     * @throws SecurityException if field access is denied by security manager
     */
    @SneakyThrows
    public static Field getField(Class<?> targetType, String fieldPath) {
        // Input validation
        Objects.requireNonNull(targetType, "Target type cannot be null");
        Objects.requireNonNull(fieldPath, "Field path cannot be null");

        if (fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Field path cannot be empty");
        }

        String[] fields = fieldPath.split("\\.");

        Field currentType = null;
        for (String fieldName : fields) {
            if (fieldName.isEmpty()) {
                throw new IllegalArgumentException("Field name cannot be empty in path: " + fieldPath);
            }

            currentType = getFieldTypeInternal(currentType == null ? targetType : currentType.getType(), fieldName);
        }

        return currentType;
    }

    private static Field getFieldTypeInternal(Class<?> type, String fieldName)
            throws NoSuchFieldException, SecurityException {
        String cacheKey = type.getName() + "#" + fieldName;
        Field field = FIELD_CACHE.get(cacheKey);

        if (field == null) {
            field = findField(type, fieldName);
            FIELD_CACHE.putIfAbsent(cacheKey, field);
        }

        return field;
    }

    public static String getPureClassNameByGenericType(String input){
        return input.substring(
                input.indexOf("<") + 1, input.length() - 1
        );
    }

    private static Field findField(Class<?> type, String fieldName)
            throws NoSuchFieldException {
        // Try public fields first
        try {
            return type.getField(fieldName);
        } catch (NoSuchFieldException e) {
            // Fall back to declared fields (including non-public)
            Class<?> currentType = type;
            while (currentType != null) {
                try {
                    Field field = currentType.getDeclaredField(fieldName);
                    field.setAccessible(true);  // Enable access to non-public fields
                    return field;
                } catch (NoSuchFieldException ex) {
                    currentType = currentType.getSuperclass();
                }
            }
            throw new NoSuchFieldException("Field '" + fieldName +
                    "' not found in class " + type.getName() + " or its superclasses");
        }
    }
}
