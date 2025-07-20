package io.github.egorkor.tests.params;

import io.github.egorkor.webutils.queryparam.Sorting;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SortingTest {

    @Mock
    private Root<TestEntity> root;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<Object> path;
    @Mock
    private Path<Object> nestedPath;
    @Mock
    private Order ascOrder;
    @Mock
    private Order descOrder;

    @Test
    public void testSQLSortEmpty() {
        Sorting sorting = Sorting.unsorted();
        Assertions.assertEquals("", sorting.toSQLSort());
    }

    @Test
    public void testSQLSort1() {
        Sorting sorting = new Sorting();
        sorting.setSort(List.of("id:asc", "name:desc"));
        Assertions.assertEquals("ORDER BY id ASC, name DESC", sorting.toSQLSort().trim());
    }

    @Test
    public void testSQLSort2() {
        Sorting sorting = new Sorting();
        sorting.setSort(List.of("id:asc", "name:desc"));
        Assertions.assertEquals("ORDER BY t.id ASC, t.name DESC", sorting.toSQLSort("t.").trim());
    }

    @Test
    public void testJpaTestEmpty() {
        Sorting sorting = new Sorting();
        Assertions.assertTrue(sorting.toJpaSort().isUnsorted());
    }

    @Test
    public void testJpaSort2() {
        Sorting sorting = new Sorting();
        sorting.setSort(List.of("id:asc", "name:desc"));
        Sort sort = sorting.toJpaSort();

        Assertions.assertTrue(Objects.requireNonNull(sort.getOrderFor("id")).getDirection().isAscending());
        Assertions.assertTrue(Objects.requireNonNull(sort.getOrderFor("name")).getDirection().isDescending());
    }

    @Test
    void toCriteriaOrderList_shouldReturnEmptyListForUnsorted() {
        Sorting sorting = Sorting.unsorted();
        List<Order> result = sorting.toCriteriaOrderList(root, cb);
        assertTrue(result.isEmpty());
    }

    @Test
    void toCriteriaOrderList_shouldCreateAscOrderForSingleField() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("name:asc");

        when(root.get("name")).thenReturn(path);
        when(cb.asc(path)).thenReturn(ascOrder);

        // Test
        List<Order> result = sorting.toCriteriaOrderList(root, cb);

        // Verify
        assertEquals(1, result.size());
        assertSame(ascOrder, result.get(0));
        verify(root).get("name");
        verify(cb).asc(path);
    }

    @Test
    void toCriteriaOrderList_shouldCreateDescOrderForSingleField() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("age:desc");

        when(root.get("age")).thenReturn(path);
        when(cb.desc(path)).thenReturn(descOrder);

        // Test
        List<Order> result = sorting.toCriteriaOrderList(root, cb);

        // Verify
        assertEquals(1, result.size());
        assertSame(descOrder, result.getFirst());
        verify(root).get("age");
        verify(cb).desc(path);
    }

    @Test
    void toCriteriaOrderList_shouldHandleMultipleSortFields() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("name:asc");
        sorting.getSort().add("age:desc");

        when(root.get("name")).thenReturn(path);
        when(root.get("age")).thenReturn(path);
        when(cb.asc(path)).thenReturn(ascOrder);
        when(cb.desc(path)).thenReturn(descOrder);

        // Test
        List<Order> result = sorting.toCriteriaOrderList(root, cb);

        // Verify
        assertEquals(2, result.size());
        assertSame(ascOrder, result.get(0));
        assertSame(descOrder, result.get(1));
    }

    @Test
    void toCriteriaOrderList_shouldHandleNestedProperties() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("nested.property:asc");

        when(root.get("nested")).thenReturn(path);
        when(path.get("property")).thenReturn(nestedPath);
        when(cb.asc(nestedPath)).thenReturn(ascOrder);

        // Test
        List<Order> result = sorting.toCriteriaOrderList(root, cb);

        // Verify
        assertEquals(1, result.size());
        assertSame(ascOrder, result.getFirst());
        verify(root).get("nested");
        verify(path).get("property");
        verify(cb).asc(nestedPath);
    }

    @Test
    void toCriteriaOrderList_shouldThrowForInvalidSortFormat() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("invalid_format");

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> {
            sorting.toCriteriaOrderList(root, cb);
        });
    }

    @Test
    void toCriteriaOrderList_shouldThrowForInvalidDirection() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("name:invalid");

        // Test & Verify
        assertThrows(IllegalArgumentException.class, () -> {
            sorting.toCriteriaOrderList(root, cb);
        });
    }

    @Test
    void toCriteriaOrderList_shouldCallCheckAllowedFields() {
        // Setup
        Sorting sorting = spy(new Sorting());
        sorting.getSort().add("name:asc");

        when(root.get("name")).thenReturn(path);
        when(cb.asc(path)).thenReturn(ascOrder);

        // Test
        sorting.toCriteriaOrderList(root, cb);

        // Verify
        verify(sorting).checkAllowedSortFields();
    }

    @Test
    void toCriteriaOrderList_shouldIgnoreCaseForDirection() {
        // Setup
        Sorting sorting = new Sorting();
        sorting.getSort().add("name:ASC");
        sorting.getSort().add("age:DESC");

        when(root.get("name")).thenReturn(path);
        when(root.get("age")).thenReturn(path);
        when(cb.asc(path)).thenReturn(ascOrder);
        when(cb.desc(path)).thenReturn(descOrder);

        // Test
        List<Order> result = sorting.toCriteriaOrderList(root, cb);

        // Verify
        assertEquals(2, result.size());
        assertSame(ascOrder, result.get(0));
        assertSame(descOrder, result.get(1));
    }

    static class TestEntity {
        String name;
        int age;
        NestedEntity nested;
    }

    static class NestedEntity {
        String property;
    }

}
