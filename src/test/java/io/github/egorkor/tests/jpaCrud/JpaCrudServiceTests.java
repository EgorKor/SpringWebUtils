package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.Tag;
import io.github.egorkor.model.TestEntity;
import io.github.egorkor.params.UserFilter;
import io.github.egorkor.params.UserSort;
import io.github.egorkor.service.TestEntityService;
import io.github.egorkor.service.TestNestedEntityService;
import io.github.egorkor.service.impl.TestEntityCrudServiceImpl;
import io.github.egorkor.service.impl.TestNestedEntityServiceImpl;
import io.github.egorkor.webutils.exception.InvalidParameterException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.PageableResult;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;
import java.util.List;

import static io.github.egorkor.webutils.queryparam.Filter.fb;

@Import({TestEntityCrudServiceImpl.class, TestNestedEntityServiceImpl.class, LocalValidatorFactoryBean.class})
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
                                .tags(List.of("tag1", "tag2"))
                                .nums(List.of(1, 2, 3))
                                .nullableProperty(10)
                                .flag(true)
                                .enumTags(List.of(Tag.TAG1, Tag.TAG2))
                                .build(),
                        TestEntity.builder()
                                .id(2L)
                                .name("Egor")
                                .isDeleted(false)
                                .flag(false)
                                .nullableProperty(null)
                                .nums(List.of(1, 2))
                                .tags(List.of("tag2"))
                                .enumTags(List.of(Tag.TAG1))
                                .build()
                )
        );
        repo.flush();
    }

    @Test
    public void shouldThrowExceedLimitParametersCountExceptionForFilter() {
        UserFilter filter = fb.and(
                        fb.equals("id", "1"),
                        fb.equals("orders_name", "name"))
                .buildDerived(UserFilter.class);
        Assertions.assertThrows(InvalidParameterException.class, filter::toSQLFilter);
    }

    @Test
    public void shouldThrowExceedLimitParametersCountExceptionForSorting() {
        UserSort userSort = Sorting.builder()
                .asc("id")
                .desc("name")
                .buildDerived(UserSort.class);
        Assertions.assertThrows(InvalidParameterException.class, userSort::toSQLSort);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForNonAllowedSortingParam() {
        UserSort userSort = Sorting.builder()
                .asc("ids")
                .buildDerived(UserSort.class);
        Assertions.assertThrows(InvalidParameterException.class, userSort::toSQLSort);
    }


    @Test
    public void shouldCorrectMapIsTrue() {
        Assertions.assertEquals(1, testEntityService
                .countByFilter(
                        fb.and(fb.is("flag", Filter.Is.TRUE)).build()
                ));
    }

    @Test
    public void shouldCorrectMapIsFalse() {
        Assertions.assertEquals(1, testEntityService
                .countByFilter(
                        fb.and(fb.is("flag", Filter.Is.FALSE)).build()
                ));
    }

    @Test
    public void shouldCorrectMapIsNull() {
        Assertions.assertEquals(1, testEntityService
                .countByFilter(
                        fb.and(
                                fb.is("nullableProperty", Filter.Is.NULL)
                        ).build()
                ));
    }

    @Test
    public void shouldCorrectMapIsNotNull() {
        Assertions.assertEquals(1, testEntityService
                .countByFilter(
                        fb.and(
                                fb.is("nullableProperty", Filter.Is.NOT_NULL)
                        ).build()
                ));
    }


    @Test
    public void shouldCorrectMapInOperationWithList() {
        Assertions.assertEquals(
                1, testEntityService.countByFilter(
                        fb.and(
                                fb.in("tags", "tag1")
                        ).build())
        );
    }

    @Test
    public void shouldCorrectMapInOperationWithEnumList() {
        Assertions.assertEquals(
                2, testEntityService.countByFilter(
                        fb.and(
                                fb.in("enumTags", "TAG1")
                        ).build()
                )
        );
    }

    @Test
    public void shouldCorrectMapInOperationWithIntegerList() {
        Assertions.assertEquals(
                2, testEntityService.countByFilter(
                        fb.and(
                                fb.in("nums", "1", "2")
                        ).build()
                )
        );
    }

    @Test
    public void shouldThrowNotFound() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(10L);
        });
    }

    @Test
    public void shouldThrowNotFoundAndGetCorrectMessage() {
        try {
            testEntityService.getById(10L);
        } catch (ResourceNotFoundException e) {
            Assertions.assertEquals("Entity TestEntity with id = 10 not found.", e.getMessage());
        }
    }

    @Test
    public void shouldNotFoundAfterSoftDeleteById() {
        testEntityService.softDeleteById(2L);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(2L);
        });
    }

    @Test
    public void shouldNotFoundAfterSoftDeleteWithFilter() {
        Filter filter = fb.and(
                fb.equals("name", "some name")
        ).build();
        testEntityService.deleteByFilter(filter);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(1L);
        });
    }

    @Test
    public void shouldFoundZeroRecordsAfterSoftDeleteAll() {
        testEntityService.softDeleteAll();
        Assertions.assertEquals(0, testEntityService.countAll());
    }

    @Test
    public void shouldParseSizeFunctionForEquals() {
        Assertions.assertEquals(
                testEntityService.countByFilter(
                        fb.and(
                                fb.equals("nums.size()", "2")
                        ).build()
                ), 1
        );
    }

    @Test
    public void shouldParseSizeFunctionForCompare() {
        Assertions.assertEquals(
                testEntityService.countByFilter(
                        fb.and(fb.greaterOrEquals("nums.size()", "2"))
                                .build()
                ), 2
        );
    }

    @Test
    public void shouldParseLengthFunctionForEquals() {
        Assertions.assertEquals(
                testEntityService.countByFilter(
                        fb.and(fb.equals("name.length()", "4"))
                                .build()), 1
        );
    }

    @Test
    public void shouldParseLengthFunctionForCompare() {
        Assertions.assertEquals(
                testEntityService.countByFilter(
                        fb.and(fb.greaterOrEquals("name.length()", "5"))
                                .build()), 1
        );
    }

    @Test
    public void shouldGetById() {
        Assertions.assertNotNull(testEntityService.getById(1L));
    }

    @Test
    public void shouldGetAllWithFiltration() {
        List<String> strFilters = new ArrayList<>();
        strFilters.add("name:like:some name");
        Filter<TestEntity> filter = new Filter<>(
                strFilters
        );

        Sorting sorting = new Sorting();
        Pagination pagination = new Pagination();
        PageableResult<TestEntity> results = testEntityService.getPage(
                filter,
                sorting,
                pagination
        );
        System.out.println(results);
        Assertions.assertEquals(1, results.getData().size());
    }


}
