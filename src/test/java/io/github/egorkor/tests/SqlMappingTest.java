package io.github.egorkor.tests;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.params.TestEntityFilter;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
public class SqlMappingTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JpaRepository<TestEntity, Long> repo;

    @BeforeEach
    public void setup() {
        repo.deleteAll();
        repo.flush();
        repo.saveAll(
                List.of(
                        TestEntity.builder()
                                .id(1L)
                                .name("some name")
                                .isDeleted(false)
                                .build(),
                        TestEntity.builder()
                                .id(2L)
                                .name("Egor")
                                .isDeleted(false)
                                .build()
                )
        );
        repo.flush();
    }

    @Test
    public void test() {
        Pagination pagination = new Pagination();
        pagination.setPage(2);
        pagination.setSize(15);

        TestEntityFilter filter = new TestEntityFilter();
        List<String> filters = new ArrayList<>();
        filters.add("id:=:1");
        filters.add("_name:like:some name");
        filter.setFilter(filters);
        filter.concat(Filter.softDeleteFilter("is_deleted", Boolean.class, false));

        Sorting sorting = new Sorting();
        sorting.setSort(List.of("id:asc"));

        String sql = "SELECT * FROM test_entity %s %s %s"
                .formatted(
                        filter.toSQLFilter().trim(),
                        sorting.toSQLSort().trim(),
                        pagination.toSqlPageable().trim()
                );
        System.out.println(Arrays.toString(filter.getFilterValues()));
        Assertions.assertEquals(
                "SELECT * FROM test_entity "
                        + "WHERE id = ? AND _name LIKE ? ESCAPE '!' AND is_deleted = false "
                        + "ORDER BY id ASC "
                        + "LIMIT 15 OFFSET 30", sql

        );
        jdbcTemplate.query(sql, (rs) -> {
        }, filter.getFilterValues());
    }

}
