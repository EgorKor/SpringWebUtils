package io.github.egorkor.tests.utils;

import io.github.egorkor.webutils.queryparam.utils.FieldTypeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FieldTypeUtilsTest {

    @Test
    void shouldGetPublicField() throws Exception {
        Field field = FieldTypeUtils.getField(TestClass.class, "publicField");
        assertEquals("publicField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetPrivateField() throws Exception {
        Field field = FieldTypeUtils.getField(TestClass.class, "privateField");
        assertEquals("privateField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetProtectedField() throws Exception {
        Field field = FieldTypeUtils.getField(TestClass.class, "protectedField");
        assertEquals("protectedField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetPackagePrivateField() throws Exception {
        Field field = FieldTypeUtils.getField(TestClass.class, "packagePrivateField");
        assertEquals("packagePrivateField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetNestedField() throws Exception {
        Field field = FieldTypeUtils.getField(TestClass.class, "nested.nestedField");
        assertEquals("nestedField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetDeeplyNestedField() throws Exception {
        Field field = FieldTypeUtils.getField(TestClass.class, "nested.deepNested.deepField");
        assertEquals("deepField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetInheritedField() throws Exception {
        Field field = FieldTypeUtils.getField(ChildClass.class, "inheritedField");
        assertEquals("inheritedField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldGetChildField() throws Exception {
        Field field = FieldTypeUtils.getField(ChildClass.class, "childField");
        assertEquals("childField", field.getName());
        assertEquals(String.class, field.getType());
    }

    @Test
    void shouldCacheFields() throws Exception {
        // First call should populate cache
        Field field1 = FieldTypeUtils.getField(TestClass.class, "publicField");

        // Second call should use cache
        Field field2 = FieldTypeUtils.getField(TestClass.class, "publicField");

        assertSame(field1, field2);
    }

    @Test
    void shouldThrowWhenFieldNotFound() {
        Executable action = () -> FieldTypeUtils.getField(TestClass.class, "nonExistentField");
        assertThrows(NoSuchFieldException.class, action);
    }

    @Test
    void shouldThrowWhenNestedFieldNotFound() {
        Executable action = () -> FieldTypeUtils.getField(TestClass.class, "nested.nonExistentField");
        assertThrows(NoSuchFieldException.class, action);
    }

    @Test
    void shouldThrowWhenNullTargetType() {
        Executable action = () -> FieldTypeUtils.getField(null, "field");
        assertThrows(NullPointerException.class, action);
    }

    @Test
    void shouldThrowWhenNullFieldPath() {
        Executable action = () -> FieldTypeUtils.getField(TestClass.class, null);
        assertThrows(NullPointerException.class, action);
    }

    @Test
    void shouldThrowWhenEmptyFieldPath() {
        Executable action = () -> FieldTypeUtils.getField(TestClass.class, "");
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void shouldThrowWhenEmptyFieldNameInPath() {
        Executable action = () -> FieldTypeUtils.getField(TestClass.class, "nested..field");
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void shouldHandleGenericFieldTypes() throws Exception {
        Field field = FieldTypeUtils.getField(GenericClass.class, "genericField");
        assertEquals("genericField", field.getName());
        assertEquals(Object.class, field.getType());
    }

    @Test
    void shouldHandleComplexGenericFieldTypes() throws Exception {
        Field field = FieldTypeUtils.getField(GenericClass.class, "complexGenericField");
        assertEquals("complexGenericField", field.getName());
        assertEquals(List.class, field.getType());
    }

    @Test
    void testGetPureClassNameByGenericType() {
        String result = FieldTypeUtils.getPureClassNameByGenericType("List<String>");
        assertEquals("String", result);

        result = FieldTypeUtils.getPureClassNameByGenericType("Map<String,Integer>");
        assertEquals("String,Integer", result);
    }

    @Test
    void shouldHandleFileSeparatorInFieldNames() throws Exception {
        // Test class with field containing dots (like file extensions)
        class FileHolder {
            public File fileWithDots;
        }

        Field field = FieldTypeUtils.getField(FileHolder.class, "fileWithDots");
        assertEquals("fileWithDots", field.getName());
        assertEquals(File.class, field.getType());
    }

    // Test classes for reflection
    static class TestClass {
        public String publicField;
        public NestedClass nested;
        protected String protectedField;
        String packagePrivateField;
        private String privateField;
    }

    static class NestedClass {
        public String nestedField;
        public DeepNested deepNested;
    }

    static class DeepNested {
        public String deepField;
    }

    static class ParentClass {
        private String inheritedField;
    }

    static class ChildClass extends ParentClass {
        public String childField;
    }

    static class GenericClass<T> {
        public T genericField;
        public List<Map<String, Integer>> complexGenericField;
    }
}