package io.github.egorkor.tests;

import io.github.egorkor.webutils.queryparam.Sorting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;

public class SortingTest {

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


}
