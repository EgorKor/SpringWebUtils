package io.github.egorkor.tests;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.model.TestNestedEntity;
import io.github.egorkor.webutils.queryparam.Filter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

public class FilterTest {
    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    static void setup() {
        try {
            emf = Persistence.createEntityManagerFactory("test-pu");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterAll
    static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void init() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
    }

    @AfterEach
    void tearDown() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        if (em.isOpen()) {
            em.close();
        }
    }

    @Test
    void testFilterJPA1() {
        Filter<TestEntity> filter = new Filter<>();
        filter.setFilter(
                List.of(
                        "id:=:10", "name:like:some name", "isDeleted:is:true"
                )
        );
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestEntity> cq = cb.createQuery(TestEntity.class);
        Predicate predicate = filter.toPredicate(cq.from(TestEntity.class), cq, cb);
        System.out.println(predicate);
        Assertions.assertEquals(predicate.getExpressions().size(), 3);
    }

    @Test
    void testFilterJPA2() {
        Filter<TestNestedEntity> filter = new Filter<>();
        filter.setFilter(
                List.of("parent.id:=:10")
        );
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestNestedEntity> cq = cb.createQuery(TestNestedEntity.class);
        Predicate predicate = filter.toPredicate(cq.from(TestNestedEntity.class), cq, cb);
        System.out.println(predicate);

        Assertions.assertEquals(predicate.getExpressions().size(), 1);
    }

    @Test
    void testFilterEmptySQL() {
        Filter<TestEntity> filter = new Filter<>();

        Assertions.assertEquals("", filter.toSQLFilter());
    }

    @Test
    void testFilterSQL1() {
        Filter<TestEntity> filter = new Filter<>();
        filter.setFilter(
                List.of(
                        "id:=:10", "name:like:%some name!%"
                )
        );
        System.out.println(filter.toSQLFilter());
        System.out.println(Arrays.toString(filter.getFilterValues()));
        Assertions.assertEquals("WHERE id = ? AND name LIKE ? ESCAPE '!'",
                filter.toSQLFilter().trim());
        Assertions.assertArrayEquals(new Object[]{"10", "%!%some name!!!%%"}, filter.getFilterValues());
    }

    @Test
    void testFilterSQL2() {
        Filter<TestNestedEntity> filter = new Filter<>();
        filter.setFilter(
                List.of("id:!=:10", "id:is:not_null")
        );
        System.out.println(filter.toSQLFilter());
        System.out.println(Arrays.toString(filter.getFilterValues()));
        Assertions.assertEquals("WHERE id <> ? AND id IS NOT NULL",
                filter.toSQLFilter().trim());
        Assertions.assertArrayEquals(new Object[]{"10"}, filter.getFilterValues());
    }

    @Test
    void testFilterSQL3() {
        Filter<TestNestedEntity> filter = new Filter<>();
        filter.setFilter(
                List.of("id:IN:10;15;23")
        );
        System.out.println(filter.toSQLFilter());
        System.out.println(Arrays.toString(filter.getFilterValues()));
        Assertions.assertEquals("WHERE id IN (?,?,?)"
                , filter.toSQLFilter().trim());
        Assertions.assertArrayEquals(new Object[]{"'10'", "'15'", "'23'"}, filter.getFilterValues());
    }


}
