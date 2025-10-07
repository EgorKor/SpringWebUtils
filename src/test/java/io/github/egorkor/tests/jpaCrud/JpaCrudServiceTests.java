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
import io.github.egorkor.webutils.queryparam.filterInternal.Is;
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

import java.util.List;

import static io.github.egorkor.webutils.queryparam.Filter.*;
import static io.github.egorkor.webutils.queryparam.Sorting.sb;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        UserFilter filter = fb.and(UserFilter.class,
                fb.equals("id", 1),
                fb.equals("orders_name", "name"));
        System.out.println(filter);
        filter.mapFilterByAllies();
        var exception = assertThrows(InvalidParameterException.class, filter::checkAllowedFilterOperations);
        System.out.println(exception.getMessage());
    }

    @Test
    public void shouldThrowExceedLimitParametersCountExceptionForSorting() {
        UserSort userSort = sb.by(UserSort.class,
                sb.asc("id"),
                sb.desc("name"));
        Assertions.assertThrows(InvalidParameterException.class, userSort::toJpaSort);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionForNonAllowedSortingParam() {
        UserSort userSort = sb.by(UserSort.class, sb.asc("ids"));
        Assertions.assertThrows(InvalidParameterException.class, userSort::toJpaSort);
    }


    @Test
    public void shouldCorrectMapIsTrue() {
        assertEquals(1, testEntityService
                .countByFilter(is("flag", Is.TRUE)));
    }

    @Test
    public void shouldCorrectMapIsFalse() {
        assertEquals(1, testEntityService.countByFilter(is("flag", Is.FALSE)));
    }

    @Test
    public void shouldCorrectMapIsNull() {
        assertEquals(1, testEntityService.countByFilter(is("nullableProperty", Is.NULL)));
    }

    @Test
    public void shouldCorrectMapIsNotNull() {
        assertEquals(1, testEntityService.countByFilter(is("nullableProperty", Is.NOT_NULL)));
    }


    @Test
    public void shouldCorrectMapInOperationWithList() {
        assertEquals(1, testEntityService.countByFilter(in("tags", "tag1")));
    }

    @Test
    public void shouldCorrectMapInOperationWithEnumList() {
        assertEquals(2, testEntityService.countByFilter(in("enumTags", "TAG1")));
    }

    @Test
    public void shouldCorrectMapInOperationWithIntegerList() {
        assertEquals(2, testEntityService.countByFilter(in("nums", 1, 2)));
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
            assertEquals("Сущность TestEntity с id = 10 не найдена.", e.getMessage());
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
        Filter filter = equal("name", "some name");
        testEntityService.deleteByFilter(filter);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            testEntityService.getById(1L);
        });
    }

    @Test
    public void shouldFoundZeroRecordsAfterSoftDeleteAll() {
        testEntityService.softDeleteAll();
        assertEquals(0, testEntityService.countAll());
    }

    @Test
    public void shouldParseSizeFunctionForEquals() {
        assertEquals(
                testEntityService.countByFilter(
                        fb.and(
                                fb.equals("nums.size()", "2")
                        )
                ), 1
        );
    }

    @Test
    public void shouldParseSizeFunctionForCompare() {
        assertEquals(testEntityService.countByFilter(greaterThanOrEqual("nums.size()", 2)), 2);
    }

    @Test
    public void shouldParseLengthFunctionForEquals() {
        assertEquals(testEntityService.countByFilter(equal("name.length()", 4)), 1);
        assertEquals(testEntityService.countByFilter(notEqual("name.length()", 4)), 1);
        assertEquals(testEntityService.countByFilter(in("name.length()", 3,4)), 0);
    }

    @Test
    public void shouldParseLengthFunctionForCompare() {
        assertEquals(testEntityService.countByFilter(greaterThanOrEqual("name.length()", 5)), 1);
    }

    @Test
    public void shouldGetById() {
        Assertions.assertNotNull(testEntityService.getById(1L));
    }

    @Test
    public void shouldGetAllWithFiltration() {
        Filter<TestEntity> filter = like("name", "some name");

        Sorting sorting = new Sorting();
        Pagination pagination = new Pagination();
        PageableResult<TestEntity> results = testEntityService.getPage(
                filter,
                sorting,
                pagination
        );
        System.out.println(results);
        assertEquals(1, results.getData().size());
    }


}
