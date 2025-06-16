package io.github.egorkor.tests;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
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

        Filter filter = new Filter();
        List<String> filters = new ArrayList<>();
        filters.add("id:=:1");
        filters.add("isDeleted:is:true");
        filter.setFilter(filters);
        filter.withSoftDeleteFlag(false);

        Sorting sorting = new Sorting();
        sorting.setSort(List.of("id:asc"));

        String sql = "SELECT * FROM test_entity\n%s\n%s\n%s"
                .formatted(
                        filter.toSQLFilter().trim(),
                        pagination.toSqlPageable().trim(),
                        sorting.toSQLSort().trim()
                );
        Assertions.assertEquals(
                """
                        SELECT * FROM test_entity
                        WHERE id = '1' AND isDeleted = true
                        LIMIT 15 OFFSET 30
                        ORDER BY id ASC""", sql

        );
    }

}
