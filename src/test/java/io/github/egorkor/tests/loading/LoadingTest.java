package io.github.egorkor.tests.loading;

import io.github.egorkor.model.CascadeLazyChildEntity;
import io.github.egorkor.model.LazyChildEntity;
import io.github.egorkor.model.ParentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
public class LoadingTest {
    @Autowired
    private JpaRepository<ParentEntity, Long> parentRepository;
    @Autowired
    private JpaRepository<LazyChildEntity, Long> lazyChildRepository;
    @Autowired
    private JpaRepository<CascadeLazyChildEntity, Long> cascadeLazyChildRepository;
    @Autowired
    private EntityManager em;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    private Statistics stats;

    @BeforeTransaction
    public void setup() {
        ParentEntity parent = parentRepository.save(ParentEntity.builder()
                .name("parent")
                .build());
        lazyChildRepository.saveAll(
                List.of(
                        LazyChildEntity.builder()
                                .name("child1")
                                .parent(parent)
                                .build(),
                        LazyChildEntity.builder()
                                .name("child2")
                                .parent(parent)
                                .build()
                )
        );
        cascadeLazyChildRepository.saveAll(
                List.of(
                        CascadeLazyChildEntity.builder()
                                .name("child1")
                                .parent(parent)
                                .build(),
                        CascadeLazyChildEntity.builder()
                                .name("child2")
                                .parent(parent)
                                .build()
                )
        );
        this.stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.stats.clear();
        this.em.clear();
    }


    @Test
    public void testFetch1() {
        stats.setStatisticsEnabled(true);
        ParentEntity parent = parentRepository.findAll().getFirst();
        stats.setStatisticsEnabled(false);
        System.out.println(stats.getPrepareStatementCount());
    }

    @Test
    public void testFetch2() {
        stats.setStatisticsEnabled(true);
        List<LazyChildEntity> entities = lazyChildRepository.findAll();
        stats.setStatisticsEnabled(false);
        System.out.println(stats.getPrepareStatementCount());
    }

    @Test
    public void testFetch3() {
        stats.setStatisticsEnabled(true);
        List<CascadeLazyChildEntity> entities = cascadeLazyChildRepository.findAll();
        stats.setStatisticsEnabled(false);
        System.out.println(stats.getPrepareStatementCount());
    }

}
