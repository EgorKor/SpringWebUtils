package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.service.TestEntityService;
import io.github.egorkor.service.TestNestedEntityService;
import io.github.egorkor.service.impl.TestEntityCrudServiceImpl;
import io.github.egorkor.service.impl.TestNestedEntityServiceImpl;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import jakarta.persistence.EntityManager;
import lombok.ToString;
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

@Import({TestEntityCrudServiceImpl.class, TestNestedEntityServiceImpl.class})
@DataJpaTest
@ActiveProfiles("test")
public class JpaCrudServiceTests {
    @Autowired
    private EntityManager em;
    @Autowired
    private TestEntityService testEntityService;
    @Autowired
    private TestNestedEntityService testNestedEntityService;
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
    public void testGetById(){
        Assertions.assertNotNull(repo.findById(1L));
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
    public void testNotFound3(){
        testEntityService.softDeleteById(2L);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(2L);
        });
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
        );

        Sorting sorting = new Sorting();
        Pagination pagination = new Pagination();
        PageableResult<TestEntity> results = testEntityService.getAll(
                filter,
                sorting,
                pagination
        );
        System.out.println(results);
        Assertions.assertEquals(1, results.getData().size());
    }


}
