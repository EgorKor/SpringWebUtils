package io.github.egorkor.tests;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.service.TestEntityService;
import io.github.egorkor.service.TestEntityServiceImpl;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@Import(TestEntityServiceImpl.class)
@DataJpaTest
@ActiveProfiles("test")
public class JpaServiceTests {
    @Autowired
    private EntityManager em;
    @Autowired
    private TestEntityService testEntityService;
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
    public void testNotFound1() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(10L);
        });
    }

    @Test
    public void testNotFound2() {
        try {
            testEntityService.getById(10L);
        } catch (ResourceNotFoundException e) {
            Assertions.assertEquals("Сущность TestEntity c id = 10 не найдена.", e.getMessage());
        }
    }

    @Test
    public void testSuccessFoundById() {
        Assertions.assertNotNull(testEntityService.getById(1L));
    }

    @Test
    public void testGetAll() {
        List<String> strFilters = new ArrayList<>();
        strFilters.add("name:like:some name");
        Filter<TestEntity> filter = new Filter<>(
                strFilters
        ).withSoftDeleteFlag(false);

        Sorting sorting = new Sorting();
        Pagination pagination = new Pagination();
        PageableResult<List<TestEntity>> results = testEntityService.getAll(
                filter,
                sorting,
                pagination
        );
        Assertions.assertEquals(1, results.getData().size());
    }
}
