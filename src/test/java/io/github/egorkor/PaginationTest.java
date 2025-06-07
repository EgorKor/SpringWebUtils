package io.github.egorkor;

import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PaginationTest {

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
        Assertions.assertEquals("OFFSET 45 LIMIT 15", pagination.toSQLPageable());
    }

    @Test
    public void testCalculatePageCount(){
        Assertions.assertEquals(PageableResult.countPages(105,10),11);
    }
}
