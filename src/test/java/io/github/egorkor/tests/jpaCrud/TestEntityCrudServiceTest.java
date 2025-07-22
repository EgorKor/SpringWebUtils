package io.github.egorkor.tests.jpaCrud;


import io.github.egorkor.model.TestEntity;
import io.github.egorkor.model.TestNestedEntity;
import io.github.egorkor.service.TestEntityService;
import io.github.egorkor.service.impl.TestEntityCrudServiceImpl;
import io.github.egorkor.webutils.exception.EntityProcessingException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.SoftDeleteUnsupportedException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.sync.PageableResult;
import io.github.egorkor.webutils.service.sync.UpdateSpecification;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NonUniqueResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.github.egorkor.webutils.queryparam.Filter.fb;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestEntityCrudServiceImpl.class, LocalValidatorFactoryBean.class})
public class TestEntityCrudServiceTest {
    @Autowired
    private TestEntityService crudService;
    @Autowired
    private TestEntityManager testEntityManager;

    private TestEntity testEntity;
    private TestNestedEntity nestedEntity;

    @BeforeEach
    void setUp() {
        nestedEntity = new TestNestedEntity();
        nestedEntity.setId(2L);
        nestedEntity.setDeletedAt(null);

        testEntity = TestEntity.builder()
                .id(1L)
                .name("Test Entity")
                .nested(List.of(nestedEntity))
                .nullableProperty(42)
                .flag(true)
                .nums(List.of(1, 2, 3))
                .tags(List.of("tag1", "tag2"))
                .isDeleted(false)
                .build();
    }

    @Test
    void updateByFilter_SimpleStringUpdate_ShouldUpdateField() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .name("Old Name")
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .updateValue("name", "New Name")
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec,Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals("New Name", updated.getName());
    }

    @Test
    void updateByFilter_SumOperation_ShouldUpdateNumericField() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .nullableProperty(5)
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .plus("nullableProperty", 10)
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals(15, updated.getNullableProperty());
    }

    @Test
    void updateByFilter_AddDaysToLocalDate_ShouldUpdateDate() {
        // Arrange
        LocalDate initialDate = LocalDate.of(2023, 1, 1);
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .someDateField(initialDate)
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .addDays("someDateField", 5)
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals(initialDate.plusDays(5), updated.getSomeDateField());
    }

    @Test
    void updateByFilter_ConcatStrings_ShouldConcatenateValues() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .name("Prefix")
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .concat("name", "_Suffix")
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals("Prefix_Suffix", updated.getName());
    }

    @Test
    void updateByFilter_UpperCase_ShouldConvertToUpperCase() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .name("lowercase")
                .isDeleted(false)
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .upperCase("name")
                .build();
        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals("LOWERCASE", updated.getName());
    }

    @Test
    void updateByFilter_CopyValue_ShouldCopyFieldValue() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .name("Source Value")
                .copyField(null)
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .copyValue("name", "copyField")
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals("Source Value", updated.getCopyField());
    }

    @Test
    void updateByFilter_MultipleOperations_ShouldUpdateAllFields() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .name("original")
                .nullableProperty(10)
                .flag(false)
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .updateValue("flag", true)
                .plus("nullableProperty", 5)
                .concat("name", "_updated")
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertAll(
                () -> assertEquals("original_updated", updated.getName()),
                () -> assertEquals(15, updated.getNullableProperty()),
                () -> assertTrue(updated.getFlag())
        );
    }

    @Test
    void updateByFilter_WithElementCollection_ShouldUpdateCollection() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .nums(List.of(1, 2, 3))
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .updateValue("nums", List.of(4, 5, 6))
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertEquals(List.of(4, 5, 6), updated.getNums());
    }

    @Test
    void updateByFilter_SoftDelete_ShouldMarkAsDeleted() {
        // Arrange
        TestEntity entity = TestEntity.builder()
                .id(3L)
                .isDeleted(false)
                .build();
        testEntityManager.persist(entity);

        UpdateSpecification spec = new UpdateSpecification.UpdateSpecificationBuilder()
                .updateValue("isDeleted", true)
                .build();

        // Act
        int updatedCount = crudService.updateByFilter(spec, Filter.empty());

        // Assert
        assertEquals(1, updatedCount);
        TestEntity updated = testEntityManager.find(TestEntity.class, entity.getId());
        assertTrue(updated.getIsDeleted());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getAll_withFilterAndPagination_shouldReturnPageableResult() {
        Filter<TestEntity> filter = Filter.empty();
        Pagination pagination = new Pagination(0, 10);

        PageableResult<TestEntity> result = crudService.getAll(filter, pagination);

        assertNotNull(result);
        assertFalse(result.getData().isEmpty());
        assertEquals(1, result.getCount());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getAll_withFilter_shouldReturnList() {
        Filter<TestEntity> filter = Filter.empty();

        List<TestEntity> result = crudService.getAll(filter);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getAll_withFilterAndSorting_shouldReturnList() {
        Filter<TestEntity> filter = Filter.empty();
        Sorting sorting = new Sorting();
        List<TestEntity> result = crudService.getAll(filter, sorting);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getById_shouldReturnEntity() throws ResourceNotFoundException {
        TestEntity result = crudService.getById(1L);

        assertNotNull(result);
        assertEquals("Test Entity", result.getName());
        assertEquals(42, result.getNullableProperty());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getByIdWithLock_shouldReturnEntity() throws ResourceNotFoundException {
        TestEntity result = crudService.getByIdWithLock(1L, LockModeType.PESSIMISTIC_WRITE);
        assertNotNull(result);
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getByFilterWithLock_shouldReturnEntity() throws ResourceNotFoundException {
        Filter<TestEntity> filter = fb.and(
                        fb.equals("id", "1"))
                .build();
        TestEntity result = crudService.getByFilterWithLock(filter, LockModeType.PESSIMISTIC_WRITE);
        assertNotNull(result);
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getById_shouldThrowResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> crudService.getById(99L));
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void create_shouldSaveNewEntity() throws EntityProcessingException {
        TestEntity newEntity = TestEntity.builder()
                .id(3L)
                .name("New Entity")
                .flag(false)
                .build();

        TestEntity savedEntity = crudService.create(newEntity);

        assertNotNull(savedEntity.getId());
        assertEquals("New Entity", savedEntity.getName());
        assertFalse(savedEntity.getFlag());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void fullUpdate_shouldUpdateAllFields() throws EntityProcessingException, ResourceNotFoundException {
        TestEntity updatedEntity = TestEntity.builder()
                .id(1L)
                .name("Updated Entity")
                .flag(false)
                .nums(List.of(4, 5, 6))
                .build();

        TestEntity result = crudService.fullUpdate(updatedEntity);

        assertEquals("Updated Entity", result.getName());
        assertFalse(result.getFlag());
        assertEquals(List.of(4, 5, 6), result.getNums());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void patchUpdate_shouldUpdateOnlyChangedFields() throws ResourceNotFoundException, EntityProcessingException {
        TestEntity patchData = new TestEntity();
        patchData.setName("Patched Name");
        patchData.setNums(new ArrayList<>(List.of(7, 8, 9)));

        TestEntity result = crudService.patchUpdate(1L, patchData);

        assertEquals("Patched Name", result.getName());
        assertEquals(List.of(7, 8, 9), result.getNums());
        assertEquals(42, result.getNullableProperty()); // Не должно измениться
        assertTrue(result.getFlag()); // Не должно измениться
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void deleteById_shouldRemoveEntity() throws ResourceNotFoundException, EntityProcessingException {
        crudService.deleteById(1L);
        assertThrows(ResourceNotFoundException.class, () -> crudService.getById(1L));
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void countAll_shouldReturnCorrectCount() {
        long count = crudService.countAll();
        assertEquals(1, count);
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void existsById_shouldReturnTrueForExistingEntity() {
        assertTrue(crudService.existsById(1L));
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void restoreById_shouldUnmarkAsDeleted() throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException {
        // Сначала помечаем как удаленную
        crudService.softDeleteById(1L);
        // Затем восстанавливаем
        crudService.restoreById(1L);

        TestEntity entity = crudService.getById(1L);
        assertFalse(entity.getIsDeleted());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void restoreAll_shouldUnmarkAsDeleted() throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException {
        // Сначала помечаем как удаленную
        crudService.softDeleteById(1L);
        // Затем восстанавливаем
        crudService.restoreAll();

        assertDoesNotThrow(() -> crudService.getById(1L));
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getByFilter_shouldReturnEntityWithMatchingName() throws ResourceNotFoundException, NonUniqueResultException {
        Filter<TestEntity> filter =
                fb.and(fb.like("name", "Test Entity")).build();

        TestEntity result = crudService.getByFilter(filter);

        assertNotNull(result);
        assertEquals("Test Entity", result.getName());
    }

    @Test
    @Sql(scripts = "/insert-test-data.sql")
    void getStream_shouldReturnStreamOfEntities() {
        Filter<TestEntity> filter = Filter.empty();

        try (Stream<TestEntity> stream = crudService.getStream(filter)) {
            long count = stream.count();
            assertEquals(1, count);
        }
    }

}
