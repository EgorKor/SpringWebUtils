package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.TestingEntityBatching;
import io.github.egorkor.service.BatchTestEntityService;
import io.github.egorkor.service.impl.BatchTestEntityServiceImpl;
import io.github.egorkor.webutils.exception.BatchOperationException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.service.batching.BatchOperationStatus;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({BatchTestEntityServiceImpl.class, LocalValidatorFactoryBean.class})
public class BatchTest {
    @Autowired
    private BatchTestEntityService service;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void checkAutoCommit() {
        service.deleteAll();
    }

    @Test
    public void batchCreateAtomic_shouldSucceed() {
        // Given
        List<TestingEntityBatching> entities = Arrays.asList(
                new TestingEntityBatching(null, "Entity 1"),
                new TestingEntityBatching(null, "Entity 2"),
                new TestingEntityBatching(null, "Entity 3")
        );

        // When
        List<TestingEntityBatching> result = service.batchCreateAtomic(entities);

        // Then
        assertEquals(3, result.size());
        assertNotNull(result.get(0).getId());
        assertNotNull(result.get(1).getId());
        assertNotNull(result.get(2).getId());

        // Verify all entities were saved


        assertEquals(3, service.countAll());
    }


    @Test
    public void batchDeleteAtomic_shouldSucceed() {
        // Given
        TestingEntityBatching e1 = service.create(new TestingEntityBatching(null, "Entity 1"));
        TestingEntityBatching e2 = service.create(new TestingEntityBatching(null, "Entity 2"));

        List<Long> ids = Arrays.asList(e1.getId(), e2.getId());

        // When
        service.batchDeleteAtomic(ids);

        // Then
        assertEquals(0, service.countAll());
        assertFalse(service.existsById(e1.getId()));
        assertFalse(service.existsById(e2.getId()));
    }

    @Test
    public void batchCreate_shouldSucceedPartially() {
        // Given
        List<TestingEntityBatching> entities = Arrays.asList(
                new TestingEntityBatching(null, "Valid 1"), // should succeed
                new TestingEntityBatching(null, ""), // should fail (empty name)
                new TestingEntityBatching(null, "Valid 2")  // should succeed
        );

        // When
        List<BatchResultWithData<TestingEntityBatching>> results = service.batchCreate(entities);

        // Then
        assertEquals(3, results.size());

        // Check first entity (success)
        assertEquals(BatchOperationStatus.SUCCESS, results.get(0).getStatus());
        assertNotNull(results.get(0).getData().getId());

        // Check second entity (failure)
        assertEquals(BatchOperationStatus.FAILED, results.get(1).getStatus());
        assertTrue(results.get(1).getMessage().contains("fails for entity"));

        // Check third entity (success)
        assertEquals(BatchOperationStatus.SUCCESS, results.get(2).getStatus());
        assertNotNull(results.get(2).getData().getId());

        // Verify only 2 entities were saved
        assertEquals(2, service.countAll());
    }


    @Test
    public void batchDelete_shouldSucceedPartially() {
        // Given
        TestingEntityBatching e1 = service.create(new TestingEntityBatching(null, "Entity 1"));
        TestingEntityBatching e2 = service.create(new TestingEntityBatching(null, "Entity 2"));

        List<Long> ids = Arrays.asList(
                e1.getId(),     // exists
                999L,           // doesn't exist
                e2.getId()      // exists
        );

        // When
        List<BatchResultWithData<Long>> results = service.batchDelete(ids);

        // Then
        assertEquals(3, results.size());

        // First delete (success)
        assertEquals(BatchOperationStatus.SUCCESS, results.get(0).getStatus());
        assertFalse(service.existsById(e1.getId()));

        // Second delete (failure)
        assertEquals(BatchOperationStatus.FAILED, results.get(1).getStatus());

        // Third delete (success)
        assertEquals(BatchOperationStatus.SUCCESS, results.get(2).getStatus());
        assertFalse(service.existsById(e2.getId()));
    }

    @Test
    public void batchCreateAtomic_shouldRollbackOnFailure() {
        // Given
        List<TestingEntityBatching> entities = Arrays.asList(
                new TestingEntityBatching(null, "Valid 1"),
                new TestingEntityBatching(null, ""), // invalid - should cause rollback
                new TestingEntityBatching(null, "Valid 2")
        );

        // When/Then
        assertThrows(BatchOperationException.class, () -> {
            service.batchCreateAtomic(entities);
        });
        var res = service.getList(Filter.empty());
        // Verify no entities were persisted
        assertEquals(0, service.countAll());
    }

}
