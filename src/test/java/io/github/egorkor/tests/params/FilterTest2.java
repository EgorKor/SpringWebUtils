package io.github.egorkor.tests.params;

import io.github.egorkor.webutils.queryparam.Filter;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FilterTest2 {
    @Mock
    private Root<TestEntity> root;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    static class TestEntity {
        String name;
        int age;
        boolean active;
        NestedEntity nested;
    }

    static class NestedEntity {
        String property;
    }

    @Test
    void testDefaultConstructor() {
        Filter<TestEntity> filter = new Filter<>();
        assertTrue(filter.isUnfiltered());
        assertFalse(filter.isFiltered());
    }

    @Test
    void testConstructorWithFilterList() {
        List<String> filters = List.of("name:like:John", "age:>:30");
        Filter<TestEntity> filter = new Filter<>(filters);
        assertEquals(2, filter.getFilter().size());
    }

    @Test
    void testIsFiltered() {
        Filter<TestEntity> filter = new Filter<>(List.of("name:like:John"));
        assertTrue(filter.isFiltered());
    }

    @Test
    void testIsUnfiltered() {
        Filter<TestEntity> filter = new Filter<>();
        assertTrue(filter.isUnfiltered());
    }

    @Test
    void testConcat() {
        Filter<TestEntity> filter1 = new Filter<>(List.of("name:like:John"));
        Filter<TestEntity> filter2 = new Filter<>(List.of("age:>:30"));

        Filter<TestEntity> result = filter1.concat(filter2);
        assertEquals(2, result.getFilter().size());
    }

    @Test
    void testToSQLFilter_unfiltered() {
        Filter<TestEntity> filter = new Filter<>();
        assertEquals("", filter.toSQLFilter());
    }

    @Test
    void testToSQLFilter_basicCondition() {
        Filter<TestEntity> filter = new Filter<>(List.of("name:=:John"));
        String sql = filter.toSQLFilter();
        assertTrue(sql.contains("WHERE name = ?"));
    }

    @Test
    void testToSQLFilter_likeCondition() {
        Filter<TestEntity> filter = new Filter<>(List.of("name:like:John"));
        String sql = filter.toSQLFilter();
        assertTrue(sql.contains("WHERE name LIKE ? ESCAPE '!'"));
    }

    @Test
    void testToSQLFilter_inCondition() {
        Filter<TestEntity> filter = new Filter<>(List.of("name:in:John;Doe;Smith"));
        String sql = filter.toSQLFilter();
        assertTrue(sql.contains("WHERE name IN (?,?,?)"));
    }

    @Test
    void testToSQLFilter_withPrefix() {
        Filter<TestEntity> filter = new Filter<>(List.of("name:=:John"));
        String sql = filter.toSQLFilter("e.");
        assertTrue(sql.contains("WHERE e.name = ?"));
    }

    @Test
    void testGetFilterValues() {
        Filter<TestEntity> filter = new Filter<>(List.of(
                "name:like:John",
                "age:>:30",
                "active:is:true",
                "nested.property:in:val1;val2"
        ));

        Object[] values = filter.getFilterValues();
        assertEquals(4, values.length);
        assertTrue(values[0].toString().contains("%John%"));
        assertEquals("30", values[1]);
        assertTrue(values[2].toString().contains("val1"));
        assertTrue(values[3].toString().contains("val2"));
    }

    @Test
    void testToPredicate_unfiltered() {
        Filter<TestEntity> filter = new Filter<>();
        Predicate result = filter.toPredicate(root, cb);
        assertNull(result);
    }

    @Test
    void testToPredicate_equalsCondition() throws Exception {
        when(root.get("name")).thenReturn(path);
        when(cb.equal(path, "John")).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        Filter<TestEntity> filter = new Filter<>(List.of("name:=:John"));
        filter.setEntityType(TestEntity.class);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).equal(path, "John");
    }

    @Test
    void testToPredicate_likeCondition() throws Exception {
        when(root.get("name")).thenReturn(path);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        Filter<TestEntity> filter = new Filter<>(List.of("name:like:John"));
        filter.setEntityType(TestEntity.class);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).like(any(), contains("%John%"));
    }

    @Test
    void testToPredicate_nestedProperty() throws Exception {
        Path<Object> nestedPath = mock(Path.class);
        when(root.get("nested")).thenReturn(path);
        when(path.get("property")).thenReturn(nestedPath);
        when(cb.equal(nestedPath, "value")).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        Filter<TestEntity> filter = new Filter<>(List.of("nested.property:=:value"));
        filter.setEntityType(TestEntity.class);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).equal(nestedPath, "value");
    }

    @Test
    void testToPredicate_isCondition() throws Exception {
        // Setup
        Path<Boolean> booleanPath = mock(Path.class);
        when(root.get("active")).thenReturn((Path)booleanPath);
        when(cb.isTrue(booleanPath)).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        // Test
        Filter<TestEntity> filter = new Filter<>(List.of("active:is:true"));
        filter.setEntityType(TestEntity.class);
        Predicate result = filter.toPredicate(root, cb);

        // Verify
        assertNotNull(result);
        verify(cb).isTrue(booleanPath);
    }

    @Test
    void testSoftDeleteFilter_booleanField() {
        Field field = getField(TestEntity.class, "active");
        Filter<TestEntity> filter = Filter.softDeleteFilter(field, true);

        assertEquals(1, filter.getFilter().size());
        assertTrue(filter.getFilter().get(0).contains("active:is:true"));
    }

    @Test
    void testEmptyFilter() {
        Filter<TestEntity> filter = Filter.empty();
        assertTrue(filter.isUnfiltered());
    }

    @Test
    void testEmptyFilterWithType() {
        Filter<TestEntity> filter = Filter.empty(TestEntity.class);
        assertTrue(filter.isUnfiltered());
        assertEquals(TestEntity.class, filter.getEntityType());
    }

    @Test
    void testFilterBuilder() {
        Filter<TestEntity> filter = Filter.builder()
                .equals("name", "John")
                .greater("age", "30")
                .build();

        assertEquals(2, filter.getFilter().size());
        assertTrue(filter.getFilter().get(0).contains("name:=:John"));
        assertTrue(filter.getFilter().get(1).contains("age:>:30"));
    }

    private Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
