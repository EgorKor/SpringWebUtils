package io.github.egorkor;

import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestEntityServiceImpl.class)
@DataJpaTest
@ActiveProfiles("test")
public class CRUDLTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private TestEntityService testEntityService;



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
        }catch (ResourceNotFoundException e) {
            Assertions.assertEquals("Сущность TestEntity c id = 10 не найдена.",e.getMessage());
        }
    }
}
