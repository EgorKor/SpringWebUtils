package io.github.egorkor.tests.params;

import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.queryparam.utils.DatabaseType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaginationTest {

    @Test
    void testDefaultConstructor() {
        Pagination pagination = new Pagination();
        assertEquals(Pagination.DEFAULT_PAGE_SIZE, pagination.getSize());
        assertEquals(Pagination.DEFAULT_PAGE, pagination.getPage());
    }

    @Test
    void testAllArgsConstructor() {
        Pagination pagination = new Pagination(2, 20);
        assertEquals(20, pagination.getSize());
        assertEquals(2, pagination.getPage());
    }

    @Test
    void testIsUnpaged_whenAllContentSize() {
        Pagination pagination = Pagination.unpaged();
        assertTrue(pagination.isUnpaged());
    }

    @Test
    void testIsUnpaged_whenNotAllContentSize() {
        Pagination pagination = new Pagination(0, 10);
        assertFalse(pagination.isUnpaged());
    }

    @Test
    void testUnpaged() {
        Pagination pagination = Pagination.unpaged();
        assertEquals(Pagination.ALL_CONTENT_SIZE, pagination.getSize());
    }

    @Test
    void testOf() {
        Pagination pagination = Pagination.of(2, 20);
        assertEquals(20, pagination.getSize());
        assertEquals(2, pagination.getPage());
    }

    @Test
    void testToJpaPageable_whenUnpaged() {
        Pagination pagination = Pagination.unpaged();
        Pageable pageable = pagination.toJpaPageable();
        assertTrue(pageable.isUnpaged());
    }

    @Test
    void testToJpaPageable_whenPaged() {
        Pagination pagination = new Pagination(2, 15);
        Pageable pageable = pagination.toJpaPageable();
        assertFalse(pageable.isUnpaged());
        assertEquals(15, pageable.getPageSize());
        assertEquals(2, pageable.getPageNumber());
    }

    @Test
    void testToJpaPageableWithSort_whenUnpaged() {
        Pagination pagination = Pagination.unpaged();
        Sort sort = Sort.by("name");
        Pageable pageable = pagination.toJpaPageable(sort);
        assertTrue(pageable.isUnpaged());
        assertTrue(pageable.getSort().get().anyMatch(order -> "name".equals(order.getProperty())));
    }

    @Test
    void testToJpaPageableWithSort_whenPaged() {
        Pagination pagination = new Pagination(1, 10);
        Sort sort = Sort.by("name");
        Pageable pageable = pagination.toJpaPageable(sort);
        assertFalse(pageable.isUnpaged());
        assertEquals(10, pageable.getPageSize());
        assertEquals(1, pageable.getPageNumber());
        assertTrue(pageable.getSort().get().anyMatch(order -> "name".equals(order.getProperty())));
    }

    @Test
    void testToJpaPageableWithSorting_whenUnpaged() {
        Pagination pagination = Pagination.unpaged();
        Sorting sorting = new Sorting();
        sorting.getSort().add("name:asc");
        Pageable pageable = pagination.toJpaPageable(sorting);
        assertTrue(pageable.isUnpaged());
    }

    @Test
    void testToSqlPageable_whenUnpaged() {
        Pagination pagination = Pagination.unpaged();
        assertEquals("", pagination.toSqlPageable());
    }

    @Test
    void testToSqlPageable_forLimitOffsetDatabases() {
        List<DatabaseType> types = Arrays.asList(
                DatabaseType.POSTGRESQL,
                DatabaseType.H2,
                DatabaseType.SQLITE,
                DatabaseType.MYSQL,
                DatabaseType.MARIADB
        );

        for (DatabaseType dbType : types) {
            Pagination pagination = new Pagination(2, 10);
            String expected = "LIMIT 10 OFFSET 20";
            assertEquals(expected, pagination.toSqlPageable(dbType),
                    "Failed for " + dbType);
        }
    }

    @Test
    void testToSqlPageable_forOracleAndSqlServer() {
        List<DatabaseType> types = Arrays.asList(
                DatabaseType.ORACLE,
                DatabaseType.SQL_SERVER
        );

        for (DatabaseType dbType : types) {
            Pagination pagination = new Pagination(2, 10);
            String expected = "OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY";
            assertEquals(expected, pagination.toSqlPageable(dbType),
                    "Failed for " + dbType);
        }
    }

    @Test
    void testToSqlPageable_forDB2() {
        Pagination pagination = new Pagination(2, 10);
        String expected = "OFFSET 20 ROWS FETCH FIRST 10 ROWS ONLY";
        assertEquals(expected, pagination.toSqlPageable(DatabaseType.DB2));
    }

    @Test
    void testToSqlPageable_withDefaultDatabaseType() {
        Pagination pagination = new Pagination(2, 10);
        assertNotNull(pagination.toSqlPageable());
    }

    @Test
    void testToSqlPageable_withUnsupportedDatabaseType() {
        Pagination pagination = new Pagination(2, 10);
        assertThrows(UnsupportedOperationException.class, () -> {
            pagination.toSqlPageable(DatabaseType.OTHER);
        });
    }

    @Test
    void testToJpaPageableWithSorting_whenPaged() {
        Pagination pagination = new Pagination(1, 15);
        Sorting sorting = new Sorting();
        sorting.getSort().add("name:asc");
        Pageable pageable = pagination.toJpaPageable(sorting);
        assertFalse(pageable.isUnpaged());
        assertEquals(15, pageable.getPageSize());
        assertEquals(1, pageable.getPageNumber());
        assertTrue(pageable.getSort().get().anyMatch(order -> "name".equals(order.getProperty())));
    }

    @Test
    public void testJpaPagination() {
        Pagination pagination = new Pagination();
        pagination.setPage(0);
        pagination.setSize(10);
        Assertions.assertEquals(0, pagination.toJpaPageable().getPageNumber());
        Assertions.assertEquals(10, pagination.toJpaPageable().getPageSize());
    }

    @Test
    public void testSQLPagination() {
        Pagination pagination = new Pagination();
        pagination.setPage(3);
        pagination.setSize(15);
        Assertions.assertEquals("LIMIT 15 OFFSET 45", pagination.toSqlPageable());
    }

    @Test
    public void testEmptySQLPagination() {
        Pagination pagination = Pagination.unpaged();
        Assertions.assertEquals("", pagination.toSqlPageable());
    }

    @Test
    public void testCalculatePageCount() {
        Assertions.assertEquals(PageableResult.countPages(105, 10), 11);
    }
}
