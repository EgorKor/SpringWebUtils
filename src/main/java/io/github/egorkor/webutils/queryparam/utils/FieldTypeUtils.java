package io.github.egorkor.webutils.queryparam.utils;

import io.github.egorkor.webutils.queryparam.Filter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FieldTypeUtils {
    private static final ConcurrentMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Safely gets the type of a field (including nested fields) with proper error handling
     * Automatically resolves collection element types for List, Set, Map, etc.
     *
     * @param targetType the starting class to inspect
     * @param fieldPath  the field name or path (e.g. "persons.address.street")
     * @return the Field object representing the field
     * @throws IllegalArgumentException if arguments are invalid
     * @throws SecurityException        if field access is denied by security manager
     */
    @SneakyThrows
    public static Field getField(Class<?> targetType, String fieldPath) {
        if (fieldPath.matches("concat\\(.*\\)")) {
            return null;
        }

        // Input validation
        Objects.requireNonNull(targetType, "Target type cannot be null");
        Objects.requireNonNull(fieldPath, "Field path cannot be null");

        if (fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Field path cannot be empty");
        }

        String[] fields = fieldPath.split("\\.");
        Class<?> currentType = targetType;
        Field currentField = null;

        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i];

            if (fieldName.isEmpty()) {
                throw new IllegalArgumentException("Field name cannot be empty in path: " + fieldPath);
            }

            currentField = getFieldTypeInternal(currentType, fieldName);

            // Если это не последнее поле в пути, подготавливаем тип для следующей итерации
            if (i < fields.length - 1) {
                currentType = resolveActualType(currentField);
            }
        }

        return currentField;
    }

    /**
     * Resolves the actual type of a field, handling collections and generics
     */
    private static Class<?> resolveActualType(Field field) {
        Class<?> fieldType = field.getType();

        // Если поле является коллекцией, получаем тип элемента
        if (isCollectionType(fieldType)) {
            return getCollectionElementType(field);
        }

        // Если поле является массивом, получаем тип элемента массива
        if (fieldType.isArray()) {
            return fieldType.getComponentType();
        }

        // Для обычных полей возвращаем тип поля
        return fieldType;
    }

    /**
     * Checks if the type is a collection (List, Set, Collection, etc.)
     */
    private static boolean isCollectionType(Class<?> type) {
        return Collection.class.isAssignableFrom(type) ||
                Map.class.isAssignableFrom(type) ||
                type.getName().startsWith("java.util.") &&
                        (type.getName().contains("List") ||
                                type.getName().contains("Set") ||
                                type.getName().contains("Collection"));
    }

    /**
     * Extracts the element type from a collection field using generics
     */
    private static Class<?> getCollectionElementType(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArguments = paramType.getActualTypeArguments();

            if (typeArguments.length > 0) {
                // Для Map используем тип значения (второй аргумент)
                if (Map.class.isAssignableFrom(field.getType())) {
                    return getClassFromType(typeArguments[1]);
                }
                // Для Collection используем первый аргумент
                else {
                    return getClassFromType(typeArguments[0]);
                }
            }
        }

        // Если не удалось определить generic тип, возвращаем Object.class
        return Object.class;
    }

    /**
     * Extracts Class from Type, handling various Type implementations
     */
    private static Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                return (Class<?>) upperBounds[0];
            }
        }

        return Object.class;
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

    /**
     * Utility method to get the final target class for a field path
     * Useful for determining what type you'll end up with after navigating the path
     */
    public static Class<?> getFinalTargetClass(Class<?> targetType, String fieldPath) {
        Field field = getField(targetType, fieldPath);
        return field != null ? resolveActualType(field) : null;
    }

    public static String getPureClassNameByGenericType(String input) {
        return input.substring(
                input.indexOf("<") + 1, input.length() - 1
        );
    }

    /*private static Field findField(Class<?> type, String fieldName)
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
    }*/
}
