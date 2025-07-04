package io.github.egorkor;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.webutils.queryparam.utils.FieldTypeUtils;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        Field field = FieldTypeUtils.getField(TestEntity.class, "nested");
        field.setAccessible(true);
        System.out.println(field.getGenericType().getTypeName());
        Class<?> genericTypeClass = Class.forName(getPureClassNameByGenericType(
                field.getGenericType().getTypeName()
        ));
        System.out.println(genericTypeClass);

    }



    public static String getPureClassNameByGenericType(String input){
        return input.substring(
                input.indexOf("<") + 1, input.length() - 1
        );
    }
}
