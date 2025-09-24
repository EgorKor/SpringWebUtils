package io.github.egorkor.tests.pathing;

import io.github.egorkor.webutils.template.jpa.JpaEntityPropertyPatcher;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatcherTest {
    // Helper method to access the cache for testing
    private static Map<Class<?>, List<Field>> getFieldCache() {
        return JpaEntityPropertyPatcher.getFieldCache();
    }

    @Test
    void shouldSkipIdField() throws NoSuchFieldException {
        Field idField = TestEntity.class.getDeclaredField("id");
        assertTrue(JpaEntityPropertyPatcher.shouldSkipField(idField));
    }

    @Test
    void shouldSkipVersionField() throws NoSuchFieldException {
        Field versionField = TestEntity.class.getDeclaredField("version");
        assertTrue(JpaEntityPropertyPatcher.shouldSkipField(versionField));
    }

    @Test
    void shouldSkipFinalField() throws NoSuchFieldException {
        Field finalField = TestEntity.class.getDeclaredField("finalField");
        assertTrue(JpaEntityPropertyPatcher.shouldSkipField(finalField));
    }

    @Test
    void shouldNotSkipRegularField() throws NoSuchFieldException {
        Field regularField = TestEntity.class.getDeclaredField("name");
        assertFalse(JpaEntityPropertyPatcher.shouldSkipField(regularField));
    }

    @Test
    void shouldCopyNonNullValueWhenTargetIsDifferent() {
        TestEntity source = new TestEntity("source", 10, true);
        TestEntity target = new TestEntity("target", 20, false);

        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        assertEquals("source", target.name);
        assertEquals(10, target.value);
        assertTrue(target.flag);
    }

    @Test
    void shouldNotCopyNullValueForObjectFields() {
        TestEntity source = new TestEntity(null, 0, false);
        TestEntity target = new TestEntity("target", 0, false);

        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        assertEquals("target", target.name); // name should not be updated to null
    }

    @Test
    void shouldCopyNullValueForPrimitiveFields() {
        TestEntity source = new TestEntity("source", 0, false);
        TestEntity target = new TestEntity("target", 10, true);

        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        assertEquals(0, target.value); // primitive should be updated even if source is 0
    }

    @Test
    void shouldHandleInheritedFields() {
        ChildEntity source = new ChildEntity("child", 5, false, "childProp");
        ChildEntity target = new ChildEntity("parent", 10, true, "targetProp");

        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        assertEquals("child", target.name);
        assertEquals(5, target.value);
        assertFalse(target.flag);
        assertEquals("childProp", target.childProperty);
    }

    @Test
    void shouldUnproxyHibernateProxy() {
        // Create a mock Hibernate proxy
        HibernateProxy proxy = mock(HibernateProxy.class);
        LazyInitializer initializer = mock(LazyInitializer.class);
        TestEntity implementation = new TestEntity("proxied", 42, true);

        when(proxy.getHibernateLazyInitializer()).thenReturn(initializer);
        when(initializer.getImplementation()).thenReturn(implementation);

        Object result = JpaEntityPropertyPatcher.unproxy(proxy);

        assertSame(implementation, result);
    }

    @Test
    void shouldReturnSameObjectWhenNotProxy() {
        TestEntity entity = new TestEntity("test", 1, true);
        Object result = JpaEntityPropertyPatcher.unproxy(entity);
        assertSame(entity, result);
    }

    @Test
    void shouldHandleNullValuesInUnproxy() {
        assertNull(JpaEntityPropertyPatcher.unproxy(null));
    }

    @Test
    void shouldNotCopyIdField() {
        TestEntity source = new TestEntity("source", 1, true);
        source.id = 100L;
        TestEntity target = new TestEntity("target", 2, false);
        target.id = 200L;

        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        assertEquals(200L, target.id); // id should not be updated
    }

    @Test
    void shouldNotCopyVersionField() {
        TestEntity source = new TestEntity("source", 1, true);
        source.version = 100L;
        TestEntity target = new TestEntity("target", 2, false);
        target.version = 200L;

        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        assertEquals(200L, target.version); // version should not be updated
    }

    @Test
    void shouldCacheFields() {
        // First call should populate cache
        TestEntity source = new TestEntity("source", 1, true);
        TestEntity target = new TestEntity("target", 2, false);
        JpaEntityPropertyPatcher.patchIgnoreNulls(source, target);

        // Verify cache was populated
        assertFalse(JpaEntityPropertyPatcher.getFieldCache().isEmpty());
        assertTrue(JpaEntityPropertyPatcher.getFieldCache().containsKey(TestEntity.class));
    }

    @AllArgsConstructor
    @NoArgsConstructor
    static class TestEntity {
        final String finalField = "constant";
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
        @Version
        Long version;
        String name;
        int value;
        boolean flag;

        public TestEntity(String name, int value, boolean flag) {
            this.name = name;
            this.value = value;
            this.flag = flag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return value == that.value && flag == that.flag && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, flag);
        }
    }

    static class ChildEntity extends TestEntity {
        String childProperty;

        public ChildEntity(String name, int value, boolean flag, String childProperty) {
            super(name, value, flag);
            this.childProperty = childProperty;
        }
    }
}
