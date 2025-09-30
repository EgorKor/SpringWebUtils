package io.github.egorkor.tests.params;

import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.filterInternal.FilterBasicOperation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static io.github.egorkor.webutils.queryparam.Filter.fb;
import static io.github.egorkor.webutils.queryparam.filterInternal.Is.TRUE;
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

    @Test
    void testDefaultConstructor() {
        Filter<TestEntity> filter = new Filter<>();
        assertTrue(filter.isUnfiltered());
        assertFalse(filter.isFiltered());
    }

    @Test
    void testConstructorWithFilterList() {
        List<FilterBasicOperation> filters =
                List.of(fb.contains("name","John"), fb.greater("age",30));
        Filter<TestEntity> filter = new Filter<>(filters);
        assertEquals(2, filter.getOperations().size());
    }

    @Test
    void testIsFiltered() {
        Filter<TestEntity> filter = new Filter<>(List.of(
                fb.contains("name","John")));
        assertTrue(filter.isFiltered());
    }

    @Test
    void testIsUnfiltered() {
        Filter<TestEntity> filter = new Filter<>();
        assertTrue(filter.isUnfiltered());
    }

    @Test
    void testAnd() {
        Filter<TestEntity> filter1 = new Filter<>(List.of(fb.contains("name","John")));
        Filter<TestEntity> filter2 = new Filter<>(List.of(fb.greater("age",30)));

        Filter<TestEntity> result = filter1._and(filter2);
        assertEquals(2, result.getOperations().size());
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

        Filter<TestEntity> filter = new Filter<>(List.of(
                fb.equals("name","John")
                //"name:=:John"
        ));
        filter.setEntityType(TestEntity.class);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).equal(path, "John");
    }

    @Test
    void testToPredicate_containsCondition() throws Exception {
        when(root.get("name")).thenReturn(path);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        Filter<TestEntity> filter = new Filter<>(List.of(
                fb.contains("name","John")
                //"name:like:John"
        ));
        filter.setEntityType(TestEntity.class);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
//        verify(cb).like(any(), contains("%John%"));
    }

    @Test
    void testToPredicate_nestedProperty() throws Exception {
        Path<Object> nestedPath = mock(Path.class);
        when(root.get("nested")).thenReturn(path);
        when(path.get("property")).thenReturn(nestedPath);
        when(cb.equal(nestedPath, "value")).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        Filter<TestEntity> filter = new Filter<>(List.of(
                fb.equals("nested.property","value")
                //"nested.property:=:value"
        ));
        filter.setEntityType(TestEntity.class);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).equal(nestedPath, "value");
    }

    @Test
    void testToPredicate_isCondition() throws Exception {
        // Setup
        Path<Boolean> booleanPath = mock(Path.class);
        when(root.get("active")).thenReturn((Path) booleanPath);
        when(cb.isTrue(booleanPath)).thenReturn(predicate);
        when(cb.and(any())).thenReturn(predicate);

        // Test
        Filter<TestEntity> filter = new Filter<>(List.of(
            fb.is("active", TRUE)
                //        "active:is:true"
        ));
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

        assertEquals(1, filter.getOperations().size());
        FilterBasicOperation first = filter.getOperations().getFirst();
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
        Filter<TestEntity> filter = fb.and(
                fb.equals("name", "John"),
                fb.greater("age", "30")
        );

        assertEquals(2, filter.getOperations().size());
    }

    private Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static class TestEntity {
        String name;
        int age;
        boolean active;
        NestedEntity nested;
    }

    static class NestedEntity {
        String property;
    }
}
